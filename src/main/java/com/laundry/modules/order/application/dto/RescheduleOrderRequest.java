package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@Schema(description = "Request dời lịch hẹn trả đồ (BR-D08)")
public record RescheduleOrderRequest(
        @Schema(description = "Thời gian hẹn trả mới (due_at)")
        Instant newDueAt,

        @Schema(description = "Thời gian hẹn trả mới (bí danh tương thích)")
        Instant newEstimatedReturnAt,

        @NotBlank(message = "Bắt buộc phải nhập lý do dời ngày hẹn (BR-D08)")
        @Schema(description = "Lý do dời ngày hẹn", example = "Mất điện cục bộ tiệm giặt, hoãn 4 tiếng")
        String reason
) {
    public Instant getTargetDueAt() {
        return newDueAt != null ? newDueAt : newEstimatedReturnAt;
    }
}
