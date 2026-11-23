package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.DropoffStatus;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin tóm tắt đơn hàng hiển thị trong danh sách công việc (BM4)")
public record OrderResponse(
        Long id,
        String orderCode,
        String customerName,
        String customerPhone,
        BigDecimal totalWeight,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        OrderPriority priority,
        Long washModeId,
        String washModeName,
        OrderStatus status,
        String statusDescription,
        PaymentStatus paymentStatus,
        String shelfLocation,
        Integer bagCount,
        int totalItemsCount,
        boolean isOverdue,
        Instant receivedAt,
        Instant dueAt,
        Instant createdAt,
        String intakeCode,
        Boolean washWhitesSeparately,
        OrderChannel channel,
        Instant appointmentAt,
        boolean appointmentOverdue,
        DropoffStatus dropoffStatus,
        ReturnStatus returnStatus,
        String pickupTrackingCode,
        String deliveryTrackingCode,
        Instant paymentClaimedAt
) {
    public static OrderResponse from(Order order) {
        int itemsCount = order.getItems() != null
                ? order.getItems().size()
                : 0;

        OrderFulfillment f = order.getFulfillment();
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getTotalWeight(),
                order.getTotalAmount(),
                order.payableAmount(),
                order.getPriority(),
                order.getWashModeId(),
                order.getWashModeName(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getPaymentStatus(),
                order.getShelfLocation(),
                order.getBagCount(),
                itemsCount,
                order.isOverdue(),
                order.getReceivedAt(),
                order.getDueAt(),
                order.getCreatedAt(),
                order.getIntakeCode(),
                order.getWashWhitesSeparately(),
                order.getChannel(),
                f != null ? f.getAppointmentAt() : null,
                OrderFulfillmentResponse.overdue(order),
                f != null ? f.getDropoffStatus() : null,
                f != null ? f.getReturnStatus() : null,
                f != null ? f.getPickupTrackingCode() : null,
                f != null ? f.getDeliveryTrackingCode() : null,
                order.getPaymentClaimedAt()
        );
    }
}
