package com.laundry.modules.service.presentation;

import com.laundry.modules.service.application.LaundryServiceService;
import com.laundry.modules.service.application.dto.ChangeServiceStatusRequest;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import com.laundry.modules.service.domain.ServiceStatus;
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

@Tag(name = "Service & Pricing Management", description = "Service catalog and active pricing management (M3 - Member 2: UC03, UC17, BM2, QD2, BR-C)")
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class LaundryServiceController {

    private final LaundryServiceService laundryServiceService;

    @Operation(summary = "Price list (paged): search by code/name, filter by status (Staff & Admin)")
    @GetMapping
    public Page<ServiceResponse> listServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceStatus status,
            // Sắp theo id: mã DVxx là chuỗi, sort theo mã sẽ đặt DV100 trước DV11
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return laundryServiceService.listServices(search, status, pageable);
    }

    @Operation(summary = "All active services, for the service picker when classifying orders")
    @GetMapping("/active")
    public List<ServiceResponse> listActiveServices() {
        return laundryServiceService.listActiveServices();
    }

    @Operation(summary = "Get service details by ID")
    @GetMapping("/{id}")
    public ServiceResponse getServiceById(@PathVariable Long id) {
        return laundryServiceService.getServiceById(id);
    }

    @Operation(summary = "Get service details by code (DVxx)")
    @GetMapping("/by-code/{code}")
    public ServiceResponse getServiceByCode(@PathVariable String code) {
        return laundryServiceService.getServiceByCode(code);
    }

    @Operation(summary = "Add new laundry service (UC17 - Admin only, BR-C01, QD2)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse createService(@Valid @RequestBody CreateServiceRequest request) {
        return laundryServiceService.createService(request);
    }

    @Operation(summary = "Update service details and price (UC17 - Admin only, BR-C01, QD2)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        return laundryServiceService.updateService(id, request);
    }

    @Operation(summary = "Change service status (ACTIVE / INACTIVE) - Soft deactivate")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeServiceStatusRequest request
    ) {
        return laundryServiceService.changeStatus(id, request.status());
    }
}
