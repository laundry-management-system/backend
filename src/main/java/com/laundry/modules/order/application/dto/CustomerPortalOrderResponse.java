package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Đơn khách tự xem: chỉ mã, ngày nhận, trạng thái, tổng tiền (null khi chưa phân loại). */
public record CustomerPortalOrderResponse(
        String orderCode,
        Instant receivedAt,
        OrderStatus status,
        BigDecimal totalAmount
) {
    public static CustomerPortalOrderResponse from(Order order) {
        return new CustomerPortalOrderResponse(order.getOrderCode(), order.getReceivedAt(), order.getStatus(), order.getTotalAmount());
    }
}
