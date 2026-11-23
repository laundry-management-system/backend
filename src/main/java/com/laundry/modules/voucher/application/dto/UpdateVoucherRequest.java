package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateVoucherRequest(
        @NotNull(message = "Discount type must not be null")
        DiscountType discountType,

        @NotNull(message = "Discount value must not be null")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        BigDecimal discountValue,

        @NotNull(message = "Start date must not be null (BR-G01)")
        LocalDate startDate,

        @NotNull(message = "End date must not be null (BR-G01)")
        LocalDate endDate,

        @DecimalMin(value = "0", message = "Minimum order value must be greater than or equal to 0 (BR-G02)")
        BigDecimal minOrderValue,

        @NotNull(message = "Maximum uses must not be null (BR-G03)")
        @Min(value = 1, message = "Maximum uses must be at least 1 (BR-G03)")
        Integer maxUses
) {}
