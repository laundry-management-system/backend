package com.laundry.modules.shift.domain;

import com.laundry.modules.shift.application.dto.ShiftTransactionResponse;
import com.laundry.modules.shift.domain.ShiftMath.ShiftTotals;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Tổng tiền của ca, đọc thẳng bảng payments (research R2).
 * Thu tính theo shift_id (mọi trạng thái, vì lúc thu tiền đã vào két); hủy / hoàn tính theo resolved_shift_id.
 */
@Repository
@RequiredArgsConstructor
public class ShiftTotalsQuery {

    private final EntityManager entityManager;

    public ShiftTotals totals(long shiftId) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT COALESCE(SUM(amount) FILTER (WHERE shift_id = :id AND method = 'CASH'), 0),
                               COUNT(*) FILTER (WHERE shift_id = :id AND method = 'CASH'),
                               COALESCE(SUM(amount) FILTER (WHERE resolved_shift_id = :id AND method = 'CASH'), 0),
                               COUNT(*) FILTER (WHERE resolved_shift_id = :id AND method = 'CASH'),
                               COALESCE(SUM(amount) FILTER (WHERE shift_id = :id AND method <> 'CASH'), 0)
                                 - COALESCE(SUM(amount) FILTER (WHERE resolved_shift_id = :id AND method <> 'CASH'), 0),
                               COUNT(*) FILTER (WHERE shift_id = :id AND method <> 'CASH')
                        FROM payments
                        WHERE shift_id = :id OR resolved_shift_id = :id
                        """)
                .setParameter("id", shiftId)
                .getSingleResult();
        return new ShiftTotals(money(row[0]), count(row[1]), money(row[2]), count(row[3]),
                money(row[4]), count(row[5]));
    }

    /** Giao dịch trong ca, mới nhất trước: dòng thu theo shift_id, dòng hủy / hoàn theo resolved_shift_id. */
    public Page<ShiftTransactionResponse> transactions(long shiftId, Pageable pageable) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT t.at, t.order_id, o.order_code, t.type, t.method, t.amount, u.full_name
                          FROM (SELECT p.paid_at AS at, p.order_id, 'COLLECT' AS type, p.method, p.amount,
                                       p.confirmed_by AS actor
                                  FROM payments p WHERE p.shift_id = :id
                                UNION ALL
                                SELECT p.voided_at, p.order_id,
                                       CASE p.status WHEN 'REFUNDED' THEN 'REFUND' ELSE 'VOID' END,
                                       p.method, p.amount, p.voided_by
                                  FROM payments p WHERE p.resolved_shift_id = :id) t
                          JOIN orders o ON o.id = t.order_id
                          LEFT JOIN users u ON u.id = t.actor
                         ORDER BY t.at DESC
                         LIMIT :limit OFFSET :offset
                        """)
                .setParameter("id", shiftId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();
        long total = ((Number) entityManager.createNativeQuery("""
                        SELECT (SELECT COUNT(*) FROM payments WHERE shift_id = :id)
                             + (SELECT COUNT(*) FROM payments WHERE resolved_shift_id = :id)
                        """)
                .setParameter("id", shiftId)
                .getSingleResult()).longValue();
        List<ShiftTransactionResponse> content = rows.stream().map(r -> {
            Object[] c = (Object[]) r;
            return new ShiftTransactionResponse(toInstant(c[0]), ((Number) c[1]).longValue(), (String) c[2],
                    (String) c[3], (String) c[4], money(c[5]), (String) c[6]);
        }).toList();
        return new PageImpl<>(content, pageable, total);
    }

    private static Instant toInstant(Object v) {
        if (v instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return v instanceof OffsetDateTime odt ? odt.toInstant() : (Instant) v;
    }

    private static BigDecimal money(Object v) {
        return new BigDecimal(v.toString());
    }

    private static int count(Object v) {
        return ((Number) v).intValue();
    }
}
