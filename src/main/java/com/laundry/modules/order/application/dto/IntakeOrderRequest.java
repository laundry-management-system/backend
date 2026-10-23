package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** Một đơn trong lượt nhận. serviceIds chỉ là dịch vụ tham khảo, không tạo dòng đồ. */
public record IntakeOrderRequest(
        @NotNull(message = "Thiếu loại đơn")
        OrderType orderType,

        @NotEmpty(message = "Chọn ít nhất một dịch vụ")
        List<@NotNull Long> serviceIds,

        @NotNull(message = "Vui lòng chọn ngày hẹn trả")
        Instant dueAt,

        boolean washWhitesSeparately
) {
}
