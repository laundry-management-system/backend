package com.laundry.modules.service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Lần ban hành bảng giá gần nhất (đổi đơn giá hoặc phụ phí); chưa có thì cả hai null")
public record PriceIssuedResponse(
        Instant effectiveFrom,
        @Schema(example = "Lê Văn Chủ")
        String issuedBy
) {}
