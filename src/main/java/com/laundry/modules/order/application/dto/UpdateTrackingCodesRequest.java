package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** null = giữ nguyên, rỗng = xóa. */
public record UpdateTrackingCodesRequest(
        @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự")
        String pickupTrackingCode,

        @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự")
        String deliveryTrackingCode
) {
    public UpdateTrackingCodesRequest {
        pickupTrackingCode = pickupTrackingCode == null ? null : pickupTrackingCode.trim();
        deliveryTrackingCode = deliveryTrackingCode == null ? null : deliveryTrackingCode.trim();
    }
}
