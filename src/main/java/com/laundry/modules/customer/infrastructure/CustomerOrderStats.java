package com.laundry.modules.customer.infrastructure;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thống kê đơn theo khách, đọc bảng orders bằng SQL thuần để module customer không phụ thuộc lớp của module order.
 */
@Repository
@RequiredArgsConstructor
public class CustomerOrderStats {

    /** {@code totalSpent}: tổng tiền các đơn đã thanh toán và chưa hủy. */
    public record Stats(long orderCount, BigDecimal totalSpent, Instant lastOrderAt) {
        public static final Stats EMPTY = new Stats(0, BigDecimal.ZERO, null);
    }

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<Long, Stats> forCustomers(Collection<Long> customerIds) {
        if (customerIds.isEmpty()) return Map.of();
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT o.customer_id,
                               COUNT(*),
                               COALESCE(SUM(o.total_amount) FILTER (WHERE o.payment_status = 'PAID' AND o.status <> 'CANCELLED'), 0),
                               MAX(o.received_at)
                          FROM orders o
                         WHERE o.customer_id IN (:ids)
                         GROUP BY o.customer_id
                        """)
                .setParameter("ids", customerIds)
                .getResultList();
        Map<Long, Stats> result = new HashMap<>();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.put(((Number) r[0]).longValue(), new Stats(((Number) r[1]).longValue(),
                    r[2] instanceof BigDecimal d ? d : new BigDecimal(r[2].toString()), asInstant(r[3])));
        }
        return result;
    }

    /** Số đơn chưa đóng (chưa giao, chưa hủy) của khách: BR-B03 không cho ngừng hoạt động khi còn đơn mở. */
    @Transactional(readOnly = true)
    public long openOrderCount(long customerId) {
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM orders o
                         WHERE o.customer_id = :id AND o.status NOT IN ('DELIVERED', 'CANCELLED')
                        """)
                .setParameter("id", customerId)
                .getSingleResult()).longValue();
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
