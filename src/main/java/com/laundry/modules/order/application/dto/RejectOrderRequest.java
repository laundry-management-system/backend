package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request từ chối đơn online ở Chờ xác nhận")
public record RejectOrderRequest(
        @NotBlank(message = "Nhập lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do hiện cho khách")
        String customerReason,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do nội bộ (tùy chọn)")
        String reason
) {
    public RejectOrderRequest {
        customerReason = customerReason == null ? null : customerReason.trim();
        reason = reason == null ? null : reason.trim();
    }
}
