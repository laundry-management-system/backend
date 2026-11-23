package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CompleteDropoffRequest(
        @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự")
        String pickupTrackingCode,
        /** Hạn trả nhân viên chọn ở bước xác nhận; null = gợi ý minDue (FR-039). */
        Instant dueAt
) {
    public CompleteDropoffRequest {
        pickupTrackingCode = pickupTrackingCode == null ? null : pickupTrackingCode.trim();
    }
}
