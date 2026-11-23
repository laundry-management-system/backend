package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteDropoffRequest(
        @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự")
        String pickupTrackingCode
) {
    public CompleteDropoffRequest {
        pickupTrackingCode = pickupTrackingCode == null ? null : pickupTrackingCode.trim();
    }
}
