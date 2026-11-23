package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.DropoffStatus;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.ReturnMethod;
import com.laundry.modules.order.domain.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Giao nhận của đơn online (null với đơn tại quầy)")
public record OrderFulfillmentResponse(
        DropoffMethod dropoffMethod,
        ReturnMethod returnMethod,
        Long pickupAreaId,
        String pickupAreaName,
        Long deliveryAreaId,
        String deliveryAreaName,
        String pickupAddress,
        String deliveryAddress,
        Instant appointmentAt,
        boolean appointmentOverdue,
        String contactName,
        String contactPhone,
        Instant termsAcceptedAt,
        BigDecimal pickupFee,
        BigDecimal deliveryFee,
        DropoffStatus dropoffStatus,
        ReturnStatus returnStatus,
        String pickupTrackingCode,
        String deliveryTrackingCode,
        String customerRejectReason
) {
    /** Quá giờ hẹn gửi khi đơn còn ở Chờ xác nhận / Chờ nhận đồ (FR-017, FR-039). */
    public static boolean overdue(Order order) {
        OrderFulfillment f = order.getFulfillment();
        return f != null && order.getStatus().isBeforeDropoff() && f.getAppointmentAt().isBefore(Instant.now());
    }

    public static OrderFulfillmentResponse from(Order order) {
        OrderFulfillment f = order.getFulfillment();
        if (f == null) {
            return null;
        }
        return new OrderFulfillmentResponse(f.getDropoffMethod(), f.getReturnMethod(), f.getPickupAreaId(),
                f.getPickupAreaName(), f.getDeliveryAreaId(), f.getDeliveryAreaName(), f.getPickupAddress(), f.getDeliveryAddress(), f.getAppointmentAt(),
                overdue(order), f.getContactName(), f.getContactPhone(), f.getTermsAcceptedAt(),
                f.getPickupFee(), f.getDeliveryFee(),
                f.getDropoffStatus(), f.getReturnStatus(), f.getPickupTrackingCode(), f.getDeliveryTrackingCode(),
                f.getCustomerRejectReason());
    }
}
