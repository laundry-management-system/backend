package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request hoàn thành đơn hàng và gán vị trí kệ (BM5)")
public record CompleteOrderRequest(
        @NotBlank(message = "Vị trí kệ không được để trống khi chuyển Hoàn thành (QĐ6)")
        @Schema(description = "Vị trí kệ để đồ giặt xong", example = "Kệ A-03")
        String shelfLocation
) {
}

