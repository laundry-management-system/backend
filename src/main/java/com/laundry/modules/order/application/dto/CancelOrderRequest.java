package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request hủy đơn hàng (chỉ khi chưa nhận đồ)")
public record CancelOrderRequest(
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do nội bộ; bắt buộc với đơn tại quầy", example = "Khách đổi ý")
        String reason,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do hiện cho khách; bắt buộc với đơn online")
        String customerReason
) {
    public CancelOrderRequest {
        reason = reason == null ? null : reason.trim();
        customerReason = customerReason == null ? null : customerReason.trim();
    }
}
