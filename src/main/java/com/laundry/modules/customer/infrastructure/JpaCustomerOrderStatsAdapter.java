package com.laundry.modules.customer.infrastructure;

import com.laundry.modules.customer.application.port.CustomerOrderStats;
import com.laundry.modules.customer.application.port.CustomerOrderStatsProvider;
import jakarta.persistence.EntityManager;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Đọc bảng orders qua hợp đồng DB chung, không import entity của module order. */
@Repository
@RequiredArgsConstructor
public class JpaCustomerOrderStatsAdapter implements CustomerOrderStatsProvider {

    private static final String STATS_SQL = """
            SELECT o.customer_id,
                   COUNT(*),
                   COALESCE(SUM(o.total_amount) FILTER (
                       WHERE o.payment_status = 'PAID' AND o.status <> 'CANCELLED'), 0),
                   MAX(o.received_at)
              FROM orders o
             WHERE o.customer_id IN (:ids)
             GROUP BY o.customer_id
            """;

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CustomerOrderStats> statsFor(Collection<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(STATS_SQL)
                .setParameter("ids", customerIds)
                .getResultList();
        Map<Long, CustomerOrderStats> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), new CustomerOrderStats(
                    ((Number) row[1]).longValue(),
                    new BigDecimal(row[2].toString()),
                    asInstant(row[3])));
        }
        return result;
    }

    private static Instant asInstant(Object value) {
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
        return null;
    }
}
