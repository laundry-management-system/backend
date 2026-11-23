package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.CancelReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request từ chối đơn online ở Chờ xác nhận")
public record RejectOrderRequest(
        @NotNull(message = "Chọn lý do hủy")
        @Schema(description = "Lý do từ chối theo danh mục (6 giá trị dùng chung cancel/reject)")
        CancelReason reasonCode,

        @NotBlank(message = "Nhập lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do hiện cho khách")
        String customerReason,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do nội bộ, tối đa 500 ký tự; bắt buộc khi reasonCode = OTHER")
        String reason
) {
    public RejectOrderRequest {
        customerReason = customerReason == null ? null : customerReason.trim();
        reason = reason == null ? null : reason.trim();
    }
}
