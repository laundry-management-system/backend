package com.laundry.modules.incident.presentation;

import com.laundry.modules.incident.application.IncidentService;
import com.laundry.modules.incident.application.dto.ApproveIncidentRequest;
import com.laundry.modules.incident.application.dto.CreateIncidentRequest;
import com.laundry.modules.incident.application.dto.IncidentDetailResponse;
import com.laundry.modules.incident.application.dto.IncidentStatsResponse;
import com.laundry.modules.incident.application.dto.IncidentSummaryResponse;
import com.laundry.modules.incident.application.dto.PayoutIncidentRequest;
import com.laundry.modules.incident.application.dto.RejectIncidentRequest;
import com.laundry.modules.incident.application.dto.UpdateIncidentRequest;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Incidents", description = "Phiếu sự cố & bồi thường (UC15, UC24, BM9, QĐ11)")
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'EMPLOYEE')")
public class IncidentController {

    private final IncidentService incidentService;

    @Operation(summary = "Danh sách phiếu sự cố (lọc trạng thái, từ khóa mã sự cố / mã đơn / SĐT, loại, ngày lập, đơn)")
    @GetMapping
    public Page<IncidentSummaryResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) Long orderId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return incidentService.list(status, q, type, createdFrom, createdTo, orderId, pageable);
    }

    @Operation(summary = "Số phiếu theo trạng thái và tổng đã chi bồi thường, cùng bộ lọc với danh sách")
    @GetMapping("/stats")
    public IncidentStatsResponse stats(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo
    ) {
        return incidentService.stats(q, type, createdFrom, createdTo);
    }

    @Operation(summary = "Chi tiết phiếu sự cố kèm ảnh ban đầu để đối chiếu")
    @GetMapping("/{id}")
    public IncidentDetailResponse get(@PathVariable Long id) {
        return incidentService.get(id);
    }

    @Operation(summary = "Lập phiếu sự cố (UC15)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDetailResponse create(@Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        return incidentService.create(request, authentication.getName());
    }

    @Operation(summary = "Sửa phiếu đang chờ duyệt")
    @PutMapping("/{id}")
    public IncidentDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateIncidentRequest request) {
        return incidentService.update(id, request);
    }

    @Operation(summary = "Duyệt phiếu sự cố (UC24 - Admin)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentDetailResponse approve(@PathVariable Long id, @Valid @RequestBody ApproveIncidentRequest request,
                                          Authentication authentication) {
        return incidentService.approve(id, request, authentication.getName());
    }

    @Operation(summary = "Từ chối phiếu sự cố (UC24 - Admin)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentDetailResponse reject(@PathVariable Long id, @Valid @RequestBody RejectIncidentRequest request,
                                         Authentication authentication) {
        return incidentService.reject(id, request, authentication.getName());
    }

    @Operation(summary = "Ghi nhận chi bồi thường, gắn vào ca đang mở (Admin)")
    @PostMapping("/{id}/payout")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentDetailResponse payout(@PathVariable Long id, @Valid @RequestBody PayoutIncidentRequest request,
                                         Authentication authentication) {
        return incidentService.payout(id, request, authentication.getName());
    }
}
