package com.laundry.modules.report.application;

import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import com.laundry.modules.payments.domain.PaymentMethod;
import com.laundry.modules.report.application.dto.DashboardResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.RevenuePaymentResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import com.laundry.modules.report.domain.Granularity;
import com.laundry.modules.report.domain.ReportPeriod;
import com.laundry.modules.report.domain.ReportQueryRepository;
import com.laundry.modules.report.domain.ReportQueryRepository.ActiveOrderTotals;
import com.laundry.modules.report.domain.ReportQueryRepository.ChannelOrderTotals;
import com.laundry.modules.report.domain.ReportQueryRepository.DashboardOrderCounts;
import com.laundry.modules.report.domain.ReportQueryRepository.OrderPeriodCount;
import com.laundry.modules.report.domain.ReportQueryRepository.OrderStatusCount;
import com.laundry.modules.report.domain.ReportQueryRepository.RevenueRecord;
import com.laundry.modules.report.domain.ReportQueryRepository.ServiceRevenueRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    ReportQueryRepository reportQueryRepository;
    OverdueDaysProvider overdueDaysProvider;

    /** BM11: doanh thu theo ngày/tháng, tách tiền mặt và chuyển khoản. */
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(String rawFrom, String rawTo, String rawGranularity,
                                                  OrderChannel channel) {
        return buildRevenueReport(ReportPeriod.of(rawFrom, rawTo), Granularity.parse(rawGranularity), channel);
    }

    // shortcut: tối đa 1000 giao dịch, đủ cho xem chi tiết một ngày; khoảng dài cần phân trang
    private static final int MAX_REVENUE_PAYMENTS = 1000;

    /** Giao dịch tạo nên doanh thu của khoảng ngày, để đối chiếu với báo cáo. */
    @Transactional(readOnly = true)
    public List<RevenuePaymentResponse> getRevenuePayments(String rawFrom, String rawTo, OrderChannel channel) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        return reportQueryRepository.findRevenuePayments(
                        period.startInstant(), period.endInstant(), channel, MAX_REVENUE_PAYMENTS)
                .stream().map(RevenuePaymentResponse::from).toList();
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

        return new RevenueReportResponse(
                period.from(), period.to(), granularity, rows,
                new RevenueReportResponse.Summary(
                        cash, bankTransfer, total, paymentCount, cashCount, bankTransferCount,
                        channels.entrySet().stream()
                                .map(e -> new RevenueReportResponse.ChannelRevenue(
                                        e.getKey(), e.getValue().total, e.getValue().paymentCount))
                                .toList()));
    }

    /** BM12: số đơn, tỉ lệ trễ và doanh thu theo dịch vụ. */
    @Transactional(readOnly = true)
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

        long totalOrders = counts.values().stream().mapToLong(Long::longValue).sum();
        long cancelledOrders = counts.getOrDefault(OrderStatus.CANCELLED.name(), 0L);
        long activeOrders = totalOrders - cancelledOrders;

        ActiveOrderTotals totals = reportQueryRepository.findActiveOrderTotals(start, end, channel);

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
                        totalOrders, cancelledOrders, activeOrders,
                        totals.lateOrders(), lateRate(totals.lateOrders(), activeOrders),
                        totals.totalAmount(), itemsRevenue, byChannel),
                byStatus,
                byService,
                rows);
    }

    /** Dashboard theo kỳ (giờ Việt Nam): số đơn, doanh thu, đơn trễ, đồ tồn, kèm kỳ liền trước để so sánh. */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(ReportPeriod period) {
        int storedAfterDays = overdueDaysProvider.getDays();
        Instant storedBefore = Instant.now().minus(storedAfterDays, ChronoUnit.DAYS);

        DashboardOrderCounts counts = reportQueryRepository.countDashboardOrders(
                period.startInstant(), period.endInstant(), storedBefore);
        // Dùng chung logic với BM11 để số doanh thu luôn khớp báo cáo doanh thu
        RevenueReportResponse.Summary revenue = buildRevenueReport(period, Granularity.DAY, null).summary();

        ReportPeriod prev = period.previous();
        long prevReceived = reportQueryRepository.countDashboardOrders(
                prev.startInstant(), prev.endInstant(), storedBefore).received();
        BigDecimal prevRevenue = buildRevenueReport(prev, Granularity.DAY, null).summary().total();

        return new DashboardResponse(
                period.from(),
                period.to(),
                new DashboardResponse.Orders(
                        counts.received(), counts.cancelled(), counts.delivered(),
                        counts.waiting(), counts.processing(), counts.readyForPickup(),
                        counts.pendingConfirmation(), counts.awaitingDropoff()),
                revenue,
                new DashboardResponse.Alerts(counts.lateOrders(), counts.storedOrders(), storedAfterDays),
                new DashboardResponse.Previous(prev.from(), prev.to(), prevReceived, prevRevenue));
    }

    private static OrderReportResponse.ServiceRevenue toServiceRevenue(ServiceRevenueRecord r) {
        return new OrderReportResponse.ServiceRevenue(
                r.serviceId(), r.serviceCode(), r.serviceName(), r.orderCount(), r.revenue(),
                r.quantity(), r.unit());
    }

    private static BigDecimal lateRate(long lateOrders, long activeOrders) {
        if (activeOrders == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(lateOrders)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(activeOrders), 2, RoundingMode.HALF_UP);
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
