package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import com.laundry.modules.voucher.domain.VoucherCheckReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Kết quả của {@code POST /api/vouchers/validate} (giữ dạng của bản release/qc, thêm {@code reason}). */
public record VoucherValidationResponse(
        boolean valid,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message,
        @Schema(description = "Mã lý do khi không hợp lệ (giống GET /vouchers/check)")
        VoucherCheckReason reason,
        VoucherResponse voucher
) {
    public static VoucherValidationResponse from(String code, BigDecimal orderValue, VoucherCheckResponse check) {
        VoucherResponse v = check.voucher();
        if (!check.ok()) {
            return new VoucherValidationResponse(false, code, v == null ? null : v.discountType(),
                    v == null ? null : v.discountValue(), BigDecimal.ZERO, null, check.message(), check.reason(), v);
        }
        return new VoucherValidationResponse(true, v.code(), v.discountType(), v.discountValue(), check.discount(),
                orderValue.subtract(check.discount()).max(BigDecimal.ZERO), "Áp dụng voucher thành công", null, v);
    }
}
