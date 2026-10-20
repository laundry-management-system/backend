package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response gợi ý thời gian hẹn trả đồ")
public record EstimateReturnTimeResponse(
        @Schema(description = "Thời gian hẹn trả dự kiến được tính toán")
        Instant estimatedReturnAt,

        @Schema(description = "Số giờ xử lý tiêu chuẩn", example = "24")
        int standardHours,

        @Schema(description = "Diễn giải căn cứ tính toán", example = "Dịch vụ giặt sấy tiêu chuẩn (24h) vào ngày thường")
        String explanation
) {
}

