package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.ResourceConflictException;
import com.laundry.modules.payments.application.port.ShiftIntegration;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaShiftIntegrationAdapter implements ShiftIntegration {

    private final EntityManager entityManager;

    @Override
    public long lockOpenShiftId() {
        List<?> rows = entityManager.createNativeQuery("SELECT id FROM shifts WHERE status = 'OPEN' FOR UPDATE")
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResourceConflictException("Chưa mở ca. Mở ca trước khi thu tiền, hủy xác nhận hoặc hoàn tiền");
        }
        return ((Number) rows.get(0)).longValue();
    }
}
