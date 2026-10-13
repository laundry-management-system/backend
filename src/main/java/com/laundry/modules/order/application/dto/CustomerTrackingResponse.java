package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin tra cứu tiến độ giặt dành cho khách hàng (BM6, BR-A05)")
public record CustomerTrackingResponse(
        String orderCode,
        String customerName,
        ServiceType serviceType,
        String serviceName,
        BigDecimal totalWeight,
        BigDecimal totalAmount,
        OrderStatus status,
        String statusDescription,
        PaymentStatus paymentStatus,
        OrderType orderType,
        String shelfLocation,
        Instant receivedAt,
        Instant dueAt,
        Instant completedAt,
        Instant deliveredAt,
        String note,
        List<OrderItemDto> items,
        List<OrderStatusHistoryResponse> timeline
) {
    public static CustomerTrackingResponse from(Order order) {
        List<OrderItemDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(OrderItemDto::from).toList()
                : List.of();

        List<OrderStatusHistoryResponse> timeline = order.getHistoryList() != null
                ? order.getHistoryList().stream().map(OrderStatusHistoryResponse::from).toList()
                : List.of();

        return new CustomerTrackingResponse(
                order.getOrderCode(),
                order.getCustomerName(),
                order.getServiceType(),
                order.getServiceType() != null ? order.getServiceType().getDisplayName() : null,
                order.getTotalWeight(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getPaymentStatus(),
                order.getOrderType(),
                order.getShelfLocation(),
                order.getReceivedAt(),
                order.getDueAt(),
                order.getCompletedAt(),
                order.getDeliveredAt(),
                order.getNote(),
                itemDtos,
                timeline
        );
    }
}
