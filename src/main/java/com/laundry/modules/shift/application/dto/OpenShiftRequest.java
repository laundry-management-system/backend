package com.laundry.modules.shift.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record OpenShiftRequest(
        @NotNull(message = "Tiền đầu ca không được để trống")
        @PositiveOrZero(message = "Tiền đầu ca phải từ 0 trở lên")
        @Digits(integer = 12, fraction = 0, message = "Tiền đầu ca phải là số nguyên tối đa 12 chữ số")
        BigDecimal openingCash,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        String openingReason
) {
}
