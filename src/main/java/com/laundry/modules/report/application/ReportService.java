package com.laundry.modules.report.application;

import com.laundry.modules.order.domain.CancelReason;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import com.laundry.modules.payments.domain.PaymentMethod;
import com.laundry.modules.report.application.dto.CustomerReportResponse;
import com.laundry.modules.report.application.dto.OperationsReportResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.OverviewReportResponse;
import com.laundry.modules.report.application.dto.PromotionReportResponse;
import com.laundry.modules.report.application.dto.RevenuePaymentResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import com.laundry.modules.report.domain.Granularity;
import com.laundry.modules.report.domain.ReportPeriod;
import com.laundry.modules.report.domain.ReportQueryRepository;
import com.laundry.modules.report.domain.ReportQueryRepository.CancelReasonCount;
import com.laundry.modules.report.domain.ReportQueryRepository.ChannelOrderTotals;
import com.laundry.modules.report.domain.ReportQueryRepository.CountPair;
import com.laundry.modules.report.domain.ReportQueryRepository.CustomerCohort;
import com.laundry.modules.report.domain.ReportQueryRepository.CustomerSpendingRecord;
import com.laundry.modules.report.domain.ReportQueryRepository.DueLateCounts;
import com.laundry.modules.report.domain.ReportQueryRepository.LiveOrderCounts;
import com.laundry.modules.report.domain.ReportQueryRepository.OnlineFunnel;
import com.laundry.modules.report.domain.ReportQueryRepository.OrderPeriodCount;
import com.laundry.modules.report.domain.ReportQueryRepository.OrderStatusCount;
import com.laundry.modules.report.domain.ReportQueryRepository.OrderSummaryCounts;
import com.laundry.modules.report.domain.ReportQueryRepository.ProcessingStats;
import com.laundry.modules.report.domain.ReportQueryRepository.RevenueRecord;
import com.laundry.modules.report.domain.ReportQueryRepository.RevenueTotals;
import com.laundry.modules.report.domain.ReportQueryRepository.ServiceRevenueRecord;
import com.laundry.modules.report.domain.ReportQueryRepository.StoredOrders;
import com.laundry.modules.report.domain.ReportQueryRepository.VoucherRevenueRecord;
import com.laundry.modules.report.domain.ReportQueryRepository.VoucherUsageRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    // shortcut: tuần tự; chuyển sang chạy song song nếu đo SC-005 vượt ngân sách
    ReportQueryRepository reportQueryRepository;
    OverdueDaysProvider overdueDaysProvider;

    /** BM11: doanh thu theo ngày/tháng, tách tiền mặt và chuyển khoản. */
    @Transactional(readOnly = true, timeout = 30)
    public RevenueReportResponse getRevenueReport(String rawFrom, String rawTo, String rawGranularity,
                                                  OrderChannel channel) {
        return buildRevenueReport(ReportPeriod.of(rawFrom, rawTo), Granularity.parse(rawGranularity), channel);
    }

    // shortcut: tối đa 50.000 giao dịch, không phân trang; thêm phân trang nếu kỳ dài vượt ngưỡng
    private static final int MAX_REVENUE_PAYMENTS = 50_000;

    /** Giao dịch tạo nên doanh thu của khoảng ngày, để đối chiếu với báo cáo. */
    @Transactional(readOnly = true, timeout = 30)
    public List<RevenuePaymentResponse> getRevenuePayments(String rawFrom, String rawTo, OrderChannel channel) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        return reportQueryRepository.findRevenuePayments(
                        period.startInstant(), period.endInstant(), channel, MAX_REVENUE_PAYMENTS)
                .stream().map(RevenuePaymentResponse::from)
                .sorted(Comparator.comparing(RevenuePaymentResponse::paidAt)).toList();
    }

    private RevenueReportResponse buildRevenueReport(ReportPeriod period, Granularity granularity,
                                                     OrderChannel channel) {
        List<RevenueRecord> records = reportQueryRepository.sumPaymentsByPeriodAndMethod(
                period.startInstant(), period.endInstant(), granularity);

        // Điền sẵn mọi kỳ với 0 để FE vẽ biểu đồ không bị đứt
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (String label : granularity.periods(period.from(), period.to())) {
            buckets.put(label, new Bucket());
        }

        // byChannel luôn đủ hai kênh và bỏ qua bộ lọc (SC-004: Online + Tại quầy = tổng không lọc)
        Map<OrderChannel, Bucket> channels = new LinkedHashMap<>();
        for (OrderChannel c : OrderChannel.values()) {
            channels.put(c, new Bucket());
        }

        long cashCount = 0;
        long bankTransferCount = 0;
        for (RevenueRecord row : records) {
            Bucket byChannel = channels.get(OrderChannel.valueOf(row.channel()));
            byChannel.total = byChannel.total.add(row.total());
            byChannel.paymentCount += row.paymentCount();
            if (channel != null && !channel.name().equals(row.channel())) {
                continue;
            }
            Bucket bucket = buckets.computeIfAbsent(row.period(), k -> new Bucket());
            if (PaymentMethod.CASH.name().equals(row.method())) {
                bucket.cash = bucket.cash.add(row.total());
                cashCount += row.paymentCount();
            } else if (PaymentMethod.BANK_TRANSFER.name().equals(row.method())) {
                bucket.bankTransfer = bucket.bankTransfer.add(row.total());
                bankTransferCount += row.paymentCount();
            }
            if (OrderChannel.ONLINE.name().equals(row.channel())) {
                bucket.online = bucket.online.add(row.total());
            } else {
                bucket.offline = bucket.offline.add(row.total());
            }
            // total cộng mọi phương thức để không mất tiền khi có phương thức mới
            bucket.total = bucket.total.add(row.total());
            bucket.paymentCount += row.paymentCount();
        }

        List<RevenueReportResponse.Row> rows = buckets.entrySet().stream()
                .map(e -> new RevenueReportResponse.Row(
                        e.getKey(), e.getValue().cash, e.getValue().bankTransfer,
                        e.getValue().online, e.getValue().offline, e.getValue().total, e.getValue().paymentCount))
                .toList();

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal bankTransfer = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        long paymentCount = 0;
        for (Bucket bucket : buckets.values()) {
            cash = cash.add(bucket.cash);
            bankTransfer = bankTransfer.add(bucket.bankTransfer);
            total = total.add(bucket.total);
            paymentCount += bucket.paymentCount;
        }

        ReportPeriod prev = period.previous();
        RevenueTotals totals = reportQueryRepository.sumRevenueTotals(
                prev.startInstant(), period.startInstant(), period.endInstant(), channel);

        return new RevenueReportResponse(
                period.from(), period.to(), granularity, rows,
                new RevenueReportResponse.Summary(
                        cash, bankTransfer, total, paymentCount, cashCount, bankTransferCount,
                        channels.entrySet().stream()
                                .map(e -> new RevenueReportResponse.ChannelRevenue(
                                        e.getKey(), e.getValue().total, e.getValue().paymentCount))
                                .toList(),
                        totals.paidOrders()),
                new RevenueReportResponse.Previous(prev.from(), prev.to(), totals.previousTotal(),
                        totals.previousPaidOrders()));
    }

    /** BM12: số đơn, tỉ lệ trễ và doanh thu theo dịch vụ. */
    @Transactional(readOnly = true, timeout = 30)
    public OrderReportResponse getOrderReport(String rawFrom, String rawTo, String rawGranularity,
                                              OrderChannel channel) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        Granularity granularity = Granularity.parse(rawGranularity);
        Instant start = period.startInstant();
        Instant end = period.endInstant();

        Map<String, Long> counts = reportQueryRepository.countOrdersByStatus(start, end, channel).stream()
                .collect(Collectors.toMap(OrderStatusCount::status, OrderStatusCount::count));

        List<OrderReportResponse.StatusCount> byStatus = Arrays.stream(OrderStatus.values())
                .filter(s -> !s.isBeforeDropoff())
                .map(s -> new OrderReportResponse.StatusCount(
                        s.name(), s.getDescription(), counts.getOrDefault(s.name(), 0L)))
                .toList();

        OrderSummaryCounts summary = reportQueryRepository.countOrderSummary(
                period.previous().startInstant(), start, end, channel);
        DueLateCounts dueLate = reportQueryRepository.countDueAndLate(start, end, channel);

        // Gộp theo code (null → OTHER), điền 0 cho code thiếu; sort ổn định nên bằng nhau giữ thứ tự enum
        Map<String, Long> cancelCounts = new LinkedHashMap<>();
        long onlineCancelled = 0;
        for (CancelReasonCount c : reportQueryRepository.countCancelledByReason(start, end, channel)) {
            cancelCounts.merge(c.code() == null ? CancelReason.OTHER.name() : c.code(), c.count(), Long::sum);
            onlineCancelled += c.online();
        }
        List<OrderReportResponse.CancelReasonCount> cancelReasons = Arrays.stream(CancelReason.values())
                .map(r -> new OrderReportResponse.CancelReasonCount(
                        r.name(), r.getLabel(), cancelCounts.getOrDefault(r.name(), 0L)))
                .sorted(Comparator.comparingLong(OrderReportResponse.CancelReasonCount::count).reversed())
                .toList();
        long cancelledOrders = cancelReasons.stream().mapToLong(OrderReportResponse.CancelReasonCount::count).sum();

        ProcessingStats ps = reportQueryRepository.processingStats(start, end, channel);
        OrderReportResponse.Processing processing = new OrderReportResponse.Processing(
                ps.completedOrders(), oneDecimal(ps.avgHours()), oneDecimal(ps.medianHours()),
                ps.onTimeOrders(), percent(ps.onTimeOrders(), ps.completedOrders()));

        List<OrderReportResponse.WashModeTotals> byWashMode = reportQueryRepository
                .sumOrdersByWashMode(start, end, channel).stream()
                .map(w -> new OrderReportResponse.WashModeTotals(w.washModeId(), w.washModeName(), w.orders(), w.amount()))
                .toList();

        OrderReportResponse.Online online = null;
        if (channel != OrderChannel.OFFLINE) {
            OnlineFunnel f = reportQueryRepository.onlineFunnel(start, end);
            online = new OrderReportResponse.Online(f.requests(), f.confirmed(), onlineCancelled,
                    percent(f.confirmed(), f.requests()), oneDecimal(f.avgConfirmMinutes()));
        }

        List<OrderReportResponse.ServiceRevenue> byService = reportQueryRepository.sumRevenueByService(start, end, channel)
                .stream()
                .map(ReportService::toServiceRevenue)
                .toList();

        BigDecimal itemsRevenue = byService.stream()
                .map(OrderReportResponse.ServiceRevenue::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Điền sẵn mọi mốc với 0 để FE vẽ đường không bị đứt
        Map<String, Long> received = new LinkedHashMap<>();
        for (String label : granularity.periods(period.from(), period.to())) {
            received.put(label, 0L);
        }
        for (OrderPeriodCount c : reportQueryRepository.countOrdersByPeriod(start, end, granularity, channel)) {
            received.put(c.period(), c.received());
        }
        List<OrderReportResponse.PeriodCount> rows = received.entrySet().stream()
                .map(e -> new OrderReportResponse.PeriodCount(e.getKey(), e.getValue()))
                .toList();

        Map<String, ChannelOrderTotals> channelTotals = reportQueryRepository.sumOrdersByChannel(start, end)
                .stream().collect(Collectors.toMap(ChannelOrderTotals::channel, t -> t));
        List<OrderReportResponse.ChannelOrders> byChannel = Arrays.stream(OrderChannel.values())
                .map(c -> {
                    ChannelOrderTotals t = channelTotals.get(c.name());
                    return t == null
                            ? new OrderReportResponse.ChannelOrders(c, 0, BigDecimal.ZERO)
                            : new OrderReportResponse.ChannelOrders(c, t.orders(), t.revenue());
                })
                .toList();

        return new OrderReportResponse(
                period.from(), period.to(), granularity,
                new OrderReportResponse.Summary(
                        summary.received(), summary.delivered(), cancelledOrders, summary.active(),
                        dueLate.due(), dueLate.late(), percent(dueLate.late(), dueLate.due()),
                        summary.totalAmount(), itemsRevenue, byChannel),
                new OrderReportResponse.Previous(summary.previousReceived()),
                cancelReasons,
                processing,
                byWashMode,
                online,
                byStatus,
                byService,
                rows);
    }

    /** Tổng quan: số liệu hiện tại + bốn chỉ số kỳ so với kỳ liền trước; bỏ cả from/to = hôm nay. */
    @Transactional(readOnly = true, timeout = 30)
    public OverviewReportResponse getOverview(String rawFrom, String rawTo) {
        ReportPeriod period = ReportPeriod.ofOptional(rawFrom, rawTo);
        ReportPeriod prev = period.previous();
        Instant from = period.startInstant();
        Instant to = period.endInstant();
        Instant prevFrom = prev.startInstant();

        OrderSummaryCounts orders = reportQueryRepository.countOrderSummary(prevFrom, from, to, null);
        RevenueTotals revenue = reportQueryRepository.sumRevenueTotals(prevFrom, from, to, null);
        CustomerCohort cohort = reportQueryRepository.customerCohort(prevFrom, from, to);
        CountPair incidents = reportQueryRepository.countIncidents(prevFrom, from, to);

        int storedAfterDays = overdueDaysProvider.getDays();
        Instant now = Instant.now();
        LiveOrderCounts live = reportQueryRepository.countLiveOrders();
        StoredOrders stored = reportQueryRepository.findStoredOrders(now.minus(storedAfterDays, ChronoUnit.DAYS));

        return new OverviewReportResponse(
                period.from(), period.to(),
                new OverviewReportResponse.Live(
                        live.waiting(), live.processing(), live.readyForPickup(),
                        live.pendingConfirmation(), live.awaitingDropoff(), live.lateOrders(),
                        stored.total(), storedAfterDays,
                        reportQueryRepository.findDueSoonOrders().stream()
                                .map(d -> new OverviewReportResponse.DueSoon(
                                        d.orderId(), d.orderCode(), d.customerName(), d.status(), d.dueAt()))
                                .toList(),
                        stored.rows().stream()
                                .map(o -> new OverviewReportResponse.Stored(
                                        o.orderId(), o.orderCode(), o.customerName(), o.readyAt(),
                                        ChronoUnit.DAYS.between(o.readyAt(), now)))
                                .toList()),
                new OverviewReportResponse.Counts(
                        orders.received(), revenue.total(), cohort.newCustomers(), incidents.current()),
                new OverviewReportResponse.Counts(
                        orders.previousReceived(), revenue.previousTotal(),
                        cohort.previousNewCustomers(), incidents.previous()));
    }

    /** Vận hành: đơn theo nhân viên, ca làm, sự cố. Tổng ca tính từ chính shiftRows để luôn khớp bảng. */
    @Transactional(readOnly = true, timeout = 30)
    public OperationsReportResponse getOperationsReport(String rawFrom, String rawTo, String rawGranularity) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        Granularity granularity = Granularity.parse(rawGranularity);
        Instant from = period.startInstant();
        Instant to = period.endInstant();

        List<ReportQueryRepository.ShiftRow> shifts = reportQueryRepository.shiftRows(from, to);
        BigDecimal difference = shifts.stream().map(ReportQueryRepository.ShiftRow::difference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal handIn = shifts.stream().map(ReportQueryRepository.ShiftRow::handInCash)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CountPair incidents = reportQueryRepository.countIncidents(period.previous().startInstant(), from, to);
        ReportQueryRepository.IncidentPayout payout = reportQueryRepository.incidentPayouts(from, to);

        return new OperationsReportResponse(
                period.from(), period.to(), granularity,
                reportQueryRepository.ordersByStaff(from, to).stream()
                        .map(r -> new OperationsReportResponse.StaffOrders(r.username(), r.fullName(), r.orders()))
                        .sorted(Comparator.comparingLong(OperationsReportResponse.StaffOrders::orders).reversed())
                        .toList(),
                new OperationsReportResponse.Shifts(
                        shifts.size(),
                        shifts.stream().filter(r -> r.difference().signum() < 0).count(),
                        shifts.stream().filter(r -> r.difference().signum() > 0).count(),
                        difference, handIn),
                shifts.stream()
                        .map(r -> new OperationsReportResponse.ShiftRow(r.shiftId(), r.code(), r.openedByName(),
                                r.closedAt(), r.status(), r.difference(), r.handInCash()))
                        .toList(),
                new OperationsReportResponse.Incidents(
                        incidents.current(),
                        reportQueryRepository.incidentsByPeriod(from, to, granularity).stream()
                                .map(r -> new OperationsReportResponse.PeriodCount(r.period(), r.received()))
                                .toList(),
                        reportQueryRepository.incidentsByType(from, to).stream()
                                .sorted(Comparator.comparingLong(ReportQueryRepository.LabelCount::count).reversed())
                                .map(r -> new OperationsReportResponse.TypeCount(r.label(), r.count()))
                                .toList(),
                        reportQueryRepository.incidentsByStatus(from, to).stream()
                                .sorted(Comparator.comparingLong(ReportQueryRepository.LabelCount::count).reversed())
                                .map(r -> new OperationsReportResponse.StatusCount(r.label(), r.count()))
                                .toList(),
                        new OperationsReportResponse.Payouts(payout.cash(), payout.bankTransfer())),
                new OperationsReportResponse.Previous(incidents.previous()));
    }

    private static final int TOP_CUSTOMERS = 10;
    // shortcut: tối đa 50.000 khách cho CSV, không phân trang; thêm phân trang nếu vượt ngưỡng
    private static final int MAX_CUSTOMER_SPENDING = 50_000;

    /** Khách mới / quay lại, số đăng ký và top khách chi tiêu. */
    @Transactional(readOnly = true, timeout = 30)
    public CustomerReportResponse getCustomerReport(String rawFrom, String rawTo) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        Instant from = period.startInstant();
        Instant to = period.endInstant();
        CustomerCohort cohort = reportQueryRepository.customerCohort(period.previous().startInstant(), from, to);
        return new CustomerReportResponse(
                period.from(), period.to(), cohort.newCustomers(), cohort.returningCustomers(),
                percent(cohort.returningCustomers(), cohort.newCustomers() + cohort.returningCustomers()),
                reportQueryRepository.countRegistrations(from, to),
                new CustomerReportResponse.Previous(cohort.previousNewCustomers()),
                spendingRows(from, to, TOP_CUSTOMERS));
    }

    /** Chi tiêu mọi khách trong kỳ, chỉ để xuất CSV. */
    @Transactional(readOnly = true, timeout = 30)
    public List<CustomerReportResponse.CustomerSpendingRow> getCustomerSpending(String rawFrom, String rawTo) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        return spendingRows(period.startInstant(), period.endInstant(), MAX_CUSTOMER_SPENDING);
    }

    private List<CustomerReportResponse.CustomerSpendingRow> spendingRows(Instant from, Instant to, int limit) {
        return reportQueryRepository.topCustomers(from, to, limit).stream()
                .map((CustomerSpendingRecord r) -> new CustomerReportResponse.CustomerSpendingRow(
                        r.customerId(), r.customerCode(), r.customerName(), r.orderCount(), r.spent(), r.lastOrderAt()))
                .toList();
    }

    /** Hiệu quả khuyến mãi theo mã voucher đã lưu trên đơn. */
    @Transactional(readOnly = true, timeout = 30)
    public PromotionReportResponse getPromotionReport(String rawFrom, String rawTo, OrderChannel channel) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        Instant from = period.startInstant();
        Instant to = period.endInstant();
        Map<String, BigDecimal> revenueByCode = reportQueryRepository.voucherRevenue(from, to, channel).stream()
                .collect(Collectors.toMap(VoucherRevenueRecord::voucherCode, VoucherRevenueRecord::revenue, BigDecimal::add));
        Map<String, VoucherUsageRecord> usageByCode = reportQueryRepository.voucherUsage(from, to, channel).stream()
                .collect(Collectors.toMap(VoucherUsageRecord::voucherCode, u -> u, (x, y) -> x));
        // Doanh thu có thể đến từ đơn dùng voucher nhận ở kỳ trước nên lấy hợp mã của cả hai nguồn
        List<PromotionReportResponse.VoucherRow> rows = Stream.concat(usageByCode.keySet().stream(), revenueByCode.keySet().stream())
                .distinct()
                .map(code -> {
                    VoucherUsageRecord u = usageByCode.get(code);
                    return new PromotionReportResponse.VoucherRow(
                            code, u == null ? null : u.voucherId(), u == null ? 0 : u.uses(),
                            u == null ? BigDecimal.ZERO : u.discountTotal(),
                            revenueByCode.getOrDefault(code, BigDecimal.ZERO));
                })
                .sorted(Comparator.comparing(PromotionReportResponse.VoucherRow::revenue).reversed()
                        .thenComparing(Comparator.comparingLong(PromotionReportResponse.VoucherRow::uses).reversed()))
                .toList();
        return new PromotionReportResponse(
                period.from(), period.to(),
                rows.stream().mapToLong(PromotionReportResponse.VoucherRow::uses).sum(),
                rows.stream().map(PromotionReportResponse.VoucherRow::discountTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(PromotionReportResponse.VoucherRow::revenue).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows);
    }

    private static OrderReportResponse.ServiceRevenue toServiceRevenue(ServiceRevenueRecord r) {
        return new OrderReportResponse.ServiceRevenue(
                r.serviceId(), r.serviceCode(), r.serviceName(), r.orderCount(), r.revenue(),
                r.quantity(), r.unit());
    }

    /** Phần trăm HALF_UP 1 chữ số; mẫu 0 → null. */
    private static BigDecimal percent(long part, long whole) {
        return whole == 0 ? null : BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal oneDecimal(BigDecimal v) {
        return v == null ? null : v.setScale(1, RoundingMode.HALF_UP);
    }

    private static final class Bucket {
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal bankTransfer = BigDecimal.ZERO;
        BigDecimal online = BigDecimal.ZERO;
        BigDecimal offline = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        long paymentCount = 0;
    }
}
