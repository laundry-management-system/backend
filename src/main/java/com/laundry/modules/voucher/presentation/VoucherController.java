package com.laundry.modules.voucher.presentation;

import com.laundry.modules.voucher.application.VoucherService;
import com.laundry.modules.voucher.application.dto.ChangeVoucherStatusRequest;
import com.laundry.modules.voucher.application.dto.VoucherCheckResponse;
import com.laundry.modules.voucher.application.dto.VoucherRequest;
import com.laundry.modules.voucher.application.dto.VoucherResponse;
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

@Tag(name = "Voucher", description = "Quản lý voucher và kiểm tra mã (UC19 / BM10 / QĐ12)")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Kiểm tra mã voucher với tiền hàng hiện tại (ô áp mã WF-C03)")
    @GetMapping("/check")
    public VoucherCheckResponse check(@RequestParam String code,
                                      @RequestParam(required = false) BigDecimal subtotal) {
        return voucherService.check(code, subtotal);
    }

    @Operation(summary = "Danh sách voucher (Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<VoucherResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VoucherStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return voucherService.list(search, status, pageable);
    }

    @Operation(summary = "Chi tiết voucher (Admin)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse get(@PathVariable Long id) {
        return voucherService.get(id);
    }

    @Operation(summary = "Tạo voucher (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse create(@Valid @RequestBody VoucherRequest request, Authentication authentication) {
        return voucherService.create(request, authentication.getName());
    }

    @Operation(summary = "Sửa voucher (Admin). Voucher đã có đơn dùng thì không đổi được mã và loại giảm giá")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VoucherResponse update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
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
