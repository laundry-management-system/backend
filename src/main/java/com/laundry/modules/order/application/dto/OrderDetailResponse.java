package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Chi tiết đầy đủ đơn hàng dành cho nhân viên (BM3, BM5)")
public record OrderDetailResponse(
        Long id,
        String orderCode,
        Long customerId,
        String customerName,
        String customerPhone,
        BigDecimal totalWeight,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String voucherCode,
        BigDecimal expressFee,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        String note,
        OrderPriority priority,
        Long washModeId,
        String washModeName,
        @Schema(description = "Giờ hoàn thành đã chụp trên đơn (chỉ đọc)") Integer washModeHours,
        OrderStatus status,
        String statusDescription,
        PaymentStatus paymentStatus,
        Integer bagCount,
        String shelfLocation,
        boolean isOverdue,
        Instant receivedAt,
        Instant dueAt,
        Instant receiptPrintedAt,
        Instant completedAt,
        Instant deliveredAt,
        String deliveredBy,
        String receiverName,
        String receiverRelation,
        List<OrderItemDto> items,
        List<OrderStatusHistoryResponse> historyList,
        String intakeCode,
        List<RequestedServiceDto> requestedServices,
        Boolean washWhitesSeparately,
        BigDecimal whitesFee,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal incidentDiscount,
        List<String> incidentCodes,
        Integer compensationMultiplier,
        BigDecimal compensationCap,
        OrderChannel channel,
        Instant confirmedAt,
        String confirmedBy,
        OrderFulfillmentResponse fulfillment,
        Instant paymentClaimedAt,
        String paymentClaimRejectedReason,
        Instant paymentClaimRejectedAt
) {
    public static OrderDetailResponse from(Order order) {
        List<RequestedServiceDto> requested = order.getRequestedServiceIds() == null ? List.of()
                : order.getRequestedServiceIds().stream().map(id -> new RequestedServiceDto(id, null, null,
                        order.getRequestedServicePrices() != null ? order.getRequestedServicePrices().get(id) : null)).toList();
        return from(order, requested, List.of(), order.getCompensationMultiplier(), order.getCompensationCap());
    }

    /** {@code compensationMultiplier} / {@code compensationCap}: điều khoản đã quy đổi (đơn chưa chốt thì lấy cấu hình hiện hành). */
    public static OrderDetailResponse from(Order order, List<RequestedServiceDto> requestedServices,
                                           List<String> incidentCodes, Integer compensationMultiplier,
                                           BigDecimal compensationCap) {
        List<OrderItemDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(OrderItemDto::from).toList()
                : List.of();

        List<OrderStatusHistoryResponse> historyDtos = order.getHistoryList() != null
                ? order.getHistoryList().stream().map(OrderStatusHistoryResponse::from).toList()
                : List.of();

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getTotalWeight(),
                order.getUnitPrice(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getVoucherCode(),
                order.getExpressFee(),
                order.getTotalAmount(),
                order.payableAmount(),
                order.getNote(),
                order.getPriority(),
                order.getWashModeId(),
                order.getWashModeName(),
                order.getWashModeHours(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getPaymentStatus(),
                order.getBagCount(),
                order.getShelfLocation(),
                order.isOverdue(),
                order.getReceivedAt(),
                order.getDueAt(),
                order.getReceiptPrintedAt(),
                order.getCompletedAt(),
                order.getDeliveredAt(),
                order.getDeliveredBy(),
                order.getReceiverName(),
                order.getReceiverRelation(),
                itemDtos,
                historyDtos,
                order.getIntakeCode(),
                requestedServices,
                order.getWashWhitesSeparately(),
                order.getWhitesFee(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getIncidentDiscount(),
                incidentCodes,
                compensationMultiplier,
                compensationCap,
                order.getChannel(),
                order.getConfirmedAt(),
                order.getConfirmedBy(),
                OrderFulfillmentResponse.from(order),
                order.getPaymentClaimedAt(),
                order.getPaymentClaimRejectedReason(),
                order.getPaymentClaimRejectedAt()
        );
    }
}
