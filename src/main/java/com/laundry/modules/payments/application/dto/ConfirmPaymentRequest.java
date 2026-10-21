package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record ConfirmPaymentRequest(
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên tối đa 12 chữ số")
        BigDecimal amount,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        Instant paidAt,

        @Size(max = 50, message = "Mã tham chiếu tối đa 50 ký tự")
        String referenceCode
) {
}
