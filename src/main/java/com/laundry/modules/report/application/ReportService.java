package com.laundry.modules.report.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.report.application.dto.DashboardSummaryResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo chỉ đọc. Mọi mốc ngày tính theo giờ Việt Nam: [from 00:00, to+1 00:00).
 * Doanh thu chỉ tính giao dịch CONFIRMED của đơn chưa hủy, theo thời điểm thanh toán (QĐ13).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_RANGE_DAYS = 366 * 2;

    private final EntityManager entityManager;
    private final SettingService settingService;

    public enum GroupBy { DAY, MONTH }

    // ---------- Doanh thu (UC26) ----------

    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(LocalDate from, LocalDate to, GroupBy groupBy) {
        validateRange(from, to);
        GroupBy group = groupBy == null ? GroupBy.DAY : groupBy;
        // Chỉ hai giá trị cố định được ghép vào SQL, không lấy chuỗi từ client
        String unit = group == GroupBy.MONTH ? "month" : "day";
        String format = group == GroupBy.MONTH ? "YYYY-MM" : "YYYY-MM-DD";

        List<Object[]> rows = rows(entityManager.createNativeQuery("""
                SELECT to_char(date_trunc('%s', p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh'), '%s') AS period,
                       p.method, COUNT(*), COALESCE(SUM(p.amount), 0)
                  FROM payments p
                  JOIN orders o ON o.id = p.order_id
                 WHERE p.status = 'CONFIRMED' AND o.status <> 'CANCELLED'
                   AND p.paid_at >= :start AND p.paid_at < :end
                 GROUP BY 1, 2
                 ORDER BY 1, 2
                """.formatted(unit, format)), from, to);

        Map<String, RevenueReportResponse.Period> periods = new LinkedHashMap<>();
        Map<String, long[]> methodCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> methodAmounts = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        long totalPayments = 0;
        for (Object[] r : rows) {
            String period = (String) r[0];
            String method = (String) r[1];
            long count = ((Number) r[2]).longValue();
            BigDecimal amount = decimal(r[3]);
            RevenueReportResponse.Period p = periods.getOrDefault(period,
                    new RevenueReportResponse.Period(period, 0, BigDecimal.ZERO, new LinkedHashMap<>()));
            p.byMethod().merge(method, amount, BigDecimal::add);
            periods.put(period, new RevenueReportResponse.Period(period, p.payments() + count,
                    p.amount().add(amount), p.byMethod()));
            methodCounts.computeIfAbsent(method, k -> new long[1])[0] += count;
            methodAmounts.merge(method, amount, BigDecimal::add);
            total = total.add(amount);
            totalPayments += count;
        }
        List<RevenueReportResponse.MethodTotal> byMethod = new ArrayList<>();
        methodAmounts.forEach((method, amount) ->
                byMethod.add(new RevenueReportResponse.MethodTotal(method, methodCounts.get(method)[0], amount)));
        return new RevenueReportResponse(from, to, group.name(), total, totalPayments, byMethod,
                List.copyOf(periods.values()));
    }

    // ---------- Đơn hàng & dịch vụ (UC27) ----------

    @Transactional(readOnly = true)
    public OrderReportResponse orders(LocalDate from, LocalDate to) {
        validateRange(from, to);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long totalOrders = 0;
        for (Object[] r : rows(entityManager.createNativeQuery("""
                SELECT o.status, COUNT(*) FROM orders o
                 WHERE o.received_at >= :start AND o.received_at < :end
                 GROUP BY o.status
                """), from, to)) {
            long count = ((Number) r[1]).longValue();
            byStatus.put((String) r[0], count);
            totalOrders += count;
        }

        Object[] late = single(entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                 WHERE o.received_at >= :start AND o.received_at < :end
                   AND o.status <> 'CANCELLED'
                   AND ((o.completed_at IS NOT NULL AND o.completed_at > o.due_at)
                     OR (o.completed_at IS NULL AND o.status IN ('RECEIVED', 'PROCESSING') AND o.due_at < NOW()))
                """), from, to);

        List<OrderReportResponse.OrderTypeStat> byOrderType = new ArrayList<>();
        for (Object[] r : rows(entityManager.createNativeQuery("""
                SELECT o.order_type, COUNT(*), COALESCE(SUM(o.total_amount), 0), COALESCE(AVG(o.total_amount), 0)
                  FROM orders o
                 WHERE o.received_at >= :start AND o.received_at < :end AND o.status <> 'CANCELLED'
                 GROUP BY o.order_type
                 ORDER BY o.order_type
                """), from, to)) {
            byOrderType.add(new OrderReportResponse.OrderTypeStat((String) r[0], ((Number) r[1]).longValue(),
                    decimal(r[2]), decimal(r[3]).setScale(0, java.math.RoundingMode.HALF_UP)));
        }

        List<OrderReportResponse.ServiceRevenue> byService = new ArrayList<>();
        for (Object[] r : rows(entityManager.createNativeQuery("""
                SELECT oi.service_id, MAX(oi.item_type), COUNT(DISTINCT o.id),
                       COALESCE(SUM(COALESCE(oi.billable_quantity, oi.quantity)), 0), COALESCE(SUM(oi.line_total), 0)
                  FROM order_items oi
                  JOIN orders o ON o.id = oi.order_id
                 WHERE o.received_at >= :start AND o.received_at < :end
                   AND o.status <> 'CANCELLED' AND o.payment_status = 'PAID'
                 GROUP BY oi.service_id
                 ORDER BY 5 DESC
                """), from, to)) {
            byService.add(new OrderReportResponse.ServiceRevenue(
                    r[0] == null ? null : ((Number) r[0]).longValue(), (String) r[1], ((Number) r[2]).longValue(),
                    decimal(r[3]), decimal(r[4])));
        }

        Object[] fees = single(entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(o.express_fee), 0), COALESCE(SUM(o.whites_fee), 0), COALESCE(SUM(o.discount_amount), 0)
                  FROM orders o
                 WHERE o.received_at >= :start AND o.received_at < :end
                   AND o.status <> 'CANCELLED' AND o.payment_status = 'PAID'
                """), from, to);

        return new OrderReportResponse(from, to, totalOrders, byStatus.getOrDefault("CANCELLED", 0L),
                ((Number) late[0]).longValue(), byStatus, byOrderType, byService,
                new OrderReportResponse.Surcharges(decimal(fees[0]), decimal(fees[1]), decimal(fees[2])));
    }

    // ---------- Dashboard (WF-B01) ----------

    @Transactional(readOnly = true)
    public DashboardSummaryResponse dashboard() {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Object[] received = single(entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o WHERE o.received_at >= :start AND o.received_at < :end
                """), today, today);
        Object[] revenue = single(entityManager.createNativeQuery("""
                SELECT COUNT(*), COALESCE(SUM(p.amount), 0)
                  FROM payments p JOIN orders o ON o.id = p.order_id
                 WHERE p.status = 'CONFIRMED' AND o.status <> 'CANCELLED'
                   AND p.paid_at >= :start AND p.paid_at < :end
                """), today, today);

        Map<String, Long> open = new LinkedHashMap<>();
        for (Object row : entityManager.createNativeQuery("""
                SELECT o.status, COUNT(*) FROM orders o
                 WHERE o.status IN ('RECEIVED', 'PROCESSING', 'COMPLETED')
                 GROUP BY o.status
                """).getResultList()) {
            Object[] r = (Object[]) row;
            open.put((String) r[0], ((Number) r[1]).longValue());
        }

        long late = count("""
                SELECT COUNT(*) FROM orders o
                 WHERE o.status IN ('RECEIVED', 'PROCESSING') AND o.due_at < NOW()
                """, Map.of());
        int overdueDays = settingService.getValue(SettingKey.OVERDUE_DAYS, Integer.class);
        long overdueStorage = count("""
                SELECT COUNT(*) FROM orders o
                 WHERE o.status = 'COMPLETED' AND o.completed_at < :threshold
                """, Map.of("threshold", Timestamp.from(Instant.now().minus(overdueDays, ChronoUnit.DAYS))));
        long pendingIncidents = count("SELECT COUNT(*) FROM incidents i WHERE i.status = 'PENDING'", Map.of());

        return new DashboardSummaryResponse(today, ((Number) received[0]).longValue(), decimal(revenue[1]),
                ((Number) revenue[0]).longValue(), late, overdueStorage,
                open.getOrDefault("RECEIVED", 0L), open.getOrDefault("PROCESSING", 0L),
                open.getOrDefault("COMPLETED", 0L), pendingIncidents);
    }

    // ---------- helpers ----------

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Vui lòng chọn từ ngày và đến ngày");
        }
        if (to.isBefore(from)) {
            throw new BusinessException("Đến ngày phải từ Từ ngày trở đi", "to");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessException("Khoảng thời gian tối đa 2 năm", "to");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query, LocalDate from, LocalDate to) {
        bindRange(query, from, to);
        return (List<Object[]>) query.getResultList();
    }

    private static Object[] single(Query query, LocalDate from, LocalDate to) {
        bindRange(query, from, to);
        Object result = query.getSingleResult();
        return result instanceof Object[] array ? array : new Object[]{result};
    }

    private long count(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private static void bindRange(Query query, LocalDate from, LocalDate to) {
        query.setParameter("start", Timestamp.from(from.atStartOfDay(VIETNAM_ZONE).toInstant()));
        query.setParameter("end", Timestamp.from(to.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant()));
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal d ? d : new BigDecimal(value.toString());
    }
}
