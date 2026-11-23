package com.laundry.modules.booking.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.booking.application.dto.CreateBookingIntakeRequest;
import com.laundry.modules.booking.application.dto.OnlineBookingResponse;
import com.laundry.modules.customer.application.IdentifierNormalizer;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.order.application.CodeSequenceService;
import com.laundry.modules.order.domain.AppointmentRules;
import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.HistoryActionType;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderFulfillment;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.order.domain.ReturnMethod;
import com.laundry.modules.service.application.WashModeService;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.WashMode;
import com.laundry.modules.service.domain.WashModeRepository;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.ServiceArea;
import com.laundry.modules.setting.domain.ServiceAreaRepository;
import com.laundry.modules.setting.domain.SettingKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Khách đặt online: mỗi lượt tạo đúng một đơn ONLINE "Chờ xác nhận" theo hình thức giặt khách chọn. */
@Service
@RequiredArgsConstructor
public class OnlineBookingService {

    private static final String DELIVERY_UNAVAILABLE = "Tiệm hiện chưa nhận lấy và giao tận nơi";
    private static final String OUTSIDE_AREA =
            "Địa chỉ ngoài khu vực giao nhận. Chọn Mang đến tiệm và Nhận tại tiệm, hoặc gọi tiệm để được tư vấn.";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final LaundryServiceRepository laundryServiceRepository;
    private final SettingService settingService;
    private final CodeSequenceService codeSequenceService;
    private final AuditLogService auditLogService;
    private final ServiceAreaRepository serviceAreaRepository;
    private final WashModeService washModeService;
    private final WashModeRepository washModeRepository;

    /** Gửi lại cùng requestId trả về đơn đã có (created=false), không ghi đè. */
    @Transactional
    public OnlineBookingResponse createIntake(Long customerId, CreateBookingIntakeRequest req) {
        String storePhone = settingService.getValue(SettingKey.STORE_PHONE, String.class);
        LocalTime open = settingService.getValue(SettingKey.OPENING_TIME, LocalTime.class);
        LocalTime close = settingService.getValue(SettingKey.CLOSING_TIME, LocalTime.class);
        if (!settingService.getValue(SettingKey.BOOKING_ENABLED, Boolean.class)) {
            throw new BusinessException("Tiệm tạm ngừng nhận đặt đơn online. Anh/chị vui lòng "
                    + (storePhone.isBlank() ? "" : "gọi " + storePhone + " hoặc ")
                    + "mang đồ đến tiệm trong giờ mở cửa " + open + " – " + close + ".");
        }
        // Khóa hàng khách trước hàng code_sequences: giữ giới hạn đơn chưa kết thúc và chống gửi lại đồng thời
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn."));

        List<Order> earlier = orderRepository.findByIntakeRequestId(req.requestId()).stream()
                .filter(o -> customer.getId().equals(o.getCustomerId()))
                .toList();
        if (!earlier.isEmpty()) {
            return response(earlier.get(0), false);
        }

        Instant now = Instant.now();
        Map<String, String> errors = new LinkedHashMap<>();

        List<LaundryService> services = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < req.items().size(); i++) {
            CreateBookingIntakeRequest.Item line = req.items().get(i);
            String key = "items[" + i + "]";
            Optional<LaundryService> found = laundryServiceRepository.findById(line.serviceId());
            if (found.isEmpty()) {
                errors.put(key + ".serviceId", "Dịch vụ không còn nhận");
                continue;
            }
            LaundryService svc = found.get();
            if (!seen.add(line.serviceId())) {
                errors.put(key + ".serviceId", "Dịch vụ " + svc.getName() + " bị chọn trùng");
                continue;
            }
            if (svc.getStatus() != ServiceStatus.ACTIVE) {
                errors.put(key + ".serviceId", "Dịch vụ " + svc.getName() + " hiện không nhận đặt");
                continue;
            }
            services.add(svc);
        }
        // Hình thức luôn được kiểm; giá từng dịch vụ chỉ khi các dòng đều hợp lệ (chỉ số items[i] khớp)
        List<Long> serviceIds = services.stream().map(LaundryService::getId).toList();
        Map<Long, BigDecimal> prices = Map.of();
        WashMode mode = null;
        try {
            prices = washModeService.requirePrices(req.washModeId(),
                    errors.isEmpty() ? serviceIds : List.of(), false);
            mode = washModeRepository.findById(req.washModeId()).orElseThrow();
        } catch (BusinessException e) {
            errors.put(e.getField(), e.getMessage());
        }
        boolean whites = Boolean.TRUE.equals(req.washWhitesSeparately());
        if (whites && services.stream().noneMatch(s -> Boolean.TRUE.equals(s.getWhitesApplicable()))) {
            errors.put("washWhitesSeparately", "Đơn không có dịch vụ áp dụng giặt riêng đồ trắng");
        }

