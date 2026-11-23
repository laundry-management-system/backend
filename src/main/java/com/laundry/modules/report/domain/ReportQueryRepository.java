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

    public record ServiceRevenueRecord(Long serviceId, String serviceCode, String serviceName,
                                       long orderCount, BigDecimal revenue, BigDecimal quantity, String unit) {}

    public record OrderPeriodCount(String period, long received) {}

    public record LabelCount(String label, long count) {}

    public record IncidentPayout(BigDecimal cash, BigDecimal bankTransfer) {}

    public record RevenuePaymentRecord(Long paymentId, Instant paidAt, String method, BigDecimal amount,
                                       String invoiceNo, Long orderId, String orderCode, String customerName,
                                       String channel) {}

    /** Giá trị kỳ hiện tại và kỳ trước (c+p) lấy trong một truy vấn. */
    public record CountPair(long current, long previous) {}

    public record RevenueTotals(BigDecimal total, long paidOrders, BigDecimal previousTotal, long previousPaidOrders) {}

    public record OrderSummaryCounts(long received, long previousReceived, long delivered, long active,
                                     BigDecimal totalAmount) {}

    public record DueLateCounts(long due, long late) {}

    /** code = COALESCE(reason_code,'OTHER'); online = phần thuộc đơn ONLINE. */
    public record CancelReasonCount(String code, long count, long online) {}

    /** Giờ có thể null khi không có đơn nào. */
    public record ProcessingStats(long completedOrders, BigDecimal avgHours, BigDecimal medianHours, long onTimeOrders) {}

    public record WashModeTotals(Long washModeId, String washModeName, long orders, BigDecimal amount) {}

    public record OnlineFunnel(long requests, long confirmed, BigDecimal avgConfirmMinutes) {}

    public record LiveOrderCounts(long waiting, long processing, long readyForPickup,
                                  long pendingConfirmation, long awaitingDropoff, long lateOrders) {}

    public record DueSoonOrder(Long orderId, String orderCode, String customerName, String status, Instant dueAt) {}

    public record StoredOrder(Long orderId, String orderCode, String customerName, Instant readyAt) {}

    public record StoredOrders(long total, List<StoredOrder> rows) {}

    public record CustomerCohort(long newCustomers, long returningCustomers, long previousNewCustomers) {}

    /** lastOrderAt có thể null (khách chỉ có thanh toán, không có đơn nhận trước to). */
    public record CustomerSpendingRecord(Long customerId, String customerCode, String customerName, long orderCount,
                                         BigDecimal spent, Instant lastOrderAt) {}

    /** voucherId = MAX(voucher_id), có thể null. */
    public record VoucherUsageRecord(String voucherCode, Long voucherId, long uses, BigDecimal discountTotal) {}

    public record VoucherRevenueRecord(String voucherCode, BigDecimal revenue) {}

    /** fullName = null khi username không còn trong users; FE hiển thị username. */
    public record StaffOrderCount(String username, String fullName, long orders) {}

    public record ShiftRow(Long shiftId, String code, String openedByName, Instant closedAt, String status,
                           BigDecimal difference, BigDecimal handInCash) {}

    /** Đơn không vào số đơn / số liệu dịch vụ (FR-027): chưa nhận đồ hoặc đã hủy. */
    private static final List<String> NOT_COUNTED = List.of(OrderStatus.PENDING_CONFIRMATION.name(),
            OrderStatus.AWAITING_DROPOFF.name(), OrderStatus.CANCELLED.name());

    // Fragment dùng chung (FR-003): trạng thái viết literal để planner chọn đúng index; kỳ vẫn bind :fromTs/:toTs.

    /** Lần sẵn sàng trả đầu tiên; join sau khi đã lọc tập đơn ứng viên, mỗi đơn một lần probe idx_osh_order_id. */
    private static final String READY_AT = """
            LEFT JOIN LATERAL (
              SELECT MIN(h.created_at) AS ready_at FROM order_status_history h
              WHERE h.order_id = o.id AND h.action_type = 'STATUS_CHANGE' AND h.new_value IN ('COMPLETED','DELIVERED')
            ) r ON true
            """;

    /** Giá trị ready_at, fallback khi đơn không có dòng lịch sử. Cần {@link #READY_AT}. */
    private static final String READY_AT_VALUE = "COALESCE(r.ready_at, o.completed_at, o.delivered_at)";

    /** Đơn trễ hiện tại. */
    private static final String LATE_NOW = "o.status IN ('RECEIVED','PROCESSING') AND o.due_at < now()";

    /** Đơn trễ trong kỳ: hẹn trả trong [from,to) mà chưa sẵn sàng đúng hạn. Cần {@link #READY_AT}. */
    private static final String LATE_BY_PERIOD = "o.status NOT IN ('PENDING_CONFIRMATION','AWAITING_DROPOFF','CANCELLED')"
            + " AND o.due_at >= :fromTs AND o.due_at < :toTs AND COALESCE(" + READY_AT_VALUE + ", now()) > o.due_at";

    /** Sự kiện hủy trong kỳ trên order_status_history h. */
    private static final String CANCEL_EVENT = "h.action_type = 'CANCELLED' AND h.created_at >= :fromTs AND h.created_at < :toTs";

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

    /**
     * Tổng doanh thu và số đơn đã thanh toán (c+p): payment CONFIRMED theo paid_at, quét [prevFrom, to).
     * Cùng điều kiện với {@link #sumPaymentsByPeriodAndMethod}.
     */
    public RevenueTotals sumRevenueTotals(Instant prevFrom, Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COALESCE(SUM(p.amount) FILTER (WHERE p.paid_at >= :fromTs), 0),
                       COUNT(DISTINCT p.order_id) FILTER (WHERE p.paid_at >= :fromTs),
                       COALESCE(SUM(p.amount) FILTER (WHERE p.paid_at < :fromTs), 0),
                       COUNT(DISTINCT p.order_id) FILTER (WHERE p.paid_at < :fromTs)
                FROM payments p
                JOIN orders o ON o.id = p.order_id
                WHERE p.status = :confirmed
                  AND p.paid_at >= :prevFromTs AND p.paid_at < :toTs %s
                """.formatted(channelClause(channel));

        Object[] r = (Object[]) withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("confirmed", PaymentStatus.CONFIRMED.name())
                .setParameter("prevFromTs", prevFrom)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new RevenueTotals(toMoney(r[0]), toLong(r[1]), toMoney(r[2]), toLong(r[3]));
    }

    /**
     * Đơn nhận (c+p, theo received_at, loại NOT_COUNTED), đã giao (delivered_at), đang mở (nhận trong kỳ, chưa
     * DELIVERED/CANCELLED) và tổng tiền đơn nhận trong kỳ, một lần quét.
     */
    public OrderSummaryCounts countOrderSummary(Instant prevFrom, Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                                          AND o.status NOT IN (:notCounted)),
                       COUNT(*) FILTER (WHERE o.received_at >= :prevFromTs AND o.received_at < :fromTs
                                          AND o.status NOT IN (:notCounted)),
                       COUNT(*) FILTER (WHERE o.delivered_at >= :fromTs AND o.delivered_at < :toTs),
                       COUNT(*) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                                          AND o.status NOT IN ('DELIVERED','CANCELLED')),
                       COALESCE(SUM(o.total_amount) FILTER (WHERE o.received_at >= :fromTs AND o.received_at < :toTs
                                          AND o.status NOT IN (:notCounted)), 0)
                FROM orders o
                WHERE ((o.received_at >= :prevFromTs AND o.received_at < :toTs)
                       OR (o.delivered_at >= :fromTs AND o.delivered_at < :toTs)) %s
                """.formatted(channelClause(channel));

        Object[] r = (Object[]) withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("prevFromTs", prevFrom)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new OrderSummaryCounts(toLong(r[0]), toLong(r[1]), toLong(r[2]), toLong(r[3]), toMoney(r[4]));
    }

    /** due = đơn đã nhận (không chờ xác nhận/chờ gửi đồ/hủy) có due_at trong kỳ; late = {@link #LATE_BY_PERIOD}. Tỉ lệ trễ = late / due (R4). */
    public DueLateCounts countDueAndLate(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COUNT(*), COUNT(*) FILTER (WHERE %2$s)
                FROM orders o %1$s
                WHERE o.status NOT IN ('PENDING_CONFIRMATION','AWAITING_DROPOFF','CANCELLED')
                  AND o.due_at >= :fromTs AND o.due_at < :toTs %3$s
                """.formatted(READY_AT, LATE_BY_PERIOD, channelClause(channel));

        Object[] r = (Object[]) withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new DueLateCounts(toLong(r[0]), toLong(r[1]));
    }

    /** Sự kiện hủy trong kỳ ({@link #CANCEL_EVENT}) theo lý do; chỉ trả code có dòng, service bổ sung code 0. */
    public List<CancelReasonCount> countCancelledByReason(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COALESCE(h.reason_code, 'OTHER') AS code,
                       COUNT(*),
                       COUNT(*) FILTER (WHERE o.channel = 'ONLINE')
                FROM order_status_history h
                JOIN orders o ON o.id = h.order_id
                WHERE %s %s
                GROUP BY 1
                """.formatted(CANCEL_EVENT, channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<CancelReasonCount> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new CancelReasonCount((String) r[0], toLong(r[1]), toLong(r[2])));
        }
        return result;
    }

    /**
     * Thời gian xử lý của đơn sẵn sàng lần đầu trong kỳ: ứng viên = đơn có dòng STATUS_CHANGE → COMPLETED|DELIVERED
     * trong kỳ, giữ ready_at ≥ from. Giờ = ready_at - received_at; đúng hạn khi ready_at ≤ due_at.
     */
    public ProcessingStats processingStats(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT COUNT(*),
                       AVG(x.hours),
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY x.hours),
                       COUNT(*) FILTER (WHERE x.ready_at <= x.due_at)
                FROM (
                  SELECT %2$s AS ready_at, o.due_at,
                         EXTRACT(EPOCH FROM (%2$s - o.received_at)) / 3600 AS hours
                  FROM orders o %1$s
                  WHERE o.id IN (SELECT c.order_id FROM order_status_history c
                                 WHERE c.action_type = 'STATUS_CHANGE' AND c.new_value IN ('COMPLETED','DELIVERED')
                                   AND c.created_at >= :fromTs AND c.created_at < :toTs)
                    AND o.received_at IS NOT NULL %3$s
                ) x
                WHERE x.ready_at >= :fromTs
                """.formatted(READY_AT, READY_AT_VALUE, channelClause(channel));

        Object[] r = (Object[]) withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new ProcessingStats(toLong(r[0]), toDecimal(r[1]), toDecimal(r[2]), toLong(r[3]));
    }

    /** Đơn nhận trong kỳ theo hình thức giặt (snapshot trên đơn); NULL = chưa có hình thức. Tiền giảm dần. */
    public List<WashModeTotals> sumOrdersByWashMode(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT o.wash_mode_id, o.wash_mode_name, COUNT(*) AS orders,
                       COALESCE(SUM(o.total_amount), 0) AS amount
                FROM orders o
                WHERE o.status NOT IN (:notCounted)
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                GROUP BY o.wash_mode_id, o.wash_mode_name
                ORDER BY amount DESC, o.wash_mode_name
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<WashModeTotals> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new WashModeTotals(r[0] == null ? null : ((Number) r[0]).longValue(), (String) r[1],
                    toLong(r[2]), toMoney(r[3])));
        }
        return result;
    }

    /** Yêu cầu online tạo trong kỳ (created_at), số đã xác nhận và phút chờ xác nhận trung bình. */
    public OnlineFunnel onlineFunnel(Instant from, Instant to) {
        String sql = """
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE o.confirmed_at IS NOT NULL),
                       AVG(EXTRACT(EPOCH FROM (o.confirmed_at - o.created_at)) / 60)
                           FILTER (WHERE o.confirmed_at IS NOT NULL)
                FROM orders o
                WHERE o.channel = 'ONLINE' AND o.created_at >= :fromTs AND o.created_at < :toTs
                """;

        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new OnlineFunnel(toLong(r[0]), toLong(r[1]), toDecimal(r[2]));
    }

    /** Bộ đếm tức thời trên đơn đang mở; đơn trễ dùng {@link #LATE_NOW}. */
    public LiveOrderCounts countLiveOrders() {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE o.status = 'RECEIVED'),
                       COUNT(*) FILTER (WHERE o.status = 'PROCESSING'),
                       COUNT(*) FILTER (WHERE o.status = 'COMPLETED'),
                       COUNT(*) FILTER (WHERE o.status = 'PENDING_CONFIRMATION'),
                       COUNT(*) FILTER (WHERE o.status = 'AWAITING_DROPOFF'),
                       COUNT(*) FILTER (WHERE %s)
                FROM orders o
                WHERE o.status IN ('RECEIVED','PROCESSING','COMPLETED','PENDING_CONFIRMATION','AWAITING_DROPOFF')
                """.formatted(LATE_NOW);

        Object[] r = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();
        return new LiveOrderCounts(toLong(r[0]), toLong(r[1]), toLong(r[2]), toLong(r[3]), toLong(r[4]),
                toLong(r[5]));
    }

    /** Đơn RECEIVED/PROCESSING hẹn trả trong 24 giờ tới, gần hạn nhất trước, tối đa 10. */
    public List<DueSoonOrder> findDueSoonOrders() {
        String sql = """
                SELECT o.id, o.order_code, o.customer_name, o.status, o.due_at
                FROM orders o
                WHERE o.status IN ('RECEIVED','PROCESSING')
                  AND o.due_at >= now() AND o.due_at < now() + interval '24 hours'
                ORDER BY o.due_at, o.id
                LIMIT 10
                """;

        List<?> rows = entityManager.createNativeQuery(sql).getResultList();
        List<DueSoonOrder> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new DueSoonOrder(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (String) r[3],
                    toInstant(r[4])));
        }
        return result;
    }

    /** Đồ tồn: COMPLETED có completed_at trước storedBefore, chờ lâu nhất trước, tối đa 10; total là tổng số. */
    public StoredOrders findStoredOrders(Instant storedBefore) {
        String sql = """
                SELECT o.id, o.order_code, o.customer_name, o.completed_at, COUNT(*) OVER () AS total
                FROM orders o
                WHERE o.status = 'COMPLETED' AND o.completed_at < :storedBefore
                ORDER BY o.completed_at, o.id
                LIMIT 10
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("storedBefore", storedBefore)
                .getResultList();
        List<StoredOrder> list = new ArrayList<>(rows.size());
        long total = 0;
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            list.add(new StoredOrder(((Number) r[0]).longValue(), (String) r[1], (String) r[2], toInstant(r[3])));
            total = toLong(r[4]);
        }
        return new StoredOrders(total, list);
    }

    /**
     * Khách có đơn nhận trong kỳ (customer_id không null): mới khi không có đơn trước from, quay lại khi có.
     * Kỳ trước cùng mẫu với mốc prevFrom; một lần quét [prevFrom, to).
     */
    public CustomerCohort customerCohort(Instant prevFrom, Instant from, Instant to) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE c.in_cur AND NOT c.before_cur),
                       COUNT(*) FILTER (WHERE c.in_cur AND c.before_cur),
                       COUNT(*) FILTER (WHERE c.in_prev AND NOT c.before_prev)
                FROM (
                  SELECT bool_or(o.received_at >= :fromTs) AS in_cur,
                         bool_or(o.received_at < :fromTs) AS in_prev,
                         EXISTS (SELECT 1 FROM orders b WHERE b.customer_id = o.customer_id
                                   AND b.received_at < :fromTs) AS before_cur,
                         EXISTS (SELECT 1 FROM orders b WHERE b.customer_id = o.customer_id
                                   AND b.received_at < :prevFromTs) AS before_prev
                  FROM orders o
                  WHERE o.customer_id IS NOT NULL
                    AND o.received_at >= :prevFromTs AND o.received_at < :toTs
                  GROUP BY o.customer_id
                ) c
                """;

        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("prevFromTs", prevFrom)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new CustomerCohort(toLong(r[0]), toLong(r[1]), toLong(r[2]));
    }

    /** Đơn đăng ký tài khoản tạo trong kỳ, trừ đơn bị từ chối. */
    public long countRegistrations(Instant from, Instant to) {
        Object r = entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM customer_registrations cr
                        WHERE cr.created_at >= :fromTs AND cr.created_at < :toTs AND cr.status <> 'REJECTED'
                        """)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return toLong(r);
    }

    /**
     * Chi tiêu theo khách (FR-021): payment CONFIRMED theo paid_at trong kỳ, bỏ đơn không gắn khách; spent giảm dần
     * rồi mã KH. orderCount = đơn nhận trong kỳ, lastOrderAt = MAX(received_at) trước to; một lateral probe mỗi khách.
     */
    public List<CustomerSpendingRecord> topCustomers(Instant from, Instant to, int limit) {
        String sql = """
                SELECT s.customer_id, c.customer_code, c.full_name, b.order_count, s.spent, b.last_order_at
                FROM (
                  SELECT o.customer_id, SUM(p.amount) AS spent
                  FROM payments p
                  JOIN orders o ON o.id = p.order_id
                  WHERE p.status = 'CONFIRMED' AND p.paid_at >= :fromTs AND p.paid_at < :toTs
                    AND o.customer_id IS NOT NULL
                  GROUP BY o.customer_id
                ) s
                JOIN customers c ON c.id = s.customer_id
                LEFT JOIN LATERAL (
                  SELECT COUNT(*) FILTER (WHERE x.received_at >= :fromTs) AS order_count,
                         MAX(x.received_at) AS last_order_at
                  FROM orders x
                  WHERE x.customer_id = s.customer_id AND x.received_at < :toTs
                ) b ON true
                ORDER BY s.spent DESC, c.customer_code
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .setMaxResults(limit)
                .getResultList();

        List<CustomerSpendingRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new CustomerSpendingRecord(((Number) r[0]).longValue(), (String) r[1], (String) r[2],
                    toLong(r[3]), toMoney(r[4]), r[5] == null ? null : toInstant(r[5])));
        }
        return result;
    }

    /** Lượt dùng voucher (FR-022): đơn nhận trong kỳ có voucher, không hủy; nhóm theo mã snapshot trên đơn. */
    public List<VoucherUsageRecord> voucherUsage(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT o.voucher_code, MAX(o.voucher_id), COUNT(*), COALESCE(SUM(o.discount_amount), 0)
                FROM orders o
                WHERE o.voucher_id IS NOT NULL AND o.status <> 'CANCELLED'
                  AND o.received_at >= :fromTs AND o.received_at < :toTs %s
                GROUP BY o.voucher_code
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<VoucherUsageRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new VoucherUsageRecord((String) r[0], r[1] == null ? null : ((Number) r[1]).longValue(),
                    toLong(r[2]), toMoney(r[3])));
        }
        return result;
    }

    /** Doanh thu đơn có voucher (FR-022): payment CONFIRMED theo paid_at trong kỳ, đơn không hủy, nhóm theo mã. */
    public List<VoucherRevenueRecord> voucherRevenue(Instant from, Instant to, OrderChannel channel) {
        String sql = """
                SELECT o.voucher_code, COALESCE(SUM(p.amount), 0)
                FROM payments p
                JOIN orders o ON o.id = p.order_id
                WHERE p.status = 'CONFIRMED' AND p.paid_at >= :fromTs AND p.paid_at < :toTs
                  AND o.voucher_id IS NOT NULL AND o.status <> 'CANCELLED' %s
                GROUP BY o.voucher_code
                """.formatted(channelClause(channel));

        List<?> rows = withChannel(entityManager.createNativeQuery(sql), channel)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<VoucherRevenueRecord> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new VoucherRevenueRecord((String) r[0], toMoney(r[1])));
        }
        return result;
    }

    /** Sự cố tạo trong kỳ (c+p, theo incidents.created_at). */
    public CountPair countIncidents(Instant prevFrom, Instant from, Instant to) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE i.created_at >= :fromTs),
                       COUNT(*) FILTER (WHERE i.created_at < :fromTs)
                FROM incidents i
                WHERE i.created_at >= :prevFromTs AND i.created_at < :toTs
                """;

        Object[] r = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("prevFromTs", prevFrom)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new CountPair(toLong(r[0]), toLong(r[1]));
    }

    /**
     * Đơn nhận trong kỳ theo nhân viên (FR-023, R5): đơn online tính cho confirmed_by, tại quầy cho created_by.
     * Cùng điều kiện "đơn nhận" với {@link #countOrderSummary}. Sắp orders giảm dần rồi username.
     */
    public List<StaffOrderCount> ordersByStaff(Instant from, Instant to) {
        String sql = """
                SELECT s.username, u.full_name, s.orders
                FROM (
                  SELECT COALESCE(CASE WHEN o.channel = 'ONLINE' THEN o.confirmed_by ELSE o.created_by END, '(không rõ)') AS username,
                         COUNT(*) AS orders
                  FROM orders o
                  WHERE o.received_at >= :fromTs AND o.received_at < :toTs AND o.status NOT IN (:notCounted)
                  GROUP BY 1
                ) s
                LEFT JOIN users u ON u.username = s.username
                ORDER BY s.orders DESC, s.username
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("notCounted", NOT_COUNTED)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<StaffOrderCount> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new StaffOrderCount((String) r[0], (String) r[1], toLong(r[2])));
        }
        return result;
    }

    /** Ca đóng trong kỳ (FR-024): closed_at trong [from,to), CLOSED/APPROVED, sắp closed_at tăng dần. */
    public List<ShiftRow> shiftRows(Instant from, Instant to) {
        String sql = """
                SELECT s.id, s.code, u.full_name, s.closed_at, s.status, s.difference, s.hand_in_cash
                FROM shifts s
                LEFT JOIN users u ON u.id = s.opened_by
                WHERE s.closed_at >= :fromTs AND s.closed_at < :toTs AND s.status IN ('CLOSED','APPROVED')
                ORDER BY s.closed_at, s.id
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        List<ShiftRow> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new ShiftRow(((Number) r[0]).longValue(), (String) r[1], (String) r[2], toInstant(r[3]),
                    (String) r[4], toMoney(r[5]), toMoney(r[6])));
        }
        return result;
    }

    /** Sự cố theo ngày/tháng (giờ VN, theo created_at). */
    public List<OrderPeriodCount> incidentsByPeriod(Instant from, Instant to, Granularity granularity) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT to_char(i.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', :pattern), COUNT(*)
                FROM incidents i
                WHERE i.created_at >= :fromTs AND i.created_at < :toTs
                GROUP BY 1 ORDER BY 1
                """)
                .setParameter("pattern", granularity.getSqlPattern())
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

    public List<LabelCount> incidentsByType(Instant from, Instant to) {
        return incidentsByColumn("type", from, to);
    }

    public List<LabelCount> incidentsByStatus(Instant from, Instant to) {
        return incidentsByColumn("status", from, to);
    }

    // column là hằng nội bộ ("type"/"status"), không nhận từ người dùng
    private List<LabelCount> incidentsByColumn(String column, Instant from, Instant to) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT i.%s, COUNT(*) AS cnt
                FROM incidents i
                WHERE i.created_at >= :fromTs AND i.created_at < :toTs
                GROUP BY 1 ORDER BY cnt DESC, 1
                """.formatted(column))
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();
        List<LabelCount> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new LabelCount((String) r[0], toLong(r[1])));
        }
        return result;
    }

    /** Tiền chi bồi thường theo paid_at trong kỳ: tiền mặt và các phương thức còn lại. */
    public IncidentPayout incidentPayouts(Instant from, Instant to) {
        Object[] r = (Object[]) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(payout_amount) FILTER (WHERE payout_method = 'CASH'), 0),
                       COALESCE(SUM(payout_amount) FILTER (WHERE payout_method <> 'CASH'), 0)
                FROM incidents
                WHERE paid_at >= :fromTs AND paid_at < :toTs
                """)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getSingleResult();
        return new IncidentPayout(toMoney(r[0]), toMoney(r[1]));
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

    /** Như {@link #toMoney} nhưng giữ null (AVG/percentile của tập rỗng). */
    private static BigDecimal toDecimal(Object value) {
        return value == null ? null : toMoney(value);
    }

    private static BigDecimal toMoney(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
