package com.laundry.modules.log.application.dto;

import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.AuditLog;
import com.laundry.modules.log.domain.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response chứa thông tin tóm gọn của một Audit Log")
public record AuditLogSummaryResponse(
        @Schema(description = "Mã khóa duy nhất của log", example = "123")
        Long id,

        @Schema(description = "Hành động của user", example = "ACCOUNT_MANAGE")
        AuditAction action,

        @Schema(description = "Kiểu dữ liệu của đối tượng bị tác động", example = "User")
        EntityType entityType,

        @Schema(description = "Mã khóa duy nhất của đối tượng bị tác động", example = "123")
        Long entityId,

        @Schema(description = "Mã khóa duy nhất của người tác động (user)", example = "123")
        Long userId,

        @Schema(description = "Họ và tên đầy đủ của user", example = "Nguyễn Văn A")
        String fullName,

        @Schema(description = "Tên tài khoản của user", example = "anv")
        String username,

        @Schema(description = "Địa chỉ ip của user khi thực hiện hành động", example = "x.x.x.x")
        String ipAddress,

        @Schema(description = "Thời gian ghi nhật hành động", example = "2026-10-22T04:26:30")
        Instant createdAt
) {
    public static AuditLogSummaryResponse from(AuditLog auditLog, String fullName, String username){
        return new AuditLogSummaryResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getUserId(),
                fullName,
                username,
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }
}
