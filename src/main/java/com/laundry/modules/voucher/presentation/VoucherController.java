package com.laundry.modules.voucher.presentation;

import com.laundry.modules.voucher.application.VoucherService;
import com.laundry.modules.voucher.application.dto.ChangeVoucherStatusRequest;
import com.laundry.modules.voucher.application.dto.CreateVoucherRequest;
import com.laundry.modules.voucher.application.dto.UpdateVoucherRequest;
import com.laundry.modules.voucher.application.dto.ValidateVoucherRequest;
import com.laundry.modules.voucher.application.dto.VoucherCheckResponse;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Voucher Management", description = "Quản lý voucher và kiểm tra mã (UC19 / BM10 / QĐ12, BR-G)")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Kiểm tra mã với tiền hàng hiện tại (ô áp mã WF-C03); trả mã lý do khớp FE")
    @GetMapping("/check")
    public VoucherCheckResponse check(@RequestParam String code,
                                      @RequestParam(required = false) BigDecimal subtotal) {
        return voucherService.check(code, subtotal);
    }

    @Operation(summary = "Kiểm tra mã và tính tiền giảm (dạng của bản release/qc)")
    @PostMapping("/validate")
    public VoucherValidationResponse validate(@Valid @RequestBody ValidateVoucherRequest request) {
        return voucherService.validate(request.code(), request.orderValue());
    }

    @Operation(summary = "Danh sách voucher: tìm theo mã, lọc trạng thái (NV & Admin)")
    @GetMapping
    public Page<VoucherResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VoucherStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return voucherService.list(search, status, pageable);
    }

    @Operation(summary = "Chi tiết voucher theo id (NV & Admin)")
    @GetMapping("/{id}")
    public VoucherResponse get(@PathVariable Long id) {
        return voucherService.get(id);
    }

    @Operation(summary = "Chi tiết voucher theo mã (NV & Admin)")
    @GetMapping("/by-code/{code}")
    public VoucherResponse getByCode(@PathVariable String code) {
        return voucherService.getByCode(code);
    }

    @Operation(summary = "Tạo voucher (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse create(@Valid @RequestBody CreateVoucherRequest request, Authentication authentication) {
        return voucherService.create(request, authentication.getName());
    }

    @Operation(summary = "Sửa voucher (Admin). Không đổi mã; voucher đã có đơn dùng thì không đổi loại giảm giá")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse update(@PathVariable Long id, @Valid @RequestBody UpdateVoucherRequest request) {
        return voucherService.update(id, request);
    }

    @Operation(summary = "Bật / tắt voucher (Admin). Không có xóa")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody ChangeVoucherStatusRequest request) {
        return voucherService.changeStatus(id, request.status());
    }
}
