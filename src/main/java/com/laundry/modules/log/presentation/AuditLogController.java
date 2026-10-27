package com.laundry.modules.log.presentation;

import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditLogDetailResponse;
import com.laundry.modules.log.application.dto.AuditLogSummaryResponse;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.AuditLogFilter;
import com.laundry.modules.log.domain.EntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Log Management", description = "API xem nhật ký hệ thống của admin")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/log")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    AuditLogService auditLogService;

    @Operation(summary = "Lấy thông tin chi tiết của môt log", description = "API lấy thông tin chi tiết của một log bằng id")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuditLogDetailResponse getAuditLogDetail(@PathVariable Long id) {
        return auditLogService.getDetail(id);
    }

    @Operation(
            summary = "Lấy danh sách log theo filter",
            description = "API lấy danh sách log theo filter (action, entityType, entityId, userId, from, to), số trang (page), kích thước trang(size), chiều của danh sách (direction = asc/desc)"
    )
    @GetMapping
    public Page<AuditLogSummaryResponse> getAuditLogSummary(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction){
        AuditLogFilter filter = new AuditLogFilter(
                action,
                entityType,
                entityId,
                userId,
                from,
                to
        );

        return auditLogService.getSummary(filter, page, size, direction);
    }
}
