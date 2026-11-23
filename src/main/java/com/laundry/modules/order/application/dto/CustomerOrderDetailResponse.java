package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.DropoffStatus;
import com.laundry.modules.order.domain.HistoryActionType;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderStatusHistory;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ReturnMethod;
import com.laundry.modules.order.domain.ReturnStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chi tiết đơn cho khách (contracts §8, FR-040). Danh sách trắng theo cấu trúc: không có người thao tác,
 * lý do lịch sử, ghi chú nội bộ, ghi chú tình trạng tự do, ghi chú ảnh hay chi tiết sự cố.
 */
public record CustomerOrderDetailResponse(
        String orderCode, OrderChannel channel, OrderType orderType, OrderStatus status,
        PaymentStatus paymentStatus, boolean classified, String customerRejectReason,
        Milestones milestones, Instant dueAt, Fulfillment fulfillment,
        List<RequestedService> requestedServices, boolean washWhitesSeparately, List<Item> items, Money money,
        Instant paymentClaimedAt, String paymentClaimRejectedReason, Instant paymentClaimRejectedAt,
        boolean voucherEditable, boolean receiptConfirmable) {

    public record Milestones(Instant placedAt, Instant receivedAt, Instant processingAt, Instant completedAt,
                             Instant deliveringAt, Instant deliveredAt) {}

    /** Null với đơn OFFLINE. */
    public record Fulfillment(DropoffMethod dropoffMethod, DropoffStatus dropoffStatus, Instant appointmentAt,
                              String pickupAreaName, String pickupAddress, String pickupTrackingCode, BigDecimal pickupFee,
                              ReturnMethod returnMethod, ReturnStatus returnStatus,
                              String deliveryAreaName, String deliveryAddress, String deliveryTrackingCode,
                              BigDecimal deliveryFee) {}

    public record RequestedService(String serviceName) {}

    public record Image(String url) {}

    public record Item(String serviceName, ItemUnit unit, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal lineTotal, boolean extra, List<String> conditionTags, List<Image> images) {}

    public record Money(BigDecimal subtotal, BigDecimal expressFee, BigDecimal whitesFee, BigDecimal discountAmount,
                        String voucherCode, BigDecimal incidentDiscount, BigDecimal totalAmount, BigDecimal payableAmount) {}

    // Keep in sync with frontend features/admin/orders/domain/types.ts CONDITION_TAGS.
    static final Set<String> CONDITION_TAGS = Set.of("Vết bẩn", "Bị rách", "Dễ phai màu", "Cần giặt riêng", "Khác");
    private static final Pattern TAG_PREFIX = Pattern.compile("^\\[([^\\]]*)\\]");

    /** {@code serviceNames}: id -> tên, cùng quy tắc với {@link CustomerPortalOrderResponse#serviceIds}. */
    public static CustomerOrderDetailResponse from(Order order, Map<Long, String> serviceNames) {
        boolean classified = !order.getItems().isEmpty();
        OrderFulfillment f = order.getFulfillment();
        Fulfillment fulfillment = f == null ? null : new Fulfillment(f.getDropoffMethod(), f.getDropoffStatus(),
                f.getAppointmentAt(), f.getPickupAreaName(), f.getPickupAddress(), f.getPickupTrackingCode(),
                f.getPickupFee(), f.getReturnMethod(), f.getReturnStatus(), f.getDeliveryAreaName(),
                f.getDeliveryAddress(), f.getDeliveryTrackingCode(), f.getDeliveryFee());
        Milestones milestones = new Milestones(order.getCreatedAt(), order.getReceivedAt(),
                firstAt(order, HistoryActionType.STATUS_CHANGE, OrderStatus.PROCESSING.name()), order.getCompletedAt(),
                firstAt(order, HistoryActionType.RETURN_STATUS_CHANGE, ReturnStatus.DELIVERING.name()),
                order.getDeliveredAt());
        List<Long> requestedIds = order.getRequestedServiceIds() == null ? List.of() : order.getRequestedServiceIds();
        List<RequestedService> requested = requestedIds.stream().filter(serviceNames::containsKey)
                .map(id -> new RequestedService(serviceNames.get(id))).toList();
        List<Item> items = order.getItems().stream().map(it -> new Item(
                it.getItemType(), it.getUnit(),
                it.getBillableQuantity() != null ? it.getBillableQuantity() : it.getQuantity(),
                it.getUnitPrice(), it.getLineTotal(), order.isExtraService(it.getServiceId()),
                conditionTags(it.getConditionNote()),
                it.getImages().stream().map(img -> new Image(img.getImageUrl())).toList())).toList();
        BigDecimal whitesFee = classified && order.getWhitesFee() != null ? order.getWhitesFee() : BigDecimal.ZERO;
        Money money = new Money(order.getSubtotal(), order.getExpressFee(), whitesFee, order.getDiscountAmount(),
                order.getVoucherCode(), order.getIncidentDiscount(), order.getTotalAmount(), order.payableAmount());
        return new CustomerOrderDetailResponse(order.getOrderCode(), order.getChannel(), order.getOrderType(),
                order.getStatus(), order.getPaymentStatus(), classified,
                f == null ? null : f.getCustomerRejectReason(), milestones, order.getDueAt(), fulfillment,
                requested, Boolean.TRUE.equals(order.getWashWhitesSeparately()), items, money,
                order.getPaymentClaimedAt(), order.getPaymentClaimRejectedReason(), order.getPaymentClaimRejectedAt(),
                order.voucherEditableByCustomer(), order.receiptConfirmable());
    }

    /** Thời điểm sớm nhất của mục lịch sử khớp (chỉ thời điểm, không người thao tác / lý do). */
    private static Instant firstAt(Order order, HistoryActionType type, String newValue) {
        return order.getHistoryList().stream()
                .filter(h -> h.getActionType() == type && newValue.equals(h.getNewValue()) && h.getCreatedAt() != null)
                .map(OrderStatusHistory::getCreatedAt).min(Comparator.naturalOrder()).orElse(null);
    }

    /** Tag trong ngoặc vuông đầu ghi chú, chỉ khi mọi phần đều là tag đã biết (như FE parseConditionNote). */
    static List<String> conditionTags(String note) {
        if (note == null) return List.of();
        Matcher m = TAG_PREFIX.matcher(note);
        if (!m.find()) return List.of();
        List<String> parts = Arrays.stream(m.group(1).split(",")).map(String::trim).toList();
        return parts.stream().allMatch(CONDITION_TAGS::contains) ? parts : List.of();
    }
}
