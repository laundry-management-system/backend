package com.laundry.modules.order.domain;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Đọc mã phiếu sự cố đã trừ vào đơn bằng SQL thuần để module đơn không phụ thuộc module sự cố. */
@Repository
@RequiredArgsConstructor
public class OrderIncidentQuery {

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<String> deductedCodes(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return entityManager.createNativeQuery("""
                        SELECT code FROM incidents
                         WHERE order_id = :id AND status = 'APPROVED' AND deducted_amount > 0
                         ORDER BY created_at
                        """)
                .setParameter("id", orderId)
                .getResultList();
    }
}
