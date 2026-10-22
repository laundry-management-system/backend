package com.laundry.modules.log.domain;

import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecs {

    private AuditLogSpecs() {}

    public static Specification<AuditLog> hasAction(AuditAction action) {
        if(action == null) return null;
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasEntityType(EntityType entityType) {
        if(entityType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        if(entityId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasUserId(Long userId) {
        if(userId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> createdFrom(Instant from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdBefore(Instant to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }
}