        boolean pickup = req.dropoffMethod() == DropoffMethod.PICKUP;
        boolean delivery = req.returnMethod() == ReturnMethod.DELIVERY;
        ServiceArea pickupArea = null;
        ServiceArea deliveryArea = null;
        String pickupAddress = null;
        String deliveryAddress = null;
        if ((pickup || delivery) && !settingService.getValue(SettingKey.BOOKING_DELIVERY_ENABLED, Boolean.class)) {
            if (pickup) errors.put("dropoffMethod", DELIVERY_UNAVAILABLE);
            if (delivery) errors.put("returnMethod", DELIVERY_UNAVAILABLE);
        } else if (pickup || delivery) {
            if (pickup) {
                if (req.pickupAreaId() == null) {
                    errors.put("pickupAreaId", "Chọn khu vực lấy đồ");
                } else {
                    pickupArea = serviceAreaRepository.findById(req.pickupAreaId()).filter(ServiceArea::isActive).orElse(null);
                    if (pickupArea == null) errors.put("pickupAreaId", OUTSIDE_AREA);
                }
                pickupAddress = addressOrProfile(req.pickupAddress(), customer);
                if (pickupAddress == null) errors.put("pickupAddress", "Nhập địa chỉ lấy đồ");
            }
            if (delivery) {
                if (req.deliveryAreaId() == null) {
                    errors.put("deliveryAreaId", "Chọn khu vực giao đồ");
                } else {
                    deliveryArea = serviceAreaRepository.findById(req.deliveryAreaId()).filter(ServiceArea::isActive).orElse(null);
                    if (deliveryArea == null) errors.put("deliveryAreaId", OUTSIDE_AREA);
                }
                deliveryAddress = addressOrProfile(req.deliveryAddress(), customer);
                if (deliveryAddress == null) errors.put("deliveryAddress", "Nhập địa chỉ giao đồ");
            }
        }

        String appointmentError = AppointmentRules.appointmentError(req.appointmentAt(), now, open, close,
                settingService.getValue(SettingKey.BOOKING_MIN_LEAD_HOURS, Integer.class));
        if (appointmentError != null) {
            errors.put("appointmentAt", appointmentError);
        } else if (mode != null) {
            // Chỉ kiểm giờ nhận lại khi giờ gửi và hình thức hợp lệ; tối thiểu = giờ hoàn thành của hình thức (FR-015)
            String e = AppointmentRules.returnError(req.returnAt(), req.appointmentAt(), open, close,
                    mode.getCompletionHours());
            if (e != null) errors.put("returnAt", e);
        }

        String contactPhone = customer.getPhone();
        if (req.contactPhone() != null) {
            contactPhone = IdentifierNormalizer.normalizePhone(req.contactPhone()).orElse(null);
            if (contactPhone == null) errors.put("contactPhone", "Số điện thoại không hợp lệ (10 chữ số)");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(errors.values().iterator().next(), errors);
        }

