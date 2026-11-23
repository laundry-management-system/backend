package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.DropoffStatus;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Tra cứu công khai: chỉ danh sách trắng FR-028a; không bao giờ có địa chỉ, liên hệ, lý do / ghi chú nội bộ. */
@Schema(description = "Tra cứu đơn công khai theo SĐT (FR-028a)")
public record CustomerTrackingResponse(
        String orderCode,
        OrderStatus status,
        DropoffStatus dropoffStatus,
        ReturnStatus returnStatus,
        BigDecimal pickupFee,
        BigDecimal deliveryFee,
        String pickupTrackingCode,
        String deliveryTrackingCode,
        String customerRejectReason
) {
    public static CustomerTrackingResponse from(Order order) {
        OrderFulfillment f = order.getFulfillment();
        if (f == null) {
            return new CustomerTrackingResponse(order.getOrderCode(), order.getStatus(), null, null, null, null, null, null, null);
        }
        return new CustomerTrackingResponse(order.getOrderCode(), order.getStatus(), f.getDropoffStatus(),
                f.getReturnStatus(), f.getPickupFee(), f.getDeliveryFee(),
                f.getPickupTrackingCode(), f.getDeliveryTrackingCode(), f.getCustomerRejectReason());
    }
}
