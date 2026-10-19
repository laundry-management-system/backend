package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request hủy đơn hàng")
public record CancelOrderRequest(
        @NotBlank(message = "Bắt buộc phải nhập lý do hủy đơn hàng")
        @Schema(description = "Lý do hủy đơn", example = "Khách đổi ý muốn lấy lại đồ ngay")
        String reason
) {
}

