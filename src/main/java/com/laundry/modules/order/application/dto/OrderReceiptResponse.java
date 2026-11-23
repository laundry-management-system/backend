package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Dữ liệu in phiếu nhận đồ thông minh 2 liên (Smart Receipt - BM3)")
public record OrderReceiptResponse(
        Long orderId,
        String orderCode,
        String customerName,
        String customerPhone,
        BigDecimal totalWeight,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String note,
        OrderPriority priority,
        Long washModeId,
        String washModeName,
        PaymentStatus paymentStatus,
        Integer bagCount,
        Instant receivedAt,
        Instant dueAt,
        String trackingQrUrl,
        String shopName,
        String hotline,
        String intakeCode,
        int intakeOrderIndex,
        int intakeOrderCount,
        Boolean washWhitesSeparately,
        BigDecimal whitesFee,
        String createdBy,
        List<ReceiptServiceLine> services,
        Integer compensationMultiplier,
        BigDecimal compensationCap
) {
    /** Dịch vụ kèm đơn giá đã chụp lúc tạo đơn; expressSurcharge chỉ có ở đơn giặt nhanh cũ (null với đơn mới, FR-022). */
    public record ReceiptServiceLine(String name, ItemUnit unit, BigDecimal unitPrice,
                                     BigDecimal expressSurcharge, BigDecimal minQuantity) {
    }

    public static OrderReceiptResponse from(Order order, String webBaseUrl) {
        return from(order, webBaseUrl, 1, 1, order.getWhitesFee(), List.of(),
                order.getCompensationMultiplier(), order.getCompensationCap());
    }

    public static OrderReceiptResponse from(Order order, String webBaseUrl, int intakeOrderIndex, int intakeOrderCount,
                                            BigDecimal whitesFee, List<ReceiptServiceLine> services,
                                            Integer compensationMultiplier, BigDecimal compensationCap) {
        String base = (webBaseUrl != null && !webBaseUrl.isBlank()) ? webBaseUrl : "http://localhost:5173";
        String trackingQrUrl = base + "/track?phone=" + order.getCustomerPhone();

        return new OrderReceiptResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getTotalWeight(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getNote(),
                order.getPriority(),
                order.getWashModeId(),
                order.getWashModeName(),
                order.getPaymentStatus(),
                order.getBagCount(),
                order.getReceivedAt(),
                order.getDueAt(),
                trackingQrUrl,
                "Tiệm Giặt Ủi Thông Minh SE114",
                "1900 1234",
                order.getIntakeCode(),
                intakeOrderIndex,
                intakeOrderCount,
                order.getWashWhitesSeparately(),
                whitesFee,
                order.getCreatedBy(),
                services,
                compensationMultiplier,
                compensationCap
        );
    }
}
