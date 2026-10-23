package com.laundry.modules.payments.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentDiscrepancyRequest(
        @NotNull(message = "Số tiền nhận được không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền nhận được phải lớn hơn 0")
        @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên tối đa 12 chữ số")
        BigDecimal receivedAmount,

        @NotBlank(message = "Ghi chú chênh lệch không được để trống")
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
}
