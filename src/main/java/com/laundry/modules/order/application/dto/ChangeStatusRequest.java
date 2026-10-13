package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request chuyển trạng thái đơn hàng")
public record ChangeStatusRequest(
        @NotNull(message = "Trạng thái mới không được để trống")
        @Schema(description = "Trạng thái mới", example = "WASHING")
        OrderStatus newStatus,

        @Schema(description = "Ghi chú thêm về trạng thái", example = "Bắt đầu cho vào máy giặt số 3")
        String note
) {
}

