package com.laundry.modules.booking.application.dto;

import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.ReturnMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OnlineBookingResponse(String intakeCode, boolean created, List<OrderSummary> orders) {

    public record OrderSummary(
            String orderCode,
            OrderType orderType,
            OrderStatus status,
            Instant createdAt,
            Instant appointmentAt,
            Instant dueAt,
            String contactName,
            String contactPhone,
            DropoffMethod dropoffMethod,
            ReturnMethod returnMethod,
            String pickupAreaName,
            String deliveryAreaName,
            String pickupAddress,
            String deliveryAddress,
            String note,
            List<ServiceRef> services,
            boolean washWhitesSeparately,
            BigDecimal pickupFee,
            BigDecimal deliveryFee
    ) {
    }

    public record ServiceRef(Long serviceId, String serviceName) {
    }

    public static OnlineBookingResponse from(Order order, boolean created, Map<Long, String> serviceNames) {
        return new OnlineBookingResponse(order.getIntakeCode(), created, List.of(summary(order, serviceNames)));
    }

    private static OrderSummary summary(Order o, Map<Long, String> serviceNames) {
        OrderFulfillment f = o.getFulfillment();
        return new OrderSummary(o.getOrderCode(), o.getOrderType(), o.getStatus(), o.getCreatedAt(),
                f.getAppointmentAt(), o.getDueAt(), f.getContactName(), f.getContactPhone(),
                f.getDropoffMethod(), f.getReturnMethod(), f.getPickupAreaName(), f.getDeliveryAreaName(),
                f.getPickupAddress(),
                f.getDeliveryAddress(), o.getNote(),
                o.getRequestedServiceIds().stream().map(id -> new ServiceRef(id, serviceNames.get(id))).toList(),
                Boolean.TRUE.equals(o.getWashWhitesSeparately()), f.getPickupFee(), f.getDeliveryFee());
    }
}
