package com.laundry.modules.payments.infrastructure;

import com.laundry.modules.payments.application.port.AuditLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class JdbcAuditLogAdapter implements AuditLogPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void record(long actorId, String action, String entityType, long entityId,
                       String oldDataJson, String newDataJson, String reason, String ipAddress) {
        jdbcTemplate.update("""
                        INSERT INTO audit_logs
                            (user_id, action, entity_type, entity_id, old_data, new_data, reason, ip_address, created_at)
                        VALUES (?, ?, ?, ?, ?::json, ?::json, ?, ?, ?)
                        """,
                actorId, action, entityType, entityId, oldDataJson, newDataJson, reason, ipAddress,
                Timestamp.from(Instant.now()));
    }
}
