package com.laundry.modules.shift.application.port;

public interface AuditLogPort {

    void record(long actorId, String action, String entityType, long entityId,
                String oldDataJson, String newDataJson, String reason, String ipAddress);
}
