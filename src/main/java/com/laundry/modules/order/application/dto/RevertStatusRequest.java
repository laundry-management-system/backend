package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request lùi trạng thái đơn hàng (BR-A05)")
public record RevertStatusRequest(
        @NotNull(message = "Trạng thái muốn lùi về không được để trống")
        @Schema(description = "Trạng thái muốn lùi về", example = "WASHING")
        OrderStatus targetStatus,

        @NotBlank(message = "Bắt buộc phải nhập lý do khi lùi trạng thái")
        @Schema(description = "Lý do lùi trạng thái", example = "Đồ sấy xong phát hiện còn vết bẩn, cần giặt lại lần 2")
        String reason
) {
}

