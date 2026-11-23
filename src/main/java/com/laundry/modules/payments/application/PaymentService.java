package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.exception.ResourceConflictException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.payments.application.dto.ConfirmPaymentRequest;
import com.laundry.modules.payments.application.dto.PaymentResponse;
import com.laundry.modules.payments.application.dto.ResolvePaymentRequest;
import com.laundry.modules.payments.application.dto.VietQrResponse;
import com.laundry.modules.payments.application.port.AuditLogPort;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import com.laundry.modules.payments.application.port.ShiftIntegration;
import com.laundry.modules.payments.domain.Payment;
import com.laundry.modules.payments.domain.PaymentRepository;
import com.laundry.modules.payments.domain.PaymentResolution;
import com.laundry.modules.payments.domain.PaymentStatus;
import com.laundry.modules.payments.infrastructure.VietQrProperties;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final List<String> QR_TEMPLATES = List.of("compact2", "compact", "qr_only", "print");

    private final PaymentRepository paymentRepository;
    private final OrderIntegration orderIntegration;
    private final ActorIdResolver actorIdResolver;
    private final AuditLogPort auditLogPort;
    private final ShiftIntegration shiftIntegration;
    private final VietQrProperties vietQrProperties;
    private final SettingService settingService;

    @Transactional(readOnly = true)
    public VietQrResponse generateVietQr(String orderCode) {
        OrderSnapshot order = orderIntegration.findByCode(orderCode)
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
        if ("CANCELLED".equals(order.status())) {
            throw new BusinessException("Không thể tạo mã thanh toán cho đơn đã hủy");
        }
        requireReceived(order);
        if ("PAID".equals(order.paymentStatus())
                || paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.CONFIRMED)) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }
        BigDecimal amount = order.payableAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Tổng thanh toán không hợp lệ để tạo mã VietQR");
        }
        String amountText = amount.stripTrailingZeros().toPlainString();
        if (amountText.length() > 13) {
            throw new BusinessException("Tổng thanh toán vượt giới hạn tạo mã VietQR");
        }

        String bankId = bankSetting(SettingKey.BANK_BIN, Integer.class);
        String accountNo = bankSetting(SettingKey.BANK_ACCOUNT_NO, String.class);
        String accountName = bankSetting(SettingKey.BANK_ACCOUNT_NAME, String.class);
        String template = vietQrProperties.getTemplate();
        if (!QR_TEMPLATES.contains(template)) {
            throw new BusinessException("VIETQR_TEMPLATE không hợp lệ");
        }
        if (!accountNo.matches("[A-Za-z0-9]{6,19}")) {
            throw new BusinessException("Số tài khoản nhận thanh toán phải có từ 6 đến 19 chữ hoặc số");
        }

        String transferContent = order.orderCode();
        String imageUrl = UriComponentsBuilder.fromUriString(vietQrProperties.getImageBaseUrl())
                .pathSegment(bankId + "-" + accountNo + "-" + template + ".svg")
                .queryParam("amount", amountText)
                .queryParam("addInfo", transferContent)
                .queryParam("accountName", accountName)
                .build()
                .encode()
                .toUriString();
        return new VietQrResponse(order.orderCode(), amount, bankId, accountNo, accountName, transferContent, imageUrl);
    }

    /** Customer self-pay QR: read-only; another customer's order looks exactly like a missing one. */
    @Transactional(readOnly = true)
    public VietQrResponse generateCustomerVietQr(String orderCode, Long customerId) {
        OrderSnapshot order = orderIntegration.findByCode(orderCode)
                .filter(o -> customerId != null && customerId.equals(o.customerId()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        if (!"COMPLETED".equals(order.status()) || !orderIntegration.isOnlineDelivery(order.id())) {
            throw new BusinessException("Đơn chưa đến bước thanh toán");
        }
        if ("PAID".equals(order.paymentStatus())) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }
        return generateVietQr(orderCode);
    }

    @Transactional
    public PaymentResponse confirm(String orderCode, ConfirmPaymentRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        validatePayable(order);
        if (request.amount().compareTo(order.payableAmount()) != 0) {
            throw new BusinessException("Số tiền xác nhận phải bằng đúng tổng thanh toán của đơn");
        }
        if (paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.CONFIRMED)) {
            throw new ResourceConflictException("Đơn hàng đã có giao dịch thanh toán được xác nhận");
        }

        Instant now = Instant.now();
        Instant paidAt = request.paidAt() == null ? now : request.paidAt();
        if ((order.receivedAt() != null && paidAt.isBefore(order.receivedAt())) || paidAt.isAfter(now)) {
            throw new BusinessException("Thời điểm thanh toán phải từ lúc nhận đơn đến hiện tại");
        }
        long actorId = actorIdResolver.resolveId(username);
        // Khóa ca đang mở tới hết giao dịch để không lọt vào ca đang được kết (BR-K03)
        long shiftId = shiftIntegration.lockOpenShiftId();
        Payment payment = Payment.confirmed(order.id(), request.amount(), request.method(), paidAt, actorId,
                normalize(request.referenceCode()), invoiceNumber(), shiftId);
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Đơn hàng đã có giao dịch thanh toán được xác nhận");
        }
        orderIntegration.setPaymentStatus(order.id(), true);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(String orderCode) {
        OrderSnapshot order = orderIntegration.findByCode(orderCode)
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.id()).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse resolve(String orderCode, long paymentId, ResolvePaymentRequest request,
            String username, String ipAddress) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        Payment payment = paymentRepository.findByIdAndOrderId(paymentId, order.id())
                .orElseThrow(() -> NotFoundException.of("Giao dịch thanh toán", paymentId));
        long adminId = actorIdResolver.resolveId(username);
        Instant now = Instant.now();
        PaymentStatus previousStatus = payment.getStatus();
        long shiftId = shiftIntegration.lockOpenShiftId();
        payment.resolve(request.resolution(), adminId, now, request.reason().trim(), shiftId);
        paymentRepository.saveAndFlush(payment);
        orderIntegration.setPaymentStatus(order.id(), false);
        String auditAction = request.resolution() == PaymentResolution.REFUNDED
                ? "PAYMENT_REFUND"
                : "PAYMENT_VOID";
        // Giá trị phải khớp tên enum AuditAction/EntityType của module log, nếu không đọc nhật ký sẽ lỗi
        auditLogPort.record(adminId, auditAction, "PAYMENT", payment.getId(),
                "{\"status\":\"" + previousStatus + "\"}",
                "{\"status\":\"" + payment.getStatus() + "\"}",
                request.reason().trim(), ipAddress);
        return PaymentResponse.from(payment);
    }

    private static void requireReceived(OrderSnapshot order) {
        if ("PENDING_CONFIRMATION".equals(order.status()) || "AWAITING_DROPOFF".equals(order.status())) {
            throw new BusinessException("Đơn online chưa nhận đồ, chưa thể thực hiện thao tác này");
        }
    }

    private static void validatePayable(OrderSnapshot order) {
        requireReceived(order);
        if ("CANCELLED".equals(order.status()) || "DELIVERED".equals(order.status())) {
            throw new BusinessException("Không thể xác nhận thanh toán cho đơn đã hủy hoặc đã giao");
        }
        if ("PAID".equals(order.paymentStatus())) {
            throw new ResourceConflictException("Đơn hàng đã được thanh toán");
        }
        if (order.payableAmount() == null || order.payableAmount().signum() <= 0) {
            throw new BusinessException("Tổng thanh toán của đơn không hợp lệ");
        }
    }

    private String bankSetting(SettingKey key, Class<?> type) {
        String value;
        try {
            Object v = settingService.getValue(key, type);
            value = v == null ? null : v.toString();
        } catch (IllegalStateException ex) {
            value = null;
        }
        if (value == null || value.isBlank()) {
            throw new BusinessException("Chưa cấu hình tài khoản nhận thanh toán trong Cài đặt");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String invoiceNumber() {
        return "INV" + UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase();
    }
}
