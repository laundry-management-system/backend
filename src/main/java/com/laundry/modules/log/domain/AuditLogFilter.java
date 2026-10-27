package com.laundry.modules.log.domain;

import java.time.Instant;

public record AuditLogFilter(
        AuditAction action,
        EntityType entityType,
        Long entityId,
        Long userId,
        Instant from,
        Instant to
) {}
