package com.laundry.modules.report.domain;

import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.payments.domain.PaymentStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn tổng hợp chỉ đọc cho báo cáo. Các khoảng thời gian đều là nửa mở [from, to).
 * Gom nhóm trong DB, không load entity.
 */
@Repository
@RequiredArgsConstructor
public class ReportQueryRepository {

    private final EntityManager entityManager;

    public record RevenueRecord(String period, String method, BigDecimal total, long paymentCount) {}

    public record OrderStatusCount(String status, long count) {}

    public record ActiveOrderTotals(long lateOrders, BigDecimal totalAmount) {}

    public record ServiceRevenueRecord(Long serviceId, String serviceCode, String serviceName,
                                       long orderCount, BigDecimal revenue) {}

    public record DashboardOrderCounts(long receivedToday, long cancelledToday, long deliveredToday,
                                       long waiting, long processing, long readyForPickup,
                                       long lateOrders, long storedOrders) {}

    public record OrderTypeRecord(String orderType, long orderCount, BigDecimal totalAmount, BigDecimal averageAmount) {}

    public record SurchargeTotals(BigDecimal expressFee, BigDecimal whitesFee, BigDecimal discount) {}

    /** Số đơn, tổng và trung bình tổng tiền theo loại đơn (Tiêu chuẩn / Giặt nhanh), không tính đơn hủy. */
    public List<OrderTypeRecord> sumOrdersByType(Instant from, Instant to) {
        String sql = """
                SELECT o.order_type AS order_type,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(o.total_amount), 0) AS total_amount,
                       COALESCE(ROUND(AVG(o.total_amount), 0), 0) AS average_amount
                FROM orders o
                WHERE o.status <> :cancelled
                  AND o.received_at >= :fromTs AND o.received_at < :toTs
                GROUP BY o.order_type
                ORDER BY o.order_type
                """;
        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("cancelled", OrderStatus.CANCELLED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();
        List<OrderTypeRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new OrderTypeRecord((String) r[0], toLong(r[1]), toMoney(r[2]), toMoney(r[3])));
        }
        return result;
    }

    /** Tổng phụ phí giặt nhanh, phụ phí đồ trắng và giảm giá của các đơn không hủy. */
    public SurchargeTotals sumSurcharges(Instant from, Instant to) {
        String sql = """
                SELECT COALESCE(SUM(o.express_fee), 0), COALESCE(SUM(o.whites_fee), 0),
                       COALESCE(SUM(o.discount_amount), 0)
                FROM orders o
                WHERE o.status <> :cancelled
                  AND o.received_at >= :fromTs AND o.received_at < :toTs
                """;
        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("cancelled", OrderStatus.CANCELLED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new SurchargeTotals(toMoney(r[0]), toMoney(r[1]), toMoney(r[2]));
    }

    /** Phiếu sự cố đang chờ Admin duyệt. */
    public long countPendingIncidents() {
        return toLong(entityManager.createNativeQuery("SELECT COUNT(*) FROM incidents i WHERE i.status = 'PENDING'")
                .getSingleResult());
    }

    /** Doanh thu theo kỳ và phương thức: chỉ giao dịch CONFIRMED, tính theo paid_at (BR-J04), nhóm theo giờ VN. */
    public List<RevenueRecord> sumPaymentsByPeriodAndMethod(Instant from, Instant to, Granularity granularity) {
        // Mẫu to_char (YYYY-MM-DD hoặc YYYY-MM) truyền bằng tham số, không nối chuỗi vào câu SQL
        String sql = """
                SELECT to_char(p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh', :pattern) AS period,
                       p.method AS method,
                       COALESCE(SUM(p.amount), 0) AS total,
                       COUNT(*) AS payment_count
                FROM payments p
                WHERE p.status = :confirmed
                  AND p.paid_at >= :fromTs AND p.paid_at < :toTs
                GROUP BY 1, 2
                ORDER BY 1, 2
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("pattern", granularity.getSqlPattern())
                .setParameter("confirmed", PaymentStatus.CONFIRMED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<RevenueRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new RevenueRecord((String) r[0], (String) r[1], toMoney(r[2]), toLong(r[3])));
        }
        return result;
    }

    /** Số đơn nhận trong kỳ theo trạng thái (gồm cả đơn đã hủy). */
    public List<OrderStatusCount> countOrdersByStatus(Instant from, Instant to) {
        String sql = """
                SELECT o.status AS status, COUNT(*) AS cnt
                FROM orders o
                WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                GROUP BY o.status
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<OrderStatusCount> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new OrderStatusCount((String) r[0], toLong(r[1])));
        }
        return result;
    }

    /**
     * Số đơn trễ và tổng tiền của các đơn không hủy nhận trong kỳ.
     * Trễ: thời điểm hoàn thành (chưa có thì lúc giao, chưa có nữa thì hiện tại) vượt due_at.
     */
    public ActiveOrderTotals findActiveOrderTotals(Instant from, Instant to) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN COALESCE(o.completed_at, o.delivered_at, now()) > o.due_at
                                         THEN 1 ELSE 0 END), 0) AS late_orders,
                       COALESCE(SUM(o.total_amount), 0) AS total_amount
                FROM orders o
                WHERE o.status <> :cancelled
                  AND o.received_at >= :fromTs AND o.received_at < :toTs
                """;

        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("cancelled", OrderStatus.CANCELLED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();

        return new ActiveOrderTotals(toLong(r[0]), toMoney(r[1]));
    }

    /** Doanh thu theo dịch vụ từ các dòng đơn của đơn không hủy; dòng không gắn dịch vụ gom thành "Khác". */
    public List<ServiceRevenueRecord> sumRevenueByService(Instant from, Instant to) {
        String sql = """
                SELECT s.id AS service_id,
                       s.service_code AS service_code,
                       COALESCE(s.name, 'Khác') AS service_name,
                       COUNT(DISTINCT o.id) AS order_count,
                       COALESCE(SUM(oi.line_total), 0) AS revenue
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN services s ON s.id = oi.service_id
                WHERE o.status <> :cancelled
                  AND o.received_at >= :fromTs AND o.received_at < :toTs
                GROUP BY s.id, s.service_code, s.name
                ORDER BY revenue DESC, service_name
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("cancelled", OrderStatus.CANCELLED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<ServiceRevenueRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new ServiceRevenueRecord(
                    r[0] == null ? null : ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    toLong(r[3]),
                    toMoney(r[4])));
        }
        return result;
    }

    /**
     * Các bộ đếm của dashboard trong một lần quét bảng orders.
     * Đơn trễ: Mới nhận / Đang xử lý mà due_at đã qua. Đồ tồn: Hoàn thành mà completed_at trước storedBefore
     * (cùng định nghĩa với danh sách đồ tồn hiện có).
     */
    public DashboardOrderCounts countDashboardOrders(Instant from, Instant to, Instant storedBefore) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs) AS received_today,
                       COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                                          AND o.status = :cancelled) AS cancelled_today,
                       COUNT(*) FILTER (WHERE o.delivered_at >= :fromTs AND o.delivered_at < :toTs) AS delivered_today,
                       COUNT(*) FILTER (WHERE o.status = :received) AS waiting,
                       COUNT(*) FILTER (WHERE o.status = :processing) AS processing,
                       COUNT(*) FILTER (WHERE o.status = :completed) AS ready_for_pickup,
                       COUNT(*) FILTER (WHERE o.status IN (:received, :processing) AND o.due_at < now()) AS late_orders,
                       COUNT(*) FILTER (WHERE o.status = :completed AND o.completed_at < :storedBefore) AS stored_orders
                FROM orders o
                WHERE o.status NOT IN (:delivered, :cancelled)
                   OR o.received_at >= :fromTs
                   OR o.delivered_at >= :fromTs
                """;

        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .setParameter("storedBefore", storedBefore)
                .setParameter("received", OrderStatus.RECEIVED.name())
                .setParameter("processing", OrderStatus.PROCESSING.name())
                .setParameter("completed", OrderStatus.COMPLETED.name())
                .setParameter("delivered", OrderStatus.DELIVERED.name())
                .setParameter("cancelled", OrderStatus.CANCELLED.name())
                .getSingleResult();

        return new DashboardOrderCounts(toLong(r[0]), toLong(r[1]), toLong(r[2]), toLong(r[3]),
                toLong(r[4]), toLong(r[5]), toLong(r[6]), toLong(r[7]));
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static BigDecimal toMoney(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
