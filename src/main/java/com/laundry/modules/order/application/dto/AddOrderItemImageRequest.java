package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request đính kèm ảnh tình trạng cho một món đồ có vấn đề")
public record AddOrderItemImageRequest(
        @Schema(description = "ID file đã upload qua POST /api/documents (ưu tiên)", example = "12")
        Long documentId,

        @Schema(description = "URL ảnh tình trạng (cách cũ, khi không có documentId)", example = "https://cdn.tiemgiat.vn/images/item-123.jpg")
        String imageUrl,

        @Schema(description = "Mô tả / ghi chú cho ảnh", example = "Vết rách nhỏ ở mép túi áo")
        String note
) {
}

