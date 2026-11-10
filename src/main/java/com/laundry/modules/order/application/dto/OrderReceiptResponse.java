package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ServiceType;
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
        ServiceType serviceType,
        String serviceName,
        BigDecimal totalWeight,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String note,
        OrderPriority priority,
        OrderType orderType,
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
        @Schema(description = "Địa chỉ tiệm (cài đặt STORE_ADDRESS)")
        String shopAddress,
        @Schema(description = "Mã voucher đã áp (null nếu không có)")
        String voucherCode,
        BigDecimal discountAmount
) {
    /** Dịch vụ tham khảo kèm giá hiện hành (giá tham khảo, chưa chốt). */
    public record ReceiptServiceLine(String name, ItemUnit unit, BigDecimal unitPrice,
                                     BigDecimal expressSurcharge, BigDecimal minQuantity) {
    }

    /** Thông tin tiệm lấy từ cài đặt (STORE_NAME, STORE_PHONE, STORE_ADDRESS). */
    public record ShopInfo(String name, String hotline, String address) {
    }

    public static OrderReceiptResponse from(Order order, String webBaseUrl, int intakeOrderIndex, int intakeOrderCount,
                                            BigDecimal whitesFee, List<ReceiptServiceLine> services,
                                            ShopInfo shop, String voucherCode) {
        String base = (webBaseUrl != null && !webBaseUrl.isBlank()) ? webBaseUrl : "http://localhost:5173";
        String trackingQrUrl = base + "/track?phone=" + order.getCustomerPhone();

        return new OrderReceiptResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getServiceType(),
                order.getServiceType() != null ? order.getServiceType().getDisplayName() : null,
                order.getTotalWeight(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getNote(),
                order.getPriority(),
                order.getOrderType(),
                order.getPaymentStatus(),
                order.getBagCount(),
                order.getReceivedAt(),
                order.getDueAt(),
                trackingQrUrl,
                shop.name(),
                shop.hotline(),
                order.getIntakeCode(),
                intakeOrderIndex,
                intakeOrderCount,
                order.getWashWhitesSeparately(),
                whitesFee,
                order.getCreatedBy(),
                services,
                shop.address(),
                voucherCode,
                order.getDiscountAmount()
        );
    }
}
