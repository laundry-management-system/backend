package com.laundry.modules.incident.presentation;

import com.laundry.modules.incident.application.IncidentService;
import com.laundry.modules.incident.application.dto.CreateIncidentRequest;
import com.laundry.modules.incident.application.dto.IncidentDecisionRequest;
import com.laundry.modules.incident.application.dto.IncidentResponse;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Incident", description = "Phiếu sự cố và bồi thường (UC15, UC24 / BM9 / QĐ11)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @Operation(summary = "Lập phiếu sự cố (NV). Phiếu chờ Admin duyệt")
    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(@Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        return incidentService.create(request, authentication.getName());
    }

    @Operation(summary = "Danh sách phiếu sự cố, lọc theo trạng thái, loại, thời gian lập")
    @GetMapping("/incidents")
    public Page<IncidentResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return incidentService.list(status, type, from, to, pageable);
    }

    @Operation(summary = "Chi tiết phiếu sự cố, kèm ảnh lúc nhận đồ để đối chiếu")
    @GetMapping("/incidents/{id}")
    public IncidentResponse get(@PathVariable Long id) {
        return incidentService.get(id);
    }

    @Operation(summary = "Các phiếu sự cố của một đơn (thẻ Sự cố ở chi tiết đơn)")
    @GetMapping("/orders/{orderId}/incidents")
    public List<IncidentResponse> listByOrder(@PathVariable Long orderId) {
        return incidentService.listByOrder(orderId);
    }

    @Operation(summary = "Duyệt phiếu và thực hiện phương án (Admin)")
    @PostMapping("/incidents/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse approve(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) IncidentDecisionRequest request,
                                    Authentication authentication) {
        return incidentService.approve(id, request == null ? null : request.note(), authentication.getName());
    }

    @Operation(summary = "Từ chối phiếu, bắt buộc lý do (Admin)")
    @PostMapping("/incidents/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse reject(@PathVariable Long id,
                                   @Valid @RequestBody IncidentDecisionRequest request,
                                   Authentication authentication) {
        return incidentService.reject(id, request.note(), authentication.getName());
    }
}
