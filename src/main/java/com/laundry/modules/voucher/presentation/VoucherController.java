package com.laundry.modules.voucher.presentation;

import com.laundry.modules.voucher.application.VoucherService;
import com.laundry.modules.voucher.application.dto.ChangeVoucherStatusRequest;
import com.laundry.modules.voucher.application.dto.CreateVoucherRequest;
import com.laundry.modules.voucher.application.dto.UpdateVoucherRequest;
import com.laundry.modules.voucher.application.dto.ValidateVoucherRequest;
import com.laundry.modules.voucher.application.dto.VoucherResponse;
import com.laundry.modules.voucher.application.dto.VoucherValidationResponse;
import com.laundry.modules.voucher.domain.VoucherStatus;
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

@Tag(name = "Voucher Management", description = "Voucher catalog and discount validation (M3 - Member 2: UC19, QĐ12, BM10, BR-G)")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Create new discount voucher (UC19 - Admin only, BR-G01 to BR-G05)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse createVoucher(
            @Valid @RequestBody CreateVoucherRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        return voucherService.createVoucher(request, username);
    }

    @Operation(summary = "Update voucher details (UC19 - Admin only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherRequest request
    ) {
        return voucherService.updateVoucher(id, request);
    }

    @Operation(summary = "Change voucher status ACTIVE / DISABLED")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeVoucherStatusRequest request
    ) {
        return voucherService.changeStatus(id, request.status());
    }

    @Operation(summary = "List vouchers with search, status filter, and pagination (Staff & Admin)")
    @GetMapping
    public Page<VoucherResponse> listVouchers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VoucherStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return voucherService.listVouchers(search, status, pageable);
    }

    @Operation(summary = "Get voucher details by ID (BM10 - Staff & Admin)")
    @GetMapping("/{id}")
    public VoucherResponse getVoucherById(@PathVariable Long id) {
        return voucherService.getVoucherById(id);
    }

    @Operation(summary = "Get voucher details by code (Staff & Admin)")
    @GetMapping("/by-code/{code}")
    public VoucherResponse getVoucherByCode(@PathVariable String code) {
        return voucherService.getVoucherByCode(code);
    }

    @Operation(summary = "Validate voucher code and calculate discount amount for order (UC04 / QĐ12)")
    @PostMapping("/validate")
    public VoucherValidationResponse validateVoucher(@Valid @RequestBody ValidateVoucherRequest request) {
        return voucherService.validateVoucher(request.code(), request.orderValue());
    }
}
