package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.PaymentResolution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolvePaymentRequest(
        @NotNull(message = "Thao tác không được để trống")
        PaymentResolution resolution,

        @NotBlank(message = "Lý do không được để trống")
        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        String reason
) {
}
