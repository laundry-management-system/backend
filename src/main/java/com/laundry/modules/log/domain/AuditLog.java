package com.laundry.modules.log.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_entity_type", columnList = "entityType"),
                @Index(name = "idx_audit_logs_entity_id", columnList = "entityId"),
                @Index(name = "idx_audit_logs_action", columnList = "action"),
                @Index(name = "idx_audit_logs_created_at", columnList = "createdAt")
        }
)
public class AuditLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "action", nullable = false, length = 40)
    String action;

    @Column(name = "entity_type", nullable = false, length = 30)
    String entityType;

    @Column(name = "entity_id", nullable = false)
    Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "jsonb", updatable = false)
    Map<String, Object> oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb", updatable = false)
    Map<String, Object> newData;

    @Column(name = "reason", length = 500)
    String reason;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

}
