package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request cập nhật danh sách chi tiết món đồ sau khi nhân viên phân loại kiểm đếm")
public record UpdateOrderItemsRequest(
        @NotEmpty(message = "Danh sách món đồ không được để trống")
        @Valid
        @Schema(description = "Danh sách các món đồ đã đếm")
        List<OrderItemDto> items,

        @DecimalMin(value = "0", message = "Giảm giá không được âm")
        @Schema(description = "Giảm giá (đơn v3; voucher FE). BE kẹp không vượt tiền hàng")
        BigDecimal discountAmount
) {
}

