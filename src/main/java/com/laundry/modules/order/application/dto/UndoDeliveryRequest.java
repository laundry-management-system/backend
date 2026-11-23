package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UndoDeliveryRequest(
        @NotBlank(message = "Nhập lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String reason
) {
    public UndoDeliveryRequest {
        reason = reason == null ? null : reason.trim();
    }
}
