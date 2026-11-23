package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import com.laundry.modules.payments.application.port.OverdueSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA boundary adapter for the order module's shared database contract. */
@Repository
@RequiredArgsConstructor
public class JpaOrderIntegrationAdapter implements OrderIntegration {

    private static final String ORDER_SELECT = """
            SELECT o.id, o.order_code, o.customer_id, o.customer_name, BTRIM(o.customer_phone),
                   o.received_at, o.due_at,
                   o.total_amount + COALESCE((SELECT f.pickup_fee + f.delivery_fee FROM order_fulfillments f WHERE f.order_id = o.id), 0),
                   o.status, o.payment_status,
                   o.completed_at, o.shelf_location, o.delivered_at, o.delivered_by,
                   o.receiver_name, o.receiver_relation, o.last_notified_at,
                   o.notify_count, o.note
              FROM orders o
            """;

    // Đơn online chỉ trả đồ khi chiều trả ở bước cuối: DELIVERING (giao tận nơi) / AWAITING_CUSTOMER_COLLECT (tại tiệm)
    private static final String RETURN_NOT_READY = """
            SELECT 1 FROM order_fulfillments f
             WHERE f.order_id = orders.id
               AND f.return_status IS DISTINCT FROM
                   CASE f.return_method WHEN 'DELIVERY' THEN 'DELIVERING' ELSE 'AWAITING_CUSTOMER_COLLECT' END
            """;

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderSnapshot> findByCode(String orderCode) {
        Query query = entityManager.createNativeQuery(ORDER_SELECT + " WHERE o.order_code = :orderCode");
        query.setParameter("orderCode", orderCode);
        return query.getResultList().stream().findFirst().map(row -> map((Object[]) row));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOnlineDelivery(Long orderId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS (SELECT 1 FROM orders o
                                         JOIN order_fulfillments f ON f.order_id = o.id
                                        WHERE o.id = :orderId AND o.channel = 'ONLINE'
                                          AND f.return_method = 'DELIVERY')
                        """)
                .setParameter("orderId", orderId)
                .getSingleResult());
    }

    @Override
    @Transactional
    public OrderSnapshot lockByCode(String orderCode) {
        Query query = entityManager.createNativeQuery(
                ORDER_SELECT + " WHERE o.order_code = :orderCode FOR UPDATE OF o");
        query.setParameter("orderCode", orderCode);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) {
            throw NotFoundException.of("Order", orderCode);
        }
        return map((Object[]) rows.get(0));
    }

    @Override
    @Transactional
    public void setPaymentStatus(long orderId, boolean paid) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE orders
                           SET payment_status = :paymentStatus, updated_at = CURRENT_TIMESTAMP
                         WHERE id = :orderId AND status NOT IN ('CANCELLED','PENDING_CONFIRMATION','AWAITING_DROPOFF')
                        """)
                .setParameter("paymentStatus", paid ? "PAID" : "UNPAID")
                .setParameter("orderId", orderId)
                .executeUpdate();
        if (updated == 0) {
            throw new BusinessException("Cannot update payment status for a cancelled or missing order");
        }
    }

