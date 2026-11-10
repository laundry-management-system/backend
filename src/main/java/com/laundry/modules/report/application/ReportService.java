package com.laundry.modules.report.application;

import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import com.laundry.modules.payments.domain.PaymentMethod;
import com.laundry.modules.report.application.dto.DashboardResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import com.laundry.modules.report.domain.Granularity;
import com.laundry.modules.report.domain.ReportPeriod;
import com.laundry.modules.report.domain.ReportQueryRepository;
import com.laundry.modules.report.domain.ReportQueryRepository.ActiveOrderTotals;
import com.laundry.modules.report.domain.ReportQueryRepository.DashboardOrderCounts;
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
    public RevenueReportResponse getRevenueReport(String rawFrom, String rawTo, String rawGranularity) {
        return buildRevenueReport(ReportPeriod.of(rawFrom, rawTo), Granularity.parse(rawGranularity));
    }

    private RevenueReportResponse buildRevenueReport(ReportPeriod period, Granularity granularity) {
        List<RevenueRecord> records = reportQueryRepository.sumPaymentsByPeriodAndMethod(
                period.startInstant(), period.endInstant(), granularity);

        // Điền sẵn mọi kỳ với 0 để FE vẽ biểu đồ không bị đứt
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (String label : granularity.periods(period.from(), period.to())) {
            buckets.put(label, new Bucket());
        }

        for (RevenueRecord row : records) {
            Bucket bucket = buckets.computeIfAbsent(row.period(), k -> new Bucket());
            if (PaymentMethod.CASH.name().equals(row.method())) {
                bucket.cash = bucket.cash.add(row.total());
            } else if (PaymentMethod.BANK_TRANSFER.name().equals(row.method())) {
                bucket.bankTransfer = bucket.bankTransfer.add(row.total());
            }
            // total cộng mọi phương thức để không mất tiền khi có phương thức mới
            bucket.total = bucket.total.add(row.total());
            bucket.paymentCount += row.paymentCount();
        }

        List<RevenueReportResponse.Row> rows = buckets.entrySet().stream()
                .map(e -> new RevenueReportResponse.Row(
                        e.getKey(), e.getValue().cash, e.getValue().bankTransfer,
                        e.getValue().total, e.getValue().paymentCount))
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
                new RevenueReportResponse.Summary(cash, bankTransfer, total, paymentCount));
    }

    /** BM12: số đơn, tỉ lệ trễ và doanh thu theo dịch vụ. */
    @Transactional(readOnly = true)
    public OrderReportResponse getOrderReport(String rawFrom, String rawTo) {
        ReportPeriod period = ReportPeriod.of(rawFrom, rawTo);
        Instant start = period.startInstant();
        Instant end = period.endInstant();

        Map<String, Long> counts = reportQueryRepository.countOrdersByStatus(start, end).stream()
                .collect(Collectors.toMap(OrderStatusCount::status, OrderStatusCount::count));

        List<OrderReportResponse.StatusCount> byStatus = Arrays.stream(OrderStatus.values())
                .map(s -> new OrderReportResponse.StatusCount(
                        s.name(), s.getDescription(), counts.getOrDefault(s.name(), 0L)))
                .toList();

        long totalOrders = counts.values().stream().mapToLong(Long::longValue).sum();
        long cancelledOrders = counts.getOrDefault(OrderStatus.CANCELLED.name(), 0L);
        long activeOrders = totalOrders - cancelledOrders;

        ActiveOrderTotals totals = reportQueryRepository.findActiveOrderTotals(start, end);

        List<OrderReportResponse.ServiceRevenue> byService = reportQueryRepository.sumRevenueByService(start, end)
                .stream()
                .map(ReportService::toServiceRevenue)
                .toList();

        BigDecimal itemsRevenue = byService.stream()
                .map(OrderReportResponse.ServiceRevenue::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderReportResponse.OrderTypeStat> byOrderType = reportQueryRepository.sumOrdersByType(start, end)
                .stream()
                .map(r -> new OrderReportResponse.OrderTypeStat(
                        r.orderType(), r.orderCount(), r.totalAmount(), r.averageAmount()))
                .toList();
        ReportQueryRepository.SurchargeTotals fees = reportQueryRepository.sumSurcharges(start, end);

        return new OrderReportResponse(
                period.from(), period.to(),
                new OrderReportResponse.Summary(
                        totalOrders, cancelledOrders, activeOrders,
                        totals.lateOrders(), lateRate(totals.lateOrders(), activeOrders),
                        totals.totalAmount(), itemsRevenue),
                byStatus,
                byService,
                byOrderType,
                new OrderReportResponse.Surcharges(fees.expressFee(), fees.whitesFee(), fees.discount()));
    }

    /** Dashboard trong ngày (giờ Việt Nam): số đơn, doanh thu, đơn trễ, đồ tồn. */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        ReportPeriod today = ReportPeriod.today();
        int storedAfterDays = overdueDaysProvider.getDays();
        Instant storedBefore = Instant.now().minus(storedAfterDays, ChronoUnit.DAYS);

        DashboardOrderCounts counts = reportQueryRepository.countDashboardOrders(
                today.startInstant(), today.endInstant(), storedBefore);
        // Dùng chung logic với BM11 để số doanh thu hôm nay luôn khớp báo cáo doanh thu
        RevenueReportResponse.Summary revenue = buildRevenueReport(today, Granularity.DAY).summary();

        return new DashboardResponse(
                today.from(),
                new DashboardResponse.Orders(
                        counts.receivedToday(), counts.cancelledToday(), counts.deliveredToday(),
                        counts.waiting(), counts.processing(), counts.readyForPickup()),
                revenue,
                new DashboardResponse.Alerts(counts.lateOrders(), counts.storedOrders(), storedAfterDays,
                        reportQueryRepository.countPendingIncidents()));
    }

    private static OrderReportResponse.ServiceRevenue toServiceRevenue(ServiceRevenueRecord r) {
        return new OrderReportResponse.ServiceRevenue(
                r.serviceId(), r.serviceCode(), r.serviceName(), r.orderCount(), r.revenue());
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
        BigDecimal total = BigDecimal.ZERO;
        long paymentCount = 0;
    }
}
