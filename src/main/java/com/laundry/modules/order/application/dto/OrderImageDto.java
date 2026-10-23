package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderImage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Ảnh tình trạng đồ khi nhận (ố, rách, có vấn đề)")
public record OrderImageDto(
        Long id,
        Long documentId,
        String imageUrl,
        String note,
        Instant createdAt
) {
    public static OrderImageDto from(OrderImage image) {
        return new OrderImageDto(
                image.getId(),
                image.getDocument() != null ? image.getDocument().getId() : null,
                image.getImageUrl(),
                image.getNote(),
                image.getCreatedAt()
        );
    }
}