    @Override
    @Transactional
    public void deliver(long orderId, Instant deliveredAt, String deliveredBy, String receiverName,
                        String receiverRelation) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE orders
                           SET status = 'DELIVERED', delivered_at = :deliveredAt, delivered_by = :deliveredBy,
                               receiver_name = :receiverName, receiver_relation = :receiverRelation,
                               updated_at = CURRENT_TIMESTAMP
                         WHERE id = :orderId AND status = 'COMPLETED' AND payment_status = 'PAID'
                           AND NOT EXISTS (""" + RETURN_NOT_READY + """
                               )
                        """)
                .setParameter("deliveredAt", Timestamp.from(deliveredAt))
                .setParameter("deliveredBy", deliveredBy)
                .setParameter("receiverName", receiverName)
                .setParameter("receiverRelation", receiverRelation)
                .setParameter("orderId", orderId)
                .executeUpdate();
        if (updated == 0) {
            boolean returnNotReady = !entityManager.createNativeQuery(
                            RETURN_NOT_READY.replace("orders.id", ":orderId"))
                    .setParameter("orderId", orderId)
                    .getResultList().isEmpty();
            throw new BusinessException(returnNotReady ? "Đơn giao tận nơi phải chuyển sang Đang giao trước"
                    : "Only completed, paid orders can be delivered");
        }
        // Đơn online: bước cuối chiều trả (đơn tại quầy không có dòng fulfillment nên không tác dụng)
        entityManager.createNativeQuery("""
                        INSERT INTO order_status_history
                               (order_id, action_type, old_value, new_value, performed_by, created_at, updated_at)
                        SELECT order_id, 'RETURN_STATUS_CHANGE', return_status,
                               CASE return_method WHEN 'DELIVERY' THEN 'DELIVERED' ELSE 'CUSTOMER_COLLECTED' END,
                               :performedBy, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                          FROM order_fulfillments
                         WHERE order_id = :orderId
                        """)
                .setParameter("performedBy", deliveredBy != null ? deliveredBy : "SYSTEM")
                .setParameter("orderId", orderId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        UPDATE order_fulfillments
                           SET return_status = CASE return_method WHEN 'DELIVERY' THEN 'DELIVERED' ELSE 'CUSTOMER_COLLECTED' END,
                               updated_at = CURRENT_TIMESTAMP
                         WHERE order_id = :orderId
                        """)
                .setParameter("orderId", orderId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void recordReminder(long orderId, Instant remindedAt) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE orders
                           SET last_notified_at = :remindedAt,
                               notify_count = COALESCE(notify_count, 0) + 1,
                               updated_at = CURRENT_TIMESTAMP
                         WHERE id = :orderId AND status = 'COMPLETED'
                        """)
                .setParameter("remindedAt", Timestamp.from(remindedAt))
                .setParameter("orderId", orderId)
                .executeUpdate();
        if (updated == 0) {
            throw new BusinessException("Only completed orders can be reminded");
        }
    }

    @Override
    @Transactional
    public void appendNote(long orderId, String note) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE orders
                           SET note = CASE
                                   WHEN note IS NULL OR BTRIM(note) = '' THEN :note
                                   ELSE note || CHR(10) || :note
                               END,
                               updated_at = CURRENT_TIMESTAMP
                         WHERE id = :orderId
                        """)
                .setParameter("note", note)
                .setParameter("orderId", orderId)
                .executeUpdate();
        if (updated == 0) {
            throw NotFoundException.of("Order", orderId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverdueSnapshot> findOverdue(Instant completedBefore) {
        // Wraps ORDER_SELECT so map() keeps its column indexes; VIP flag is appended as the last column
        Query query = entityManager.createNativeQuery("SELECT x.*, COALESCE(c.is_vip, false) FROM ("
                + ORDER_SELECT + """
                 WHERE o.status = 'COMPLETED' AND o.completed_at < :completedBefore
                ) x
                LEFT JOIN customers c ON c.id = x.customer_id
                ORDER BY x.completed_at ASC
                """);
        query.setParameter("completedBefore", Timestamp.from(completedBefore));
        query.setMaxResults(200);
        return ((List<?>) query.getResultList()).stream()
                .map(row -> (Object[]) row)
                .map(row -> new OverdueSnapshot(map(row), Boolean.TRUE.equals(row[row.length - 1])))
                .toList();
    }

    private static OrderSnapshot map(Object[] row) {
        return new OrderSnapshot(
                asLong(row[0]), (String) row[1], asLong(row[2]), (String) row[3], (String) row[4],
                asInstant(row[5]), asInstant(row[6]), asBigDecimal(row[7]), (String) row[8], (String) row[9],
                asInstant(row[10]), (String) row[11], asInstant(row[12]), (String) row[13],
                (String) row[14], (String) row[15], asInstant(row[16]),
                row[17] == null ? 0 : ((Number) row[17]).intValue(), (String) row[18]);
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null || value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private static Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported database timestamp type: " + value.getClass());
    }
}
