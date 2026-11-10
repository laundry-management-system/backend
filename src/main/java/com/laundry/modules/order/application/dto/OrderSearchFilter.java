package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;

import java.time.Instant;

/**
 * Bộ lọc danh sách đơn. {@code phone} so khớp chính xác; tìm gần đúng theo mã / tên / SĐT dùng {@code search}.
 * {@code receivedTo} là mốc loại trừ (receivedAt &lt; receivedTo).
 */
public record OrderSearchFilter(
        OrderStatus status,
        OrderPriority priority,
        String phone,
        String search,
        String intakeCode,
        Long customerId,
        Instant receivedFrom,
        Instant receivedTo,
        PaymentStatus paymentStatus,
        OrderType orderType,
        Boolean classified
) {
}
