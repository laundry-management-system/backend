package com.laundry.modules.report.domain;

import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.payments.domain.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
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

    public record RevenueRecord(String period, String method, String channel, BigDecimal total, long paymentCount) {}

    public record ChannelOrderTotals(String channel, long orders, BigDecimal revenue) {}

    public record OrderStatusCount(String status, long count) {}

    public record ActiveOrderTotals(long lateOrders, BigDecimal totalAmount) {}

    public record ServiceRevenueRecord(Long serviceId, String serviceCode, String serviceName,
                                       long orderCount, BigDecimal revenue, BigDecimal quantity, String unit) {}

    public record OrderPeriodCount(String period, long received) {}

    public record DashboardOrderCounts(long received, long cancelled, long delivered,
                                       long waiting, long processing, long readyForPickup,
                                       long lateOrders, long storedOrders,
                                       long pendingConfirmation, long awaitingDropoff) {}

    public record RevenuePaymentRecord(Long paymentId, Instant paidAt, String method, BigDecimal amount,
                                       String invoiceNo, Long orderId, String orderCode, String customerName,
                                       String channel) {}

    /** Đơn không vào số đơn / số liệu dịch vụ (FR-027): chưa nhận đồ hoặc đã hủy. */
    private static final List<String> NOT_COUNTED = List.of(OrderStatus.PENDING_CONFIRMATION.name(),
            OrderStatus.AWAITING_DROPOFF.name(), OrderStatus.CANCELLED.name());

    /**
     * Doanh thu theo kỳ, phương thức và kênh của đơn: chỉ giao dịch CONFIRMED, tính theo paid_at (BR-J04),
     * nhóm theo giờ VN. Không lọc kênh ở đây để service vừa lọc vừa tách đủ hai kênh.
     */
    public List<RevenueRecord> sumPaymentsByPeriodAndMethod(Instant from, Instant to, Granularity granularity) {
        // Mẫu to_char (YYYY-MM-DD hoặc YYYY-MM) truyền bằng tham số, không nối chuỗi vào câu SQL
        String sql = """
                SELECT to_char(p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh', :pattern) AS period,
                       p.method AS method,
                       o.channel AS channel,
                       COALESCE(SUM(p.amount), 0) AS total,
                       COUNT(*) AS payment_count
                FROM payments p
                JOIN orders o ON o.id = p.order_id
                WHERE p.status = :confirmed
                  AND p.paid_at >= :fromTs AND p.paid_at < :toTs
                GROUP BY 1, 2, 3
                ORDER BY 1, 2, 3
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
            result.add(new RevenueRecord((String) r[0], (String) r[1], (String) r[2], toMoney(r[3]), toLong(r[4])));
        }
        return result;
    }

    /** Số đơn nhận theo ngày/tháng (giờ VN, theo received_at), không tính đơn hủy / chưa nhận đồ (FR-027). */
    public List<OrderPeriodCount> countOrdersByPeriod(Instant from, Instant to, Granularity granularity,
                                                      OrderChannel channel) {
        String sql = """
                SELECT to_char(o.received_at AT TIME ZONE 'Asia/Ho_Chi_Minh', :pattern) AS period,
                       COUNT(*) AS cnt
                FROM orders o
                WHERE o.status NOT IN (:notCounted)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                GROUP BY 1
                ORDER BY 1
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("pattern", granularity.getSqlPattern())
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<OrderPeriodCount> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new OrderPeriodCount((String) r[0], toLong(r[1])));
        }
        return result;
    }

    /** Số đơn nhận trong kỳ theo trạng thái (gồm cả đơn đã hủy, không gồm đơn chưa nhận đồ). */
    public List<OrderStatusCount> countOrdersByStatus(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT o.status AS status, COUNT(*) AS cnt
                FROM orders o
                WHERE o.status NOT IN (:beforeDropoff)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                GROUP BY o.status
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("beforeDropoff", NOT_COUNTED.subList(0, 2))
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
    public ActiveOrderTotals findActiveOrderTotals(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN COALESCE(o.completed_at, o.delivered_at, now()) > o.due_at
                                         THEN 1 ELSE 0 END), 0) AS late_orders,
                       COALESCE(SUM(o.total_amount), 0) AS total_amount
                FROM orders o
                WHERE o.status NOT IN (:notCounted)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                """.formatted(channelClause(channel));

        Object[] r = (Object[]) withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();

        return new ActiveOrderTotals(toLong(r[0]), toMoney(r[1]));
    }

    /** Doanh thu theo dịch vụ từ các dòng đơn của đơn không hủy; dòng không gắn dịch vụ gom thành "Khác". */
    public List<ServiceRevenueRecord> sumRevenueByService(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT s.id AS service_id,
                       s.service_code AS service_code,
                       COALESCE(s.name, 'Khác') AS service_name,
                       COUNT(DISTINCT o.id) AS order_count,
                       COALESCE(SUM(oi.line_total), 0) AS revenue,
                       COALESCE(SUM(oi.quantity), 0) AS quantity,
                       s.unit AS unit
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN services s ON s.id = oi.service_id
                WHERE o.status NOT IN (:notCounted)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                GROUP BY s.id, s.service_code, s.name, s.unit
                ORDER BY revenue DESC, service_name
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("notCounted", NOT_COUNTED)
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
                    toMoney(r[4]),
                    toMoney(r[5]),
                    (String) r[6]));
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
                SELECT COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs) AS received,
                       COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                                          AND o.status = :cancelled) AS cancelled,
                       COUNT(*) FILTER (WHERE o.delivered_at >= :fromTs AND o.delivered_at < :toTs) AS delivered,
                       COUNT(*) FILTER (WHERE o.status = :received) AS waiting,
                       COUNT(*) FILTER (WHERE o.status = :processing) AS processing,
                       COUNT(*) FILTER (WHERE o.status = :completed) AS ready_for_pickup,
                       COUNT(*) FILTER (WHERE o.status IN (:received, :processing) AND o.due_at < now()) AS late_orders,
                       COUNT(*) FILTER (WHERE o.status = :completed AND o.completed_at < :storedBefore) AS stored_orders,
                       COUNT(*) FILTER (WHERE o.status = :pendingConfirmation) AS pending_confirmation,
                       COUNT(*) FILTER (WHERE o.status = :awaitingDropoff) AS awaiting_dropoff
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
                .setParameter("pendingConfirmation", OrderStatus.PENDING_CONFIRMATION.name())
                .setParameter("awaitingDropoff", OrderStatus.AWAITING_DROPOFF.name())
                .getSingleResult();

        return new DashboardOrderCounts(toLong(r[0]), toLong(r[1]), toLong(r[2]), toLong(r[3]),
                toLong(r[4]), toLong(r[5]), toLong(r[6]), toLong(r[7]),
                toLong(r[8]), toLong(r[9]));
    }

    /**
     * Từng giao dịch tạo nên doanh thu trong [from, to): cùng điều kiện với {@link #sumPaymentsByPeriodAndMethod}
     * để tổng luôn khớp báo cáo. Mới nhất trước.
     */
    public List<RevenuePaymentRecord> findRevenuePayments(Instant from, Instant to, OrderChannel channel, int limit) {
        String sql = """
                SELECT p.id, p.paid_at, p.method, p.amount, p.invoice_no, o.id, o.order_code, o.customer_name,
                       o.channel
                FROM payments p
                JOIN orders o ON o.id = p.order_id
                WHERE p.status = :confirmed
                  AND p.paid_at >= :fromTs AND p.paid_at < :toTs %s
                ORDER BY p.paid_at DESC
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("confirmed", PaymentStatus.CONFIRMED.name())
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .setMaxResults(limit)
                .getResultList();

        List<RevenuePaymentRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new RevenuePaymentRecord(
                    ((Number) r[0]).longValue(), toInstant(r[1]), (String) r[2], toMoney(r[3]),
                    (String) r[4], ((Number) r[5]).longValue(), (String) r[6], (String) r[7], (String) r[8]));
        }
        return result;
    }

    /**
     * Số đơn và tiền thực thu (payments CONFIRMED) theo kênh của các đơn nhận trong kỳ (received_at),
     * không tính đơn hủy / chưa nhận đồ. Không lọc kênh: báo cáo luôn tách đủ hai kênh.
     */
    public List<ChannelOrderTotals> sumOrdersByChannel(Instant from, Instant to) {
        String sql = """
                SELECT o.channel AS channel,
                       COUNT(*) AS orders,
                       COALESCE(SUM(pay.amount), 0) AS revenue
                FROM orders o
                LEFT JOIN (SELECT p.order_id, SUM(p.amount) AS amount
                           FROM payments p
                           WHERE p.status = :confirmed
                           GROUP BY p.order_id) pay ON pay.order_id = o.id
                WHERE o.status NOT IN (:notCounted)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs
                GROUP BY o.channel
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("confirmed", PaymentStatus.CONFIRMED.name())
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<ChannelOrderTotals> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new ChannelOrderTotals((String) r[0], toLong(r[1]), toMoney(r[2])));
        }
        return result;
    }

    // Mệnh đề cố định, giá trị kênh vẫn truyền bằng tham số
    private static String channelClause(OrderChannel channel) {
        return channel == null ? "" : "AND o.channel = :channel";
    }

    private static Query withChannel(Query query, OrderChannel channel) {
        return channel == null ? query : query.setParameter("channel", channel.name());
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof OffsetDateTime odt) return odt.toInstant();
        return ((Timestamp) value).toInstant();
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
