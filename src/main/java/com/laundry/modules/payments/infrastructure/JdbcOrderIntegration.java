package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter to the orders/customer tables described in the project database diagram.
 * Keeping this SQL behind a port lets the orders module replace it with its application facade later.
 */
@Repository
@RequiredArgsConstructor
public class JdbcOrderIntegration implements OrderIntegration {

    private static final String ORDER_SELECT = """
            SELECT o.id, o.order_code, o.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
                   o.received_at, o.due_at, o.total_amount, o.status, o.payment_status, o.completed_at,
                   o.shelf_location, o.delivered_at, o.delivered_by, o.receiver_name, o.receiver_relation,
                   o.last_notified_at, o.notify_count, o.note
              FROM orders o
              JOIN customers c ON c.id = o.customer_id
            """;

    private static final RowMapper<OrderSnapshot> ORDER_ROW_MAPPER = JdbcOrderIntegration::mapOrder;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<OrderSnapshot> findByCode(String orderCode) {
        return jdbcTemplate.query(ORDER_SELECT + " WHERE o.order_code = ?", ORDER_ROW_MAPPER, orderCode)
                .stream().findFirst();
    }

    @Override
    public OrderSnapshot lockByCode(String orderCode) {
        return jdbcTemplate.query(ORDER_SELECT + " WHERE o.order_code = ? FOR UPDATE OF o",
                        ORDER_ROW_MAPPER, orderCode)
                .stream().findFirst()
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
    }

    @Override
    public void setPaymentStatus(long orderId, boolean paid) {
        int changed = jdbcTemplate.update(
                "UPDATE orders SET payment_status = ? WHERE id = ? AND status <> 'CANCELLED'",
                paid ? "PAID" : "UNPAID", orderId);
        if (changed == 0) {
            throw new BusinessException("Không thể cập nhật thanh toán cho đơn đã hủy");
        }
    }

    @Override
    public void deliver(long orderId, Instant deliveredAt, long deliveredBy, String receiverName,
                        String receiverRelation) {
        int changed = jdbcTemplate.update("""
                        UPDATE orders
                           SET status = 'DELIVERED', delivered_at = ?, delivered_by = ?,
                               receiver_name = ?, receiver_relation = ?
                         WHERE id = ? AND status = 'COMPLETED' AND payment_status = 'PAID'
                        """,
                Timestamp.from(deliveredAt), deliveredBy, receiverName, receiverRelation, orderId);
        if (changed == 0) {
            throw new BusinessException("Chỉ giao được đơn Hoàn thành đã thanh toán");
        }
    }

    @Override
    public void recordReminder(long orderId, Instant remindedAt) {
        int changed = jdbcTemplate.update("""
                        UPDATE orders
                           SET last_notified_at = ?, notify_count = COALESCE(notify_count, 0) + 1
                         WHERE id = ? AND status = 'COMPLETED'
                        """,
                Timestamp.from(remindedAt), orderId);
        if (changed == 0) {
            throw new BusinessException("Chỉ có thể nhắn khách về đơn đã hoàn thành");
        }
    }

    @Override
    public void appendNote(long orderId, String note) {
        int changed = jdbcTemplate.update("""
                        UPDATE orders
                           SET note = CASE WHEN note IS NULL OR note = '' THEN ? ELSE note || E'\\n' || ? END
                         WHERE id = ?
                        """,
                note, note, orderId);
        if (changed == 0) {
            throw NotFoundException.of("Đơn hàng", orderId);
        }
    }

    @Override
    public List<OrderSnapshot> findOverdue(Instant completedBefore) {
        return jdbcTemplate.query(ORDER_SELECT + " WHERE o.status = 'COMPLETED' AND o.completed_at < ?"
                        + " ORDER BY o.completed_at ASC LIMIT 200",
                ORDER_ROW_MAPPER, Timestamp.from(completedBefore));
    }

    private static OrderSnapshot mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new OrderSnapshot(
                rs.getLong("id"),
                rs.getString("order_code"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                instant(rs, "received_at"),
                instant(rs, "due_at"),
                rs.getBigDecimal("total_amount"),
                rs.getString("status"),
                rs.getString("payment_status"),
                instant(rs, "completed_at"),
                rs.getString("shelf_location"),
                instant(rs, "delivered_at"),
                rs.getObject("delivered_by", Long.class),
                rs.getString("receiver_name"),
                rs.getString("receiver_relation"),
                instant(rs, "last_notified_at"),
                rs.getInt("notify_count"),
                rs.getString("note"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
