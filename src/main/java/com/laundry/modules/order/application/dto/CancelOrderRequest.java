package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.CancelReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request hủy đơn hàng (chỉ khi chưa nhận đồ)")
public record CancelOrderRequest(
        @NotNull(message = "Chọn lý do hủy")
        @Schema(description = "Lý do hủy theo danh mục (6 giá trị dùng chung cancel/reject)")
        CancelReason reasonCode,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Lý do nội bộ, tối đa 500 ký tự; bắt buộc khi reasonCode = OTHER", example = "Khách đổi ý")
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
