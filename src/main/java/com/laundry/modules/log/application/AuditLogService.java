package com.laundry.modules.log.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ClientInfo;
import com.laundry.common.security.JwtUtil;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.application.dto.AuditLogDetailResponse;
import com.laundry.modules.log.application.dto.AuditLogSummaryResponse;
import com.laundry.modules.log.domain.AuditLog;
import com.laundry.modules.log.domain.AuditLogFilter;
import com.laundry.modules.log.domain.AuditLogRepository;
import com.laundry.modules.log.domain.AuditLogSpecs;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    /** Ghi nhật ký thao tác của khách: bảng nhật ký chỉ trỏ được tới nhân viên nên lưu kèm mã khách. */
    @Transactional
    public void logAsCustomer(AuditEntry entry, String customerCode) {
        Map<String, Object> newData = new HashMap<>();
        if (entry.newData() != null) newData.putAll(entry.newData());
        newData.put("actorCustomerCode", customerCode);
        log(new AuditEntry(entry.action(), entry.entityType(), entry.entityId(), entry.oldData(), newData, entry.reason()));
    }

    private Long resolveActorId() {
        return clientInfo.currentUsername()
                // Phiên khách không có dòng trong bảng users: ghi dưới tài khoản system
                .filter(username -> !JwtUtil.isCustomerSubject(username))
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

        Page<AuditLog> result = auditLogRepository.findAll(toSpecification(filter), pageable);
        Map<Long, User> users = usersOf(result.getContent());

        return result.map(a -> {
            User user = users.get(a.getUserId());
            return AuditLogSummaryResponse.from(a, user.getFullName(), user.getUsername());
        });
    }

    /** Toàn bộ log khớp bộ lọc, mới nhất trước, không phân trang. Dùng cho các màn đọc nhật ký theo nghiệp vụ (lịch sử giá). */
    @Transactional(readOnly = true)
    public List<AuditLogDetailResponse> listAll(AuditLogFilter filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        List<AuditLog> logs = auditLogRepository.findAll(toSpecification(filter), sort);
        Map<Long, User> users = usersOf(logs);

        return logs.stream().map(a -> {
            User user = users.get(a.getUserId());
            return AuditLogDetailResponse.from(a, user.getFullName(), user.getUsername());
        }).toList();
    }

    private Specification<AuditLog> toSpecification(AuditLogFilter filter) {
        // Spec trả null khi bộ lọc trống; Spring Data 4 không nhận null trong and() nên lọc bỏ trước khi ghép
        return Specification.allOf(Stream.of(
                        AuditLogSpecs.hasAction(filter.action()),
                        AuditLogSpecs.hasEntityType(filter.entityType()),
                        AuditLogSpecs.hasEntityId(filter.entityId()),
                        AuditLogSpecs.hasUserId(filter.userId()),
                        AuditLogSpecs.createdFrom(filter.from()),
                        AuditLogSpecs.createdBefore(filter.to()))
                .filter(Objects::nonNull)
                .toList());
    }

    private Map<Long, User> usersOf(List<AuditLog> logs) {
        Set<Long> ids = logs.stream().map(AuditLog::getUserId).collect(Collectors.toSet());
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, user -> user));
    }
}
