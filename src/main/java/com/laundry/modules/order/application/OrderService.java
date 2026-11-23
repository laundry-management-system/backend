package com.laundry.modules.order.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.customer.application.CustomerService;
import com.laundry.modules.document.application.DocumentService;
import com.laundry.modules.document.application.dto.DocumentResponse;
import com.laundry.modules.document.domain.Document;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.ApplyOrderVoucherRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.CustomerTrackingResponse;
import com.laundry.modules.order.application.dto.EstimateReturnTimeRequest;
import com.laundry.modules.order.application.dto.EstimateReturnTimeResponse;
import com.laundry.modules.order.application.dto.IntakeOrderRequest;
import com.laundry.modules.order.application.dto.IntakeRequest;
import com.laundry.modules.order.application.dto.IntakeResponse;
import com.laundry.modules.order.application.dto.OrderDetailResponse;
import com.laundry.modules.order.application.dto.OrderFilter;
import com.laundry.modules.order.application.dto.OrderImageDto;
import com.laundry.modules.order.application.dto.OrderItemDto;
import com.laundry.modules.order.application.dto.OrderReceiptResponse;
import com.laundry.modules.order.application.dto.OrderResponse;
import com.laundry.modules.order.application.dto.QuickCreateOrderRequest;
import com.laundry.modules.order.application.dto.CompleteDropoffRequest;
import com.laundry.modules.order.application.dto.RejectOrderRequest;
import com.laundry.modules.order.application.dto.StartDeliveryRequest;
import com.laundry.modules.order.application.dto.UndoDeliveryRequest;
import com.laundry.modules.order.application.dto.UpdateTrackingCodesRequest;
import com.laundry.modules.order.application.dto.RescheduleAppointmentRequest;
import com.laundry.modules.order.application.dto.RequestedServiceDto;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.application.dto.WorkCountsResponse;
import com.laundry.modules.order.domain.AppointmentRules;
import com.laundry.modules.order.domain.HistoryActionType;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderIncidentQuery;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderImage;
import com.laundry.modules.order.domain.OrderImageRepository;
import com.laundry.modules.order.domain.OrderItem;
import com.laundry.modules.order.domain.OrderItemRepository;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ReturnMethod;
import com.laundry.modules.order.infrastructure.CustomerVoucherRateLimiter;
import com.laundry.modules.order.domain.ServiceType;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import com.laundry.modules.voucher.application.VoucherService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import com.laundry.modules.order.application.dto.CustomerOrderDetailResponse;
import com.laundry.modules.order.application.dto.CustomerPortalOrderResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderImageRepository orderImageRepository;
    private final CodeSequenceService codeSequenceService;
    private final CustomerService customerService;
    private final LaundryServiceRepository laundryServiceRepository;
    private final SettingService settingService;
    private final DocumentService documentService;
    private final VoucherService voucherService;
    private final OrderIncidentQuery orderIncidentQuery;
    private final AuditLogService auditLogService;
    private final CustomerVoucherRateLimiter customerVoucherRateLimiter;

    /**
     * Tạo một lượt nhận (1–2 đơn) trong một transaction. Dịch vụ chọn ở quầy chỉ lưu làm tham khảo:
     * đơn chưa có dòng đồ và chưa có giá; giá chốt lúc phân loại (PUT /items).
     */
    @Transactional
    public IntakeResponse createIntake(IntakeRequest request, String performedBy) {
        List<Order> existing = orderRepository.findByIntakeRequestId(request.requestId());
        if (!existing.isEmpty()) {
            return toIntakeResponse(existing, false);
        }

        Map<Long, LaundryService> services = loadIntakeServices(request.orders());
        Instant now = Instant.now();
        validateIntake(request.orders(), services, now);

        Long customerId = customerService.findOrCreateForIntake(
                request.customerPhone(), request.customerName(), performedBy);
        String intakeCode = codeSequenceService.getNextIntakeCode();

        // Điều khoản bồi thường chốt theo đơn lúc tạo, in trên phiếu nhận (BR-I04)
        int compensationMultiplier = settingService.getCompensationMultiplier();
        BigDecimal compensationCap = settingService.getCompensationCap();
        List<Order> created = request.orders().stream().map(o -> {
            String serviceNames = o.serviceIds().stream().map(id -> services.get(id).getName())
                    .collect(Collectors.joining(", "));
            Order order = Order.builder()
                    .orderCode(codeSequenceService.getNextOrderCode())
                    .compensationMultiplier(compensationMultiplier)
                    .compensationCap(compensationCap)
                    .intakeCode(intakeCode)
                    .intakeRequestId(request.requestId())
                    .customerId(customerId)
                    .customerName(request.customerName().trim())
                    .customerPhone(request.customerPhone().trim())
                    .createdBy(performedBy)
                    .orderType(o.orderType())
                    .requestedServiceIds(List.copyOf(o.serviceIds()))
                    .washWhitesSeparately(o.washWhitesSeparately())
                    .totalAmount(BigDecimal.ZERO)
                    .status(OrderStatus.RECEIVED)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .receivedAt(now)
                    .dueAt(o.dueAt())
                    .build();
            order.addHistory(HistoryActionType.CREATED, null, OrderStatus.RECEIVED.name(),
                    "Tiếp nhận tại quầy · lượt " + intakeCode + " · " + serviceNames, performedBy);
            return orderRepository.save(order);
        }).toList();

        log.info("Created intake [{}] with {} order(s) for [{}]", intakeCode, created.size(), request.customerPhone());
        return toIntakeResponse(created, true);
    }

    private Map<Long, LaundryService> loadIntakeServices(List<IntakeOrderRequest> orders) {
        Set<Long> ids = orders.stream().flatMap(o -> o.serviceIds().stream()).collect(Collectors.toSet());
        Map<Long, LaundryService> found = laundryServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LaundryService::getId, Function.identity()));
        for (Long id : ids) {
            if (!found.containsKey(id)) throw new NotFoundException("Không tìm thấy dịch vụ với ID: " + id);
        }
        return found;
    }

    private void validateIntake(List<IntakeOrderRequest> orders, Map<Long, LaundryService> services, Instant now) {
        if (orders.stream().map(IntakeOrderRequest::orderType).distinct().count() != orders.size()) {
            throw new BusinessException("Một lượt nhận chỉ có tối đa 1 đơn Tiêu chuẩn và 1 đơn Giặt nhanh", "orders");
        }
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < orders.size(); i++) {
            IntakeOrderRequest o = orders.get(i);
            String prefix = "orders[" + i + "].";
            boolean whitesAvailable = false;
            for (Long id : o.serviceIds()) {
                LaundryService svc = services.get(id);
                if (!seen.add(id)) {
                    throw new BusinessException("Dịch vụ " + svc.getName() + " bị chọn lặp", prefix + "serviceIds");
                }
                if (svc.getStatus() != ServiceStatus.ACTIVE) {
                    throw new BusinessException("Dịch vụ " + svc.getName() + " đã ngừng hoạt động", prefix + "serviceIds");
                }
                if (o.orderType() == OrderType.EXPRESS && !Boolean.TRUE.equals(svc.getExpressSupported())) {
                    throw new BusinessException("Dịch vụ " + svc.getName() + " không hỗ trợ giặt nhanh", prefix + "serviceIds");
                }
                whitesAvailable |= Boolean.TRUE.equals(svc.getWhitesApplicable());
            }
            if (o.washWhitesSeparately() && !whitesAvailable) {
                throw new BusinessException("Đơn không có dịch vụ áp dụng giặt riêng đồ trắng", prefix + "washWhitesSeparately");
            }
            if (!o.dueAt().isAfter(now)) {
                throw new BusinessException("Ngày hẹn trả phải sau thời điểm nhận", prefix + "dueAt");
            }
        }
    }

    private IntakeResponse toIntakeResponse(List<Order> orders, boolean created) {
        List<Order> sorted = orders.stream()
                .sorted(Comparator.comparing((Order o) -> o.getOrderType() != OrderType.NORMAL))
                .toList();
        Order first = sorted.getFirst();
        return new IntakeResponse(first.getIntakeCode(), first.getCustomerId(),
                sorted.stream().map(this::toDetail).toList(), created);
    }

    private OrderDetailResponse toDetail(Order order) {
        List<Long> ids = order.getRequestedServiceIds();
        if (ids == null || ids.isEmpty()) return withIncidents(order, List.of());
        Map<Long, LaundryService> byId = laundryServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LaundryService::getId, Function.identity()));
        List<RequestedServiceDto> requested = ids.stream().map(id -> {
            LaundryService svc = byId.get(id);
            return svc == null ? new RequestedServiceDto(id, null, null)
                    : new RequestedServiceDto(id, svc.getName(), ItemUnit.valueOf(svc.getUnit().name()));
        }).toList();
        return withIncidents(order, requested);
    }

    private OrderDetailResponse withIncidents(Order order, List<RequestedServiceDto> requested) {
        return OrderDetailResponse.from(order, requested, orderIncidentQuery.deductedCodes(order.getId()),
                order.effectiveMultiplier(settingService.getCompensationMultiplier()),
                order.effectiveCap(settingService.getCompensationCap()));
    }

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
                .compensationMultiplier(settingService.getCompensationMultiplier())
                .compensationCap(settingService.getCompensationCap())
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
        return toDetail(saved);
    }

    @Transactional
    public OrderReceiptResponse getReceipt(Long id, String webBaseUrl) {
        Order order = findOrderById(id);
        if (order.getStatus().isBeforeDropoff()) {
            throw new BusinessException(Order.BEFORE_DROPOFF_MESSAGE);
        }
        order.markReceiptPrinted();
        if (!order.isIntakeV3()) {
            return OrderReceiptResponse.from(order, webBaseUrl, 1, 1, order.getWhitesFee(), List.of(),
                    order.effectiveMultiplier(settingService.getCompensationMultiplier()),
                    order.effectiveCap(settingService.getCompensationCap()));
        }

        List<Order> intake = orderRepository.findByIntakeCode(order.getIntakeCode()).stream()
                .sorted(Comparator.comparing((Order o) -> o.getOrderType() != OrderType.NORMAL))
                .toList();
        int index = intake.indexOf(order) + 1;
        List<OrderReceiptResponse.ReceiptServiceLine> lines = laundryServiceRepository
                .findAllById(order.getRequestedServiceIds()).stream()
                .sorted(Comparator.comparingInt(s -> order.getRequestedServiceIds().indexOf(s.getId())))
                .map(s -> new OrderReceiptResponse.ReceiptServiceLine(s.getName(), ItemUnit.valueOf(s.getUnit().name()),
                        s.getCurrentPrice(),
                        order.getOrderType() == OrderType.EXPRESS ? s.getExpressSurcharge() : null,
                        s.getMinQuantity()))
                .toList();
        // Chưa phân loại thì phí đồ trắng chưa chốt: in mức hiện hành để tham khảo
        BigDecimal whitesFee = !Boolean.TRUE.equals(order.getWashWhitesSeparately()) ? BigDecimal.ZERO
                : order.getItems().isEmpty() ? settingService.getWhitesFee() : order.getWhitesFee();
        return OrderReceiptResponse.from(order, webBaseUrl, Math.max(index, 1), Math.max(intake.size(), 1), whitesFee, lines,
                order.effectiveMultiplier(settingService.getCompensationMultiplier()),
                order.effectiveCap(settingService.getCompensationCap()));
    }

    @Transactional
    public OrderDetailResponse updateItems(Long id, UpdateOrderItemsRequest request, String performedBy) {
        Order order = findOrderById(id);
        if (order.isIntakeV3()) {
            return classifyIntakeOrder(order, request, performedBy);
        }
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
        return toDetail(saved);
    }

    /**
     * Phân loại đơn v3 = bước chốt giá: lấy đơn giá, phụ phí nhanh, tối thiểu hiện hành của dịch vụ và phụ phí
     * đồ trắng hiện hành; bỏ qua mọi giá client gửi; BE tính lại tổng.
     */
    private OrderDetailResponse classifyIntakeOrder(Order order, UpdateOrderItemsRequest request, String performedBy) {
        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new BusinessException("Chỉ phân loại được đơn đang ở trạng thái Mới nhận");
        }
        List<OrderItemDto> lines = request.items();
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            Long serviceId = lines.get(i).serviceId();
            if (serviceId == null) throw new BusinessException("Chọn dịch vụ cho dòng đồ", "items[" + i + "].serviceId");
            if (!ids.add(serviceId)) throw new BusinessException("Mỗi dịch vụ chỉ một dòng", "items[" + i + "].serviceId");
        }
        Map<Long, LaundryService> services = laundryServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LaundryService::getId, Function.identity()));
        boolean express = order.getOrderType() == OrderType.EXPRESS;

        List<OrderItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            OrderItemDto dto = lines.get(i);
            String field = "items[" + i + "].";
            LaundryService svc = services.get(dto.serviceId());
            if (svc == null) throw new NotFoundException("Không tìm thấy dịch vụ với ID: " + dto.serviceId());
            if (svc.getStatus() != ServiceStatus.ACTIVE) {
                throw new BusinessException("Dịch vụ " + svc.getName() + " đã ngừng hoạt động", field + "serviceId");
            }
            if (express && !Boolean.TRUE.equals(svc.getExpressSupported())) {
                throw new BusinessException("Dịch vụ " + svc.getName() + " không hỗ trợ giặt nhanh", field + "serviceId");
            }
            BigDecimal qty = dto.quantity();
            int maxScale = svc.getUnit() == com.laundry.modules.service.domain.ServiceUnit.KG ? 2 : 0;
            if (qty == null || qty.signum() <= 0 || qty.stripTrailingZeros().scale() > maxScale) {
                throw new BusinessException(maxScale == 0 ? "Số lượng phải là số nguyên dương" : "Khối lượng phải > 0, tối đa 2 chữ số thập phân",
                        field + "quantity");
            }
            OrderItem item = OrderItem.builder()
                    .serviceId(svc.getId())
                    .itemType(svc.getName())
                    .unit(ItemUnit.valueOf(svc.getUnit().name()))
                    .unitPrice(svc.getCurrentPrice())
                    .expressSurcharge(express && svc.getExpressSurcharge() != null ? svc.getExpressSurcharge() : BigDecimal.ZERO)
                    .minQuantity(svc.getMinQuantity())
                    .conditionNote(dto.conditionNote())
                    .build();
            item.bill(qty);
            items.add(item);
        }

        order.updateItems(items, performedBy);
        boolean whitesLine = services.values().stream().anyMatch(s -> Boolean.TRUE.equals(s.getWhitesApplicable()));
        order.setWhitesFee(Boolean.TRUE.equals(order.getWashWhitesSeparately()) && whitesLine ? settingService.getWhitesFee() : BigDecimal.ZERO);
        order.recalcTotals(request.discountAmount());
        return toDetail(orderRepository.saveAndFlush(order));
    }

    /** Voucher chỉ áp ở bước thanh toán, tính trên tiền hàng; giữ / đổi / trả lượt theo voucher đơn đang giữ. */
    @Transactional
    public OrderDetailResponse applyVoucher(Long id, ApplyOrderVoucherRequest request, String performedBy) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng với ID: " + id));
        if (isOnlineDelivery(order)) {
            throw new BusinessException("Voucher của đơn giao tận nơi chỉ do khách áp dụng");
        }
        String code = request.voucherCode();
        if (code == null || code.isBlank()) {
            if (order.getVoucherId() == null) return toDetail(order);
            voucherService.releaseVoucher(order.getVoucherId());
            order.applyVoucher(null, null, BigDecimal.ZERO, performedBy);
        } else {
            VoucherService.AppliedVoucher applied = voucherService.applyToOrder(code, order.getSubtotal(), order.getVoucherId());
            order.applyVoucher(applied.voucherId(), applied.code(), applied.discountAmount(), performedBy);
        }
        return toDetail(orderRepository.saveAndFlush(order));
    }

    /** Khách áp / đổi voucher của chính mình cho đơn ONLINE giao tận nơi trước khi báo đã thanh toán (FR-043..FR-046). */
    @Transactional
    public CustomerOrderDetailResponse applyCustomerVoucher(Long customerId, String clientIp, String orderCode, String code) {
        customerVoucherRateLimiter.check(customerId, clientIp);
        Order order = lockCustomerPayable(customerId, orderCode);
        if (!order.voucherEditableByCustomer()) {
            throw new BusinessException("Voucher đã khoá, không thể đổi");
        }
        VoucherService.AppliedVoucher applied = voucherService.applyToOrder(code, order.getSubtotal(), order.getVoucherId());
        order.applyVoucher(applied.voucherId(), applied.code(), applied.discountAmount(),
                customerService.getCustomerById(customerId).customerCode());
        order.setVoucherByCustomer(true);
        return toCustomerDetail(orderRepository.saveAndFlush(order));
    }

    /** Khách gỡ voucher của chính mình, hoàn lượt dùng (FR-044). */
    @Transactional
    public CustomerOrderDetailResponse removeCustomerVoucher(Long customerId, String clientIp, String orderCode) {
        customerVoucherRateLimiter.check(customerId, clientIp);
        Order order = lockCustomerPayable(customerId, orderCode);
        if (!order.voucherEditableByCustomer()) {
            throw new BusinessException("Voucher đã khoá, không thể gỡ");
        }
        if (order.getVoucherId() == null) {
            throw new BusinessException("Đơn chưa có voucher");
        }
        voucherService.releaseVoucher(order.getVoucherId());
        order.applyVoucher(null, null, BigDecimal.ZERO, customerService.getCustomerById(customerId).customerCode());
        order.setVoucherByCustomer(null);
        return toCustomerDetail(orderRepository.saveAndFlush(order));
    }

    /** Khách báo đã chuyển khoản; không giới hạn tần suất vì idempotent (FR-047). */
    @Transactional
    public CustomerOrderDetailResponse claimCustomerPayment(Long customerId, String orderCode) {
        Order order = lockOwnOrder(customerId, orderCode);
        order.claimPayment(customerService.getCustomerById(customerId).customerCode());
        return toCustomerDetail(orderRepository.saveAndFlush(order));
    }

    /** Khách xác nhận đã nhận đơn giao tận nơi; idempotent nên không giới hạn tần suất (FR-057, FR-058). */
    @Transactional
    public CustomerOrderDetailResponse confirmCustomerReceived(Long customerId, String orderCode) {
        Order order = lockOwnOrder(customerId, orderCode);
        order.confirmReceived(customerService.getCustomerById(customerId).customerCode());
        return toCustomerDetail(orderRepository.saveAndFlush(order));
    }

    /** Nhân viên từ chối báo thanh toán kèm lý do (FR-048). */
    @Transactional
    public OrderDetailResponse rejectPaymentClaim(Long orderId, String reason, String staff) {
        Order order = lockOrder(orderId);
        order.rejectPaymentClaim(reason, staff);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    private Order lockOwnOrder(Long customerId, String orderCode) {
        Long id = orderRepository.findByOrderCodeAndCustomerId(orderCode, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng")).getId();
        return lockOrder(id);
    }

    private Order lockCustomerPayable(Long customerId, String orderCode) {
        Order order = lockOwnOrder(customerId, orderCode);
        order.requireCustomerPayable();
        return order;
    }

    private static boolean isOnlineDelivery(Order order) {
        return order.getChannel() == OrderChannel.ONLINE && order.getFulfillment() != null
                && order.getFulfillment().getReturnMethod() == ReturnMethod.DELIVERY;
    }

    @Transactional
    public OrderImageDto addOrderItemImage(Long orderId, Long itemId, AddOrderItemImageRequest request) {
        findOrderById(orderId);
        OrderItem item = orderItemRepository.findById(itemId)
                .filter(i -> i.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dòng món đồ với ID: " + itemId));

        Document document = request.documentId() != null ? documentService.requireImage(request.documentId()) : null;
        String imageUrl = document != null ? DocumentResponse.urlOf(document)
                : request.imageUrl() != null && !request.imageUrl().isBlank() ? request.imageUrl().trim() : null;
        if (imageUrl == null) {
            throw new BusinessException("Vui lòng chọn ảnh", "documentId");
        }

        OrderImage image = OrderImage.builder()
                .orderItem(item)
                .document(document)
                .imageUrl(imageUrl)
                .note(request.note())
                .build();

        OrderImage saved = orderImageRepository.save(image);
        return OrderImageDto.from(saved);
    }

    @Transactional
    public OrderDetailResponse changeStatus(Long id, ChangeStatusRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.changeStatus(request.newStatus(), request.note(), performedBy);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDetailResponse completeOrder(Long id, CompleteOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.complete(request.shelfLocation(), performedBy);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDetailResponse revertStatus(Long id, RevertStatusRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.revertStatus(request.targetStatus(), request.reason(), performedBy);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDetailResponse reschedule(Long id, RescheduleOrderRequest request, String performedBy) {
        Order order = findOrderById(id);
        Instant targetDueAt = request.getTargetDueAt();
        if (targetDueAt == null) {
            throw new BusinessException("Thời gian hẹn trả mới không được để trống");
        }
        order.reschedule(targetDueAt, request.reason(), performedBy);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDetailResponse adjustPrice(Long id, AdjustPriceRequest request, String performedBy) {
        Order order = findOrderById(id);
        order.adjustPrice(request.newTotalAmount(), request.reason(), performedBy);
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDetailResponse cancel(Long id, CancelOrderRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.cancel(request.customerReason(), request.reason(), performedBy);
        voucherService.releaseVoucher(order.getVoucherId());
        Order saved = orderRepository.saveAndFlush(order);
        if (saved.getChannel() == OrderChannel.ONLINE) {
            audit(AuditAction.ORDER_REJECT, saved, old, request.reason());
        }
        return toDetail(saved);
    }

    @Transactional
    public OrderDetailResponse confirm(Long id, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.confirm(performedBy);
        return toDetail(audited(AuditAction.ORDER_CONFIRM, order, old, null));
    }

    @Transactional
    public OrderDetailResponse reject(Long id, RejectOrderRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.reject(request.customerReason(), request.reason(), performedBy);
        voucherService.releaseVoucher(order.getVoucherId());
        return toDetail(audited(AuditAction.ORDER_REJECT, order, old, request.reason()));
    }

    @Transactional
    public OrderDetailResponse completeDropoff(Long id, CompleteDropoffRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.completeDropoff(request.pickupTrackingCode(), performedBy);
        return toDetail(audited(AuditAction.ORDER_FULFILLMENT_UPDATE, order, old, null));
    }

    @Transactional
    public OrderDetailResponse startDelivery(Long id, StartDeliveryRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.startDelivery(request.deliveryTrackingCode(), performedBy);
        return toDetail(audited(AuditAction.ORDER_FULFILLMENT_UPDATE, order, old, null));
    }

    @Transactional
    public OrderDetailResponse undoStartDelivery(Long id, UndoDeliveryRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.undoStartDelivery(request.reason(), performedBy);
        return toDetail(audited(AuditAction.ORDER_FULFILLMENT_UPDATE, order, old, request.reason()));
    }

    @Transactional
    public OrderDetailResponse updateTrackingCodes(Long id, UpdateTrackingCodesRequest request, String performedBy) {
        Order order = lockOrder(id);
        Map<String, Object> old = auditSnapshot(order);
        order.updateTrackingCodes(request.pickupTrackingCode(), request.deliveryTrackingCode(), performedBy);
        return toDetail(audited(AuditAction.ORDER_FULFILLMENT_UPDATE, order, old, null));
    }

    @Transactional
    public OrderDetailResponse rescheduleAppointment(Long id, RescheduleAppointmentRequest request, String performedBy) {
        Order order = lockOrder(id);
        if (order.getFulfillment() != null && order.getStatus().isBeforeDropoff()) {
            Instant now = Instant.now();
            LocalTime open = settingService.getValue(SettingKey.OPENING_TIME, LocalTime.class);
            LocalTime close = settingService.getValue(SettingKey.CLOSING_TIME, LocalTime.class);
            Instant dropoffAt = request.appointmentAt() != null
                    ? request.appointmentAt() : order.getFulfillment().getAppointmentAt();
            String error = null;
            String field = "appointmentAt";
            if (request.appointmentAt() != null) {
                error = AppointmentRules.appointmentError(request.appointmentAt(), now, open, close,
                        settingService.getValue(SettingKey.BOOKING_MIN_LEAD_HOURS, Integer.class));
            }
            if (error == null && (request.returnAt() != null || request.appointmentAt() != null)) {
                field = "returnAt";
                SettingKey gap = order.getOrderType() == OrderType.EXPRESS
                        ? SettingKey.BOOKING_RETURN_GAP_HOURS_EXPRESS : SettingKey.BOOKING_RETURN_GAP_HOURS_NORMAL;
                error = AppointmentRules.returnError(
                        request.returnAt() != null ? request.returnAt() : order.getDueAt(), dropoffAt, open, close,
                        settingService.getValue(gap, Integer.class));
            }
            if (error != null) {
                throw new BusinessException(error, field);
            }
        }
        Map<String, Object> old = auditSnapshot(order);
        order.rescheduleAppointment(request.appointmentAt(), request.returnAt(), performedBy);
        return toDetail(audited(AuditAction.ORDER_FULFILLMENT_UPDATE, order, old, null));
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
    public Page<OrderResponse> getOrders(OrderFilter f, Pageable pageable) {
        List<OrderStatus> statuses = f.statuses().isEmpty() ? List.of(OrderStatus.values()) : f.statuses();
        Pageable sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), OrderRepository.DEFAULT_SORT);
        return orderRepository.searchOrders(f.status(), statuses, f.channel(), f.priority(), f.orderType(), f.classified(),
                        f.late(), Instant.now(), f.dueStart(), f.dueEnd(), f.phone(), f.search(), f.intakeCode(), sorted)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CustomerPortalOrderResponse> listForCustomer(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Order> orders = orderRepository.findByCustomerId(customerId, pageable);
        Set<Long> ids = orders.stream().flatMap(o -> CustomerPortalOrderResponse.serviceIds(o).stream())
                .collect(Collectors.toSet());
        Map<Long, String> names = ids.isEmpty() ? Map.of() : laundryServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LaundryService::getId, LaundryService::getName));
        return orders.map(o -> CustomerPortalOrderResponse.from(o, names));
    }

    /** Đơn không thuộc khách và mã không tồn tại cùng 404 (FR-041). */
    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse getForCustomer(Long customerId, String orderCode) {
        Order order = orderRepository.findByOrderCodeAndCustomerId(orderCode, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        return toCustomerDetail(order);
    }

    private CustomerOrderDetailResponse toCustomerDetail(Order order) {
        Set<Long> ids = order.getRequestedServiceIds() == null ? Set.of() : order.getRequestedServiceIds().stream()
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = ids.isEmpty() ? Map.of() : laundryServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LaundryService::getId, LaundryService::getName));
        return CustomerOrderDetailResponse.from(order, names);
    }

    @Transactional(readOnly = true)
    public WorkCountsResponse getWorkCounts(OrderFilter f) {
        return WorkCountsResponse.from(orderRepository.countWork(
                Instant.now(), f.channel(), f.dueStart(), f.dueEnd(), f.phone(), f.search()));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long id) {
        return toDetail(findOrderById(id));
    }

    private Order audited(AuditAction action, Order order, Map<String, Object> old, String reason) {
        Order saved = orderRepository.saveAndFlush(order);
        audit(action, saved, old, reason);
        return saved;
    }

    private void audit(AuditAction action, Order order, Map<String, Object> old, String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.ORDER, order.getId(), old, auditSnapshot(order), reason));
    }

    private Map<String, Object> auditSnapshot(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderCode", order.getOrderCode());
        m.put("status", order.getStatus().name());
        m.put("dueAt", order.getDueAt());
        var f = order.getFulfillment();
        if (f != null) {
            m.put("appointmentAt", f.getAppointmentAt());
            m.put("dropoffStatus", f.getDropoffStatus());
            m.put("returnStatus", f.getReturnStatus());
            m.put("pickupTrackingCode", f.getPickupTrackingCode());
            m.put("deliveryTrackingCode", f.getDeliveryTrackingCode());
        }
        return m;
    }

    private Order lockOrder(Long id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng với ID: " + id));
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
