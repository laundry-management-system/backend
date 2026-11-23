package com.laundry.modules.setting.presentation;

import com.laundry.modules.setting.application.ServiceAreaService;
import com.laundry.modules.setting.application.dto.ServiceAreaRequest;
import com.laundry.modules.setting.application.dto.ServiceAreaResponse;
import com.laundry.modules.setting.application.dto.ServiceAreaStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Không có DELETE: khu vực chỉ ngừng phục vụ (FR-029d)
@Tag(name = "Service areas", description = "Khu vực và phí giao nhận (Admin)")
@RestController
@RequestMapping("/api/service-areas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ServiceAreaController {

    private final ServiceAreaService serviceAreaService;

    @Operation(summary = "Mọi khu vực, cả đang và ngừng phục vụ, theo thứ tự thêm")
    @GetMapping
    public List<ServiceAreaResponse> list() {
        return serviceAreaService.list();
    }

    @Operation(summary = "Thêm khu vực (đang phục vụ)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceAreaResponse create(@Valid @RequestBody ServiceAreaRequest request) {
        return serviceAreaService.create(request);
    }

    @Operation(summary = "Sửa tên, phí lượt lấy, phí lượt giao")
    @PutMapping("/{id}")
    public ServiceAreaResponse update(@PathVariable Long id, @Valid @RequestBody ServiceAreaRequest request) {
        return serviceAreaService.update(id, request);
    }

    @Operation(summary = "Ngừng hoặc phục vụ lại khu vực")
    @PatchMapping("/{id}/status")
    public ServiceAreaResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ServiceAreaStatusRequest request) {
        return serviceAreaService.changeActive(id, request.active());
    }
}
