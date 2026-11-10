package com.laundry.modules.voucher.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidateVoucherRequest(
        @NotBlank(message = "Vui lòng nhập mã voucher")
        String code,
        @NotNull(message = "Thiếu tiền hàng của đơn")
        @DecimalMin(value = "0", message = "Tiền hàng không được âm")
        BigDecimal orderValue
) {
}
