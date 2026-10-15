package com.laundry.modules.service.presentation;

import com.laundry.modules.service.application.LaundryServiceService;
import com.laundry.modules.service.application.dto.ChangeServiceStatusRequest;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Service & Pricing Management", description = "Quản lý dịch vụ và bảng giá hiện hành (M3 - Thành viên 2: UC03, UC17, BM2, QĐ2, BR-C)")
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final LaundryServiceService laundryServiceService;

    @Operation(summary = "Xem danh sách dịch vụ và bảng giá hiện hành (UC03 / BM2 - Nhân viên & Admin)")
    @GetMapping
    public List<ServiceResponse> getAllServices(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        return laundryServiceService.getAllServices(activeOnly);
    }

    @Operation(summary = "Xem chi tiết dịch vụ theo ID (BM2)")
    @GetMapping("/{id}")
    public ServiceResponse getServiceById(@PathVariable Long id) {
        return laundryServiceService.getServiceById(id);
    }

    @Operation(summary = "Xem chi tiết dịch vụ theo Mã (DVxx)")
    @GetMapping("/by-code/{code}")
    public ServiceResponse getServiceByCode(@PathVariable String code) {
        return laundryServiceService.getServiceByCode(code);
    }

    @Operation(summary = "Thêm dịch vụ mới vào bảng giá (UC17 - Chỉ Admin được thực hiện, BR-C01, QĐ2)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse createService(@Valid @RequestBody CreateServiceRequest request) {
        return laundryServiceService.createService(request);
    }

    @Operation(summary = "Cập nhật thông tin dịch vụ & đơn giá (UC17 - Chỉ Admin được thực hiện, BR-C01, QĐ2)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        return laundryServiceService.updateService(id, request);
    }

    @Operation(summary = "Thay đổi trạng thái hoạt động (ACTIVE / INACTIVE) - Không xóa dịch vụ đã dùng theo BR-C05")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeServiceStatusRequest request
    ) {
        return laundryServiceService.changeStatus(id, request.status());
    }
}
