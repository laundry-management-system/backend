package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request đính kèm ảnh tình trạng cho một món đồ có vấn đề")
public record AddOrderItemImageRequest(
        @NotBlank(message = "Đường dẫn ảnh không được để trống")
        @Schema(description = "URL ảnh tình trạng", example = "https://cdn.tiemgiat.vn/images/item-123.jpg")
        String imageUrl,

        @Schema(description = "Mô tả / ghi chú cho ảnh", example = "Vết rách nhỏ ở mép túi áo")
        String note
) {
}

