package com.laundry.modules.order.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.CustomerTrackingResponse;
import com.laundry.modules.order.application.dto.DeliverOrderRequest;
import com.laundry.modules.order.application.dto.EstimateReturnTimeRequest;
import com.laundry.modules.order.application.dto.EstimateReturnTimeResponse;
import com.laundry.modules.order.application.dto.OrderDetailResponse;
import com.laundry.modules.order.application.dto.OrderImageDto;
import com.laundry.modules.order.application.dto.OrderReceiptResponse;
import com.laundry.modules.order.application.dto.OrderResponse;
import com.laundry.modules.order.application.dto.QuickCreateOrderRequest;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.domain.HistoryActionType;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderImage;
import com.laundry.modules.order.domain.OrderImageRepository;
import com.laundry.modules.order.domain.OrderItem;
import com.laundry.modules.order.domain.OrderItemRepository;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderImageRepository orderImageRepository;
    private final CodeSequenceService codeSequenceService;

    @Transactional
    public OrderDetailResponse quickCreate(QuickCreateOrderRequest request, String performedBy) {
        Instant now = Instant.now();
        ServiceType serviceType = request.serviceType();
        OrderPriority priority = request.priority() != null ? request.priority() : OrderPriority.NORMAL;

        OrderType orderType = request.orderType() != null
                ? request.orderType()
                : (priority == OrderPriority.URGENT ? OrderType.EXPRESS : OrderType.NORMAL);

        BigDecimal unitPrice = request.unitPrice() != null && request.unitPrice().compareTo(BigDecimal.ZERO) > 0
                ? request.unitPrice()
                : serviceType.getDefaultPricePerKg();

        BigDecimal subtotal = request.totalWeight()
                .multiply(unitPrice)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal expressFee = (orderType == OrderType.EXPRESS)
                ? request.totalWeight().multiply(BigDecimal.valueOf(10000)).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.add(expressFee);

        Instant dueAt = request.dueAt();
        if (dueAt == null) {
            dueAt = calculateDueAt(serviceType, orderType, priority, now);
        }

        String orderCode = codeSequenceService.getNextOrderCode();
        int bagCount = request.bagCount() != null && request.bagCount() > 0 ? request.bagCount() : 1;

        Order order = Order.builder()
                .orderCode(orderCode)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .createdBy(performedBy)
                .serviceType(serviceType)
                .totalWeight(request.totalWeight())
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .expressFee(expressFee)
                .totalAmount(totalAmount)
                .note(request.customerNote())
                .priority(priority)
                .orderType(orderType)
                .status(OrderStatus.RECEIVED)
                .paymentStatus(PaymentStatus.UNPAID)
                .bagCount(bagCount)
                .receivedAt(now)
                .dueAt(dueAt)
                .build();

        order.addHistory(
                HistoryActionType.CREATED,
                null,
                OrderStatus.RECEIVED.name(),
                "Tiếp nhận tại quầy (" + request.totalWeight() + " kg - " + serviceType.getDisplayName() + ")",
                performedBy
        );

        Order saved = orderRepository.save(order);
        log.info("Created order [{}] for [{}] with amount [{}]", saved.getOrderCode(), saved.getCustomerName(), saved.getTotalAmount());
        return OrderDetailResponse.from(saved);
    }

    @Transactional
    public OrderReceiptResponse getReceipt(Long id, String webBaseUrl) {
        Order order = findOrderById(id);
        order.markReceiptPrinted();
        return OrderReceiptResponse.from(order, webBaseUrl);
    }

    @Transactional
    public OrderDetailResponse updateItems(Long id, UpdateOrderItemsRequest request, String performedBy) {
        Order order = findOrderById(id);
        List<OrderItem> items = request.items().stream()
                .map(dto -> {
                    BigDecimal qty = dto.quantity() != null ? dto.quantity() : BigDecimal.ONE;
                    BigDecimal price = dto.unitPrice() != null ? dto.unitPrice() : BigDecimal.ZERO;
                    BigDecimal surcharge = dto.expressSurcharge() != null ? dto.expressSurcharge() : BigDecimal.ZERO;
                    BigDecimal lineTot = dto.lineTotal() != null ? dto.lineTotal() : qty.multiply(price.add(surcharge)).setScale(0, RoundingMode.HALF_UP);

                    return OrderItem.builder()
                            .itemType(dto.itemType().trim())
                            .quantity(qty)
                            .unit(dto.unit() != null ? dto.unit() : ItemUnit.PIECE)
                            .unitPrice(price)
                            .expressSurcharge(surcharge)
                            .lineTotal(lineTot)
                            .conditionNote(dto.conditionNote())
                            .build();
                })
                .toList();

        order.updateItems(items, performedBy);
        Order saved = orderRepository.saveAndFlush(order);
        return OrderDetailResponse.from(saved);
    }

    @Transactional
    public OrderImageDto addOrderItemImage(Long orderId, Long itemId, AddOrderItemImageRequest request) {
        findOrderById(orderId);
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dòng món đồ với ID: " + itemId));

        OrderImage image = OrderImage.builder()
                .orderItem(item)
                .imageUrl(request.imageUrl().trim())
                .note(request.note())
                .build();

        OrderImage saved = orderImageRepository.save(image);
        return OrderImageDto.from(saved);
    }

    @Transactional
    public OrderDetailResponse changeStatus(Long id, ChangeStatusRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.changeStatus(request.newStatus(), request.note(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse completeOrder(Long id, CompleteOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.complete(request.shelfLocation(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse deliverOrder(Long id, DeliverOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.deliver(request.receiverName(), request.receiverRelation(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse revertStatus(Long id, RevertStatusRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.revertStatus(request.targetStatus(), request.reason(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse reschedule(Long id, RescheduleOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        Instant targetDueAt = request.getTargetDueAt();
        if (targetDueAt == null) {
            throw new BusinessException("Thời gian hẹn trả mới không được để trống");
        }
        order.reschedule(targetDueAt, request.reason(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse adjustPrice(Long id, AdjustPriceRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.adjustPrice(request.newTotalAmount(), request.reason(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDetailResponse cancel(Long id, CancelOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.cancel(request.reason(), performedBy);
        return OrderDetailResponse.from(order);
    }

    @Transactional(readOnly = true)
    public EstimateReturnTimeResponse estimateReturnTime(EstimateReturnTimeRequest request) {
        Instant now = Instant.now();
        OrderPriority priority = request.priority() != null ? request.priority() : OrderPriority.NORMAL;
        OrderType orderType = (priority == OrderPriority.URGENT) ? OrderType.EXPRESS : OrderType.NORMAL;
        Instant dueTime = calculateDueAt(request.serviceType(), orderType, priority, now);
        int hours = (int) Duration.between(now, dueTime).toHours();

        String explanation = String.format("Dịch vụ '%s' (%s) dự kiến xử lý trong khoảng %d giờ",
                request.serviceType().getDisplayName(), orderType.getDescription(), hours);

        return new EstimateReturnTimeResponse(dueTime, hours, explanation);
    }

    @Transactional(readOnly = true)
    public List<CustomerTrackingResponse> trackByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }
        List<Order> orders = orderRepository.findByCustomerPhoneOrderByCreatedAtDesc(phone.trim());
        return orders.stream().map(CustomerTrackingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, OrderPriority priority, String phone, String search, Pageable pageable) {
        String cleanPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return orderRepository.searchOrders(status, priority, cleanPhone, cleanSearch, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long id) {
        return OrderDetailResponse.from(findOrderById(id));
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng với ID: " + id));
    }

    private Instant calculateDueAt(ServiceType serviceType, OrderType orderType, OrderPriority priority, Instant baseTime) {
        int hours = serviceType.getStandardHours();
        if (orderType == OrderType.EXPRESS || priority == OrderPriority.URGENT) {
            hours = Math.max(2, hours / 2);
        } else if (priority == OrderPriority.HIGH) {
            hours = Math.max(4, (int) (hours * 0.75));
        }
        return baseTime.plus(Duration.ofHours(hours));
    }
}
