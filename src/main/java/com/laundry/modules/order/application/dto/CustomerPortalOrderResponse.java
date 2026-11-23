package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.DropoffStatus;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderItem;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ReturnMethod;
import com.laundry.modules.order.domain.ReturnStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Đơn khách tự xem (contracts §2); chỉ lý do dành cho khách, không bao giờ lý do / ghi chú nội bộ. */
public record CustomerPortalOrderResponse(
        String orderCode,
        Instant createdAt,
        OrderChannel channel,
        OrderType orderType,
        Fulfillment fulfillment,
        String customerRejectReason,
        Instant receivedAt,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        boolean classified,
        List<ServiceLine> services,
        BigDecimal whitesFee,
        PaymentStatus paymentStatus,
        Instant paymentClaimedAt
) {
    public record ServiceLine(String serviceName, boolean extra) {}

    /** Null với đơn OFFLINE. */
    public record Fulfillment(DropoffMethod dropoffMethod, ReturnMethod returnMethod, Instant appointmentAt,
                              Instant dueAt, DropoffStatus dropoffStatus, ReturnStatus returnStatus,
                              String pickupTrackingCode, String deliveryTrackingCode, BigDecimal pickupFee, BigDecimal deliveryFee,
                              String pickupAreaName, String pickupAddress, String deliveryAreaName, String deliveryAddress) {}

    /** Id dịch vụ cần tên: dịch vụ thực tế nếu đã phân loại, không thì dịch vụ khách chọn (thứ tự xuất hiện, không trùng). */
    public static List<Long> serviceIds(Order order) {
        List<Long> ids = order.getItems().isEmpty()
                ? (order.getRequestedServiceIds() == null ? List.<Long>of() : order.getRequestedServiceIds())
                : order.getItems().stream().map(OrderItem::getServiceId).toList();
        return List.copyOf(ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /** {@code serviceNames}: id -> tên (gồm dịch vụ ngừng); id không còn tồn tại bị bỏ qua. */
    public static CustomerPortalOrderResponse from(Order order, Map<Long, String> serviceNames) {
        boolean classified = !order.getItems().isEmpty();
        List<ServiceLine> services = serviceIds(order).stream().filter(serviceNames::containsKey)
                .map(id -> new ServiceLine(serviceNames.get(id), classified && order.isExtraService(id)))
                .toList();
        BigDecimal whitesFee = classified && order.getWhitesFee() != null ? order.getWhitesFee() : BigDecimal.ZERO;
        OrderFulfillment f = order.getFulfillment();
        Fulfillment fulfillment = f == null ? null : new Fulfillment(f.getDropoffMethod(), f.getReturnMethod(),
                f.getAppointmentAt(), order.getDueAt(), f.getDropoffStatus(), f.getReturnStatus(),
                f.getPickupTrackingCode(), f.getDeliveryTrackingCode(), f.getPickupFee(), f.getDeliveryFee(),
                f.getPickupAreaName(), f.getPickupAddress(), f.getDeliveryAreaName(), f.getDeliveryAddress());
        return new CustomerPortalOrderResponse(order.getOrderCode(), order.getCreatedAt(), order.getChannel(),
                order.getOrderType(), fulfillment, f == null ? null : f.getCustomerRejectReason(),
                order.getReceivedAt(), order.getStatus(), order.getTotalAmount(), classified ? order.payableAmount() : null, classified, services, whitesFee,
                order.getPaymentStatus(), order.getPaymentClaimedAt());
    }
}
