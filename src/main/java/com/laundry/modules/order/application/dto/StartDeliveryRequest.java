package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartDeliveryRequest(
        @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự")
        String deliveryTrackingCode
) {
    public StartDeliveryRequest {
        deliveryTrackingCode = deliveryTrackingCode == null ? null : deliveryTrackingCode.trim();
    }
}
