package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentClaimRequest(
        @NotBlank(message = "Nhập lý do (tối đa 200 ký tự)")
        @Size(max = 200, message = "Nhập lý do (tối đa 200 ký tự)")
        String reason
) {
    public RejectPaymentClaimRequest {
        reason = reason == null ? null : reason.trim();
    }
}
