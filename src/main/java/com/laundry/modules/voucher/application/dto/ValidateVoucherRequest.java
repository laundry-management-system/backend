package com.laundry.modules.voucher.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidateVoucherRequest(
        @NotBlank(message = "Voucher code must not be blank")
        String code,

        @NotNull(message = "Order value must not be null")
        @DecimalMin(value = "0", message = "Order value must be greater than or equal to 0")
        BigDecimal orderValue
) {}
