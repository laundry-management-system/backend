package com.laundry.modules.log.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ClientInfo;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.application.dto.AuditLogDetailResponse;
import com.laundry.modules.log.application.dto.AuditLogSummaryResponse;
import com.laundry.modules.log.domain.AuditLog;
import com.laundry.modules.log.domain.AuditLogFilter;
import com.laundry.modules.log.domain.AuditLogRepository;
import com.laundry.modules.log.domain.AuditLogSpecs;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        if (entry == null) throw new IllegalArgumentException("AuditEntry không được null");
        if (entry.action() == null) throw new IllegalArgumentException("Thiếu hành động (action) của nhật ký");
        if (entry.entityType() == null) throw new IllegalArgumentException("Thiếu kiểu dữ liệu của đối tượng (entityType)");
        if (entry.entityId() == null) throw new IllegalArgumentException("Thiếu id của đối tượng (entityId)");

        // Lý do đến từ người dùng nên dùng BusinessException (400) thay vì IAE (500)
        String reason = entry.reason();
        if (entry.action().isReasonRequired() && (reason == null || reason.isBlank())) {
            throw new BusinessException("Hành động này bắt buộc phải nhập lý do");
        }
        if (reason != null && reason.length() > 500) {
            throw new BusinessException("Lý do không được vượt quá 500 ký tự");
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

    @Transactional(readOnly = true)
    public AuditLogDetailResponse getDetail(Long id){
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin log"));
        User user = userRepository.findById(auditLog.getUserId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin người dùng"));
        return AuditLogDetailResponse.from(auditLog, user.getFullName(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogSummaryResponse> getSummary(AuditLogFilter filter, int page, int size, String direction){
        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(dir, "createdAt").and(Sort.by(dir, "id"));

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, 100),
                sort);

        Specification<AuditLog> specs = Specification
                .where(AuditLogSpecs.hasAction(filter.action()))
                .and(AuditLogSpecs.hasEntityType(filter.entityType()))
                .and(AuditLogSpecs.hasEntityId(filter.entityId()))
                .and(AuditLogSpecs.hasUserId(filter.userId()))
                .and(AuditLogSpecs.createdFrom(filter.from()))
                .and(AuditLogSpecs.createdBefore(filter.to()));

        Page<AuditLog> result = auditLogRepository.findAll(specs, pageable);
        Set<Long> ids = result.getContent()
                .stream()
                .map(AuditLog::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> users = userRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return result.map(a -> {
            User user = users.get(a.getUserId());
            return AuditLogSummaryResponse.from(a, user.getFullName(), user.getUsername());
        });
    }
}