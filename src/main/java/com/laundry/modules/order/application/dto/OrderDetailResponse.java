package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ServiceType;
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
        ServiceType serviceType,
        String serviceName,
        BigDecimal totalWeight,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal expressFee,
        BigDecimal totalAmount,
        String note,
        OrderPriority priority,
        OrderType orderType,
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
        @Schema(description = "Mã voucher đã áp (null nếu không có)")
        String voucherCode,
        @Schema(description = "Lần nhắn khách gần nhất")
        Instant lastNotifiedAt,
        @Schema(description = "Số lần đã nhắn khách")
        Integer notifyCount,
        @Schema(description = "Phương thức của giao dịch thanh toán đang hiệu lực (CASH / BANK_TRANSFER)")
        String paymentMethod,
        @Schema(description = "Thời điểm thanh toán của giao dịch đang hiệu lực")
        Instant paidAt,
        @Schema(description = "Số lần xử lý (tăng khi mở lần xử lý mới theo phiếu sự cố)")
        Integer processingCount
) {
    /** Thông tin không nằm trên entity Order: mã voucher và giao dịch thanh toán đang hiệu lực. */
    public record Extras(String voucherCode, String paymentMethod, Instant paidAt) {
        public static final Extras NONE = new Extras(null, null, null);
    }

    public static OrderDetailResponse from(Order order) {
        List<RequestedServiceDto> requested = order.getRequestedServiceIds() == null ? List.of()
                : order.getRequestedServiceIds().stream().map(id -> new RequestedServiceDto(id, null, null)).toList();
        return from(order, requested, Extras.NONE);
    }

    public static OrderDetailResponse from(Order order, List<RequestedServiceDto> requestedServices, Extras extras) {
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
                order.getServiceType(),
                order.getServiceType() != null ? order.getServiceType().getDisplayName() : null,
                order.getTotalWeight(),
                order.getUnitPrice(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getExpressFee(),
                order.getTotalAmount(),
                order.getNote(),
                order.getPriority(),
                order.getOrderType(),
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
                extras.voucherCode(),
                order.getLastNotifiedAt(),
                order.getNotifyCount(),
                extras.paymentMethod(),
                extras.paidAt(),
                order.getProcessingCount()
        );
    }
}
