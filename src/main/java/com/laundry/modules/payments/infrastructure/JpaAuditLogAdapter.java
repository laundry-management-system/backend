package com.laundry.modules.payments.infrastructure;

import com.laundry.modules.payments.application.port.AuditLogPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaAuditLogAdapter implements AuditLogPort {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void record(long actorId, String action, String entityType, long entityId,
                       String oldDataJson, String newDataJson, String reason, String ipAddress) {
        entityManager.createNativeQuery("""
                        INSERT INTO audit_logs
                            (user_id, action, entity_type, entity_id, old_data, new_data,
                             reason, ip_address, created_at)
                        VALUES
                            (:actorId, :action, :entityType, :entityId,
                             CAST(:oldDataJson AS json), CAST(:newDataJson AS json),
                             :reason, :ipAddress, CURRENT_TIMESTAMP)
                        """)
                .setParameter("actorId", actorId)
                .setParameter("action", action)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .setParameter("oldDataJson", oldDataJson)
                .setParameter("newDataJson", newDataJson)
                .setParameter("reason", reason)
                .setParameter("ipAddress", ipAddress)
                .executeUpdate();
    }
}
