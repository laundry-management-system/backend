package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import com.laundry.modules.voucher.domain.Voucher;

import java.math.BigDecimal;

public record VoucherValidationResponse(
        boolean valid,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message,
        VoucherResponse voucher
) {
    public static VoucherValidationResponse valid(Voucher voucher, BigDecimal orderValue, BigDecimal discountAmount) {
        BigDecimal finalAmount = orderValue.subtract(discountAmount).max(BigDecimal.ZERO);
        return new VoucherValidationResponse(
                true,
                voucher.getCode(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                discountAmount,
                finalAmount,
                "Voucher applied successfully",
                VoucherResponse.from(voucher)
        );
    }

    public static VoucherValidationResponse invalid(String code, String message) {
        return new VoucherValidationResponse(
                false,
                code,
                null,
                null,
                BigDecimal.ZERO,
                null,
                message,
                null
        );
    }
}
