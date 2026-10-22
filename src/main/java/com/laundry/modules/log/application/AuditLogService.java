package com.laundry.modules.log.application;

import com.laundry.common.security.ClientInfo;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditLog;
import com.laundry.modules.log.domain.AuditLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {

    static String SYSTEM_USERNAME = "system";

    AuditLogRepository auditLogRepository;
    UserRepository userRepository;
    ClientInfo clientInfo;

    // Cache id của tài khoản system để khỏi truy vấn lại mỗi lần ghi log
    @NonFinal
    volatile Long systemUserId;

    @Transactional
    public void log(AuditEntry auditEntry) {
        validate(auditEntry);

        AuditLog auditLog = AuditLog.builder()
                .userId(resolveActorId())
                .action(auditEntry.action())
                .entityType(auditEntry.entityType())
                .entityId(auditEntry.entityId())
                .oldData(auditEntry.oldData())
                .newData(auditEntry.newData())
                .reason(auditEntry.reason())
                .ipAddress(clientInfo.currentIp())
                .build();

        auditLogRepository.save(auditLog);
    }

    private void validate(AuditEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("AuditEntry không được null");
        }
        if (entry.action() == null) {
            throw new IllegalArgumentException("Thiếu hành động (action) của nhật ký");
        }
        if (entry.entityType() == null) {
            throw new IllegalArgumentException("Thiếu kiểu dữ liệu của đối tượng (entityType)");
        }
        if (entry.entityId() == null) {
            throw new IllegalArgumentException("Thiếu id của đối tượng (entityId)");
        }
    }

    /**
     * Có xác thực: lấy id người dùng; không tìm thấy thì báo lỗi, KHÔNG ghi tạm dưới tên system.
     * Không có xác thực (public, webhook, hệ thống tự chạy): dùng tài khoản system.
     */
    private Long resolveActorId() {
        return clientInfo.currentUsername()
                .map(username -> userRepository.findIdByUsername(username)
                        .orElseThrow(() -> new IllegalStateException(
                                "Người dùng đã xác thực nhưng không có trong DB: " + username)))
                .orElseGet(this::getSystemUserId);
    }

    private Long getSystemUserId() {
        Long id = systemUserId;
        if (id == null) {
            id = userRepository.findIdByUsername(SYSTEM_USERNAME)
                    .orElseThrow(() -> new IllegalStateException(
                            "Chưa có tài khoản hệ thống '" + SYSTEM_USERNAME + "' trong DB"));
            systemUserId = id;
        }
        return id;
    }
}