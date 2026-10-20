package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request điều chỉnh giá đơn hàng")
public record AdjustPriceRequest(
        @NotNull(message = "Tổng tiền mới không được để trống")
        @DecimalMin(value = "0.0", message = "Tổng tiền mới phải lớn hơn hoặc bằng 0")
        @Schema(description = "Tổng tiền mới", example = "95000")
        BigDecimal newTotalAmount,

        @NotBlank(message = "Bắt buộc phải nhập lý do điều chỉnh giá")
        @Schema(description = "Lý do sửa giá", example = "Giảm giá 10% khách hàng thân thiết")
        String reason
) {
}