        int maxOpen = settingService.getValue(SettingKey.BOOKING_MAX_OPEN_PER_CUSTOMER, Integer.class);
        long openCount = orderRepository.countByCustomerIdAndChannelAndStatusNotIn(customer.getId(),
                OrderChannel.ONLINE, List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        if (openCount + 1 > maxOpen) {
            throw new BusinessException("Anh/chị đang có " + openCount + " yêu cầu chưa đóng. Mỗi khách có tối đa "
                    + maxOpen + " yêu cầu chưa đóng. Hủy yêu cầu cũ"
                    + (storePhone.isBlank() ? "." : " hoặc gọi tiệm " + storePhone + "."));
        }

        String contactName = req.contactName() != null ? req.contactName().strip() : customer.getFullName();
        String performedBy = customer.getCustomerCode();
        String intakeCode = codeSequenceService.getNextIntakeCode();
        // Phí lấy / giao không cộng vào totalAmount (FR-009, FR-036)
        BigDecimal pickupFee = pickup ? pickupArea.getPickupFee() : BigDecimal.ZERO;
        BigDecimal deliveryFee = delivery ? deliveryArea.getDeliveryFee() : BigDecimal.ZERO;

        Order order = Order.builder()
                .orderCode(codeSequenceService.getNextOrderCode())
                .channel(OrderChannel.ONLINE)
                .compensationMultiplier(settingService.getCompensationMultiplier())
                .compensationCap(settingService.getCompensationCap())
                .intakeCode(intakeCode)
                .intakeRequestId(req.requestId())
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerPhone(customer.getPhone())
                .createdBy(performedBy)
                .washModeId(mode.getId())
                .washModeName(mode.getName())
                .washModeHours(mode.getCompletionHours())
                .requestedServicePrices(new LinkedHashMap<>(prices))
                .requestedServiceIds(serviceIds)
                .washWhitesSeparately(whites)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING_CONFIRMATION)
                .paymentStatus(PaymentStatus.UNPAID)
                .dueAt(req.returnAt())
                .note(req.note())
                .build();
        order.setFulfillment(OrderFulfillment.builder()
                .order(order)
                .dropoffMethod(req.dropoffMethod())
                .returnMethod(req.returnMethod())
                .pickupAreaId(pickupArea != null ? pickupArea.getId() : null)
                .pickupAreaName(pickupArea != null ? pickupArea.getName() : null)
                .deliveryAreaId(deliveryArea != null ? deliveryArea.getId() : null)
                .deliveryAreaName(deliveryArea != null ? deliveryArea.getName() : null)
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .appointmentAt(req.appointmentAt())
                .contactName(contactName)
                .contactPhone(contactPhone)
                .termsAcceptedAt(now)
                .pickupFee(pickupFee)
                .deliveryFee(deliveryFee)
                .build());
        String serviceNames = services.stream().map(LaundryService::getName).collect(Collectors.joining(", "));
        order.addHistory(HistoryActionType.CREATED, null, OrderStatus.PENDING_CONFIRMATION.name(),
                "Đặt online · lượt " + intakeCode + " · " + serviceNames, performedBy);

        Order saved = orderRepository.save(order);
        auditLogService.logAsCustomer(new AuditEntry(AuditAction.BOOKING_REQUEST, EntityType.ORDER,
                saved.getId(), null,
                Map.of("orderCode", saved.getOrderCode(),
                        "intakeCode", intakeCode,
                        "status", saved.getStatus().name(),
                        "appointmentAt", req.appointmentAt().toString()),
                null), performedBy);
        return response(saved, true);
    }

    // Tên dịch vụ dùng chung cho tạo mới và gửi lại
    private OnlineBookingResponse response(Order order, boolean created) {
        Map<Long, String> names = laundryServiceRepository.findAllById(order.getRequestedServiceIds()).stream()
                .collect(Collectors.toMap(LaundryService::getId, LaundryService::getName));
        return OnlineBookingResponse.from(order, created, names);
    }

    /** Địa chỉ khách gửi (đã trim), không gửi thì lấy địa chỉ hồ sơ; hồ sơ không bị sửa. */
    private static String addressOrProfile(String sent, Customer customer) {
        if (sent != null) return sent.strip();
        String profile = customer.getAddress();
        return profile == null || profile.isBlank() ? null : profile.strip();
    }
}
