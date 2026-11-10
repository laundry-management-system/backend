package com.laundry.modules.order.infrastructure;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Đọc giao dịch thanh toán đang hiệu lực (CONFIRMED) của đơn qua bảng payments.
 * Dùng SQL thuần để module order không phụ thuộc lớp của module payments (giống JpaOrderIntegrationAdapter chiều ngược lại).
 */
@Repository
@RequiredArgsConstructor
public class OrderPaymentLookup {

    public record ConfirmedPayment(String method, Instant paidAt) {
    }

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<ConfirmedPayment> findConfirmed(long orderId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT p.method, p.paid_at
                          FROM payments p
                         WHERE p.order_id = :orderId AND p.status = 'CONFIRMED'
                         ORDER BY p.created_at DESC
                         LIMIT 1
                        """)
                .setParameter("orderId", orderId)
                .getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(new ConfirmedPayment((String) row[0], asInstant(row[1])));
    }

    private static Instant asInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case Timestamp timestamp -> timestamp.toInstant();
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case LocalDateTime dateTime -> dateTime.toInstant(ZoneOffset.UTC);
            default -> throw new IllegalArgumentException("Unsupported timestamp type: " + value.getClass());
        };
    }
}
