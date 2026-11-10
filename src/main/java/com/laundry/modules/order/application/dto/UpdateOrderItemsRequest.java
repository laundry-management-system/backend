package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request cập nhật danh sách chi tiết món đồ sau khi nhân viên phân loại kiểm đếm")
public record UpdateOrderItemsRequest(
        @NotEmpty(message = "Danh sách món đồ không được để trống")
        @Valid
        @Schema(description = "Danh sách các món đồ đã đếm")
        List<OrderItemDto> items,

        @DecimalMin(value = "0", message = "Giảm giá không được âm")
        @Schema(description = "Không còn dùng: BE bỏ qua, giảm giá chỉ tính từ voucherCode", deprecated = true)
        @Deprecated
        BigDecimal discountAmount,

        @Size(max = 30, message = "Mã voucher tối đa 30 ký tự")
        @Schema(description = "Mã voucher áp cho đơn (đơn v3). Bỏ trống = không dùng voucher; mã cũ được trả lượt", example = "GIAT10")
        String voucherCode
) {
}

