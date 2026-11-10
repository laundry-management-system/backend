package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.exception.ResourceConflictException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.payments.application.dto.ConfirmPaymentRequest;
import com.laundry.modules.payments.application.dto.PaymentResponse;
import com.laundry.modules.payments.application.dto.ResolvePaymentRequest;
import com.laundry.modules.payments.application.dto.VietQrResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import com.laundry.modules.payments.domain.Payment;
import com.laundry.modules.payments.domain.PaymentRepository;
import com.laundry.modules.payments.domain.PaymentResolution;
import com.laundry.modules.payments.domain.PaymentStatus;
import com.laundry.modules.payments.infrastructure.VietQrProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final List<String> QR_TEMPLATES = List.of("compact2", "compact", "qr_only", "print");

    private final PaymentRepository paymentRepository;
    private final OrderIntegration orderIntegration;
    private final ActorIdResolver actorIdResolver;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final VietQrProperties vietQrProperties;

    @Transactional(readOnly = true)
    public VietQrResponse generateVietQr(String orderCode) {
        OrderSnapshot order = orderIntegration.findByCode(orderCode)
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
        if ("CANCELLED".equals(order.status())) {
            throw new BusinessException("Không thể tạo mã thanh toán cho đơn đã hủy");
        }
        if ("PAID".equals(order.paymentStatus())
                || paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.CONFIRMED)) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }
        BigDecimal amount = order.totalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Tổng thanh toán không hợp lệ để tạo mã VietQR");
        }
        String amountText = amount.stripTrailingZeros().toPlainString();
        if (amountText.length() > 13) {
            throw new BusinessException("Tổng thanh toán vượt giới hạn tạo mã VietQR");
        }

        String bankId = requireConfiguration(vietQrProperties.getBankId(), "VIETQR_BANK_ID");
        String accountNo = requireConfiguration(vietQrProperties.getAccountNo(), "VIETQR_ACCOUNT_NO");
        String accountName = requireConfiguration(vietQrProperties.getAccountName(), "VIETQR_ACCOUNT_NAME");
        String template = vietQrProperties.getTemplate();
        if (!QR_TEMPLATES.contains(template)) {
            throw new BusinessException("VIETQR_TEMPLATE không hợp lệ");
        }
        if (!accountNo.matches("[A-Za-z0-9]{6,19}")) {
            throw new BusinessException("VIETQR_ACCOUNT_NO phải có từ 6 đến 19 chữ hoặc số");
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

    @Transactional
    public PaymentResponse confirm(String orderCode, ConfirmPaymentRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        validatePayable(order);
        if (request.amount().compareTo(order.totalAmount()) != 0) {
            throw new BusinessException("Số tiền xác nhận phải bằng đúng tổng thanh toán của đơn");
        }
        if (paymentRepository.existsByOrderIdAndStatus(order.id(), PaymentStatus.CONFIRMED)) {
            throw new ResourceConflictException("Đơn hàng đã có giao dịch thanh toán được xác nhận");
        }

        Instant now = Instant.now();
        Instant paidAt = request.paidAt() == null ? now : request.paidAt();
        if (paidAt.isBefore(order.receivedAt()) || paidAt.isAfter(now)) {
            throw new BusinessException("Thời điểm thanh toán phải từ lúc nhận đơn đến hiện tại");
        }
        long actorId = actorIdResolver.resolveId(username);
        Payment payment = Payment.confirmed(order.id(), request.amount(), request.method(), paidAt, actorId,
                normalize(request.referenceCode()), invoiceNumber());
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Đơn hàng đã có giao dịch thanh toán được xác nhận");
        }
        orderIntegration.setPaymentStatus(order.id(), true);
        orderIntegration.appendHistory(order.id(), "PAYMENT_CONFIRMED", "UNPAID", "PAID",
                payment.getMethod() + " · " + payment.getAmount().toPlainString() + "đ · " + payment.getInvoiceNo(),
                username);
        audit(AuditAction.PAYMENT_CONFIRM, payment, order.orderCode(), null, snapshot(payment), null);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(String orderCode) {
        OrderSnapshot order = orderIntegration.findByCode(orderCode)
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.id());
        Map<Long, String> names = userNames(payments.stream()
                .flatMap(p -> Stream.of(p.getConfirmedBy(), p.getVoidedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return payments.stream().map(p -> PaymentResponse.from(p, names)).toList();
    }

    /** Admin hủy xác nhận (VOIDED) hoặc hoàn tiền (REFUNDED) một giao dịch; đơn trở lại Chưa thanh toán. */
    @Transactional
    public PaymentResponse resolve(String orderCode, long paymentId, ResolvePaymentRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        Payment payment = paymentRepository.findByIdAndOrderId(paymentId, order.id())
                .orElseThrow(() -> NotFoundException.of("Giao dịch thanh toán", paymentId));
        String reason = request.reason().trim();
        Map<String, Object> before = snapshot(payment);
        payment.resolve(request.resolution(), actorIdResolver.resolveId(username), Instant.now(), reason);
        paymentRepository.saveAndFlush(payment);
        orderIntegration.setPaymentStatus(order.id(), false);
        orderIntegration.appendHistory(order.id(), "PAYMENT_VOIDED", "CONFIRMED", payment.getStatus().name(),
                reason, username);
        AuditAction action = request.resolution() == PaymentResolution.REFUNDED
                ? AuditAction.PAYMENT_REFUND : AuditAction.PAYMENT_VOID;
        audit(action, payment, order.orderCode(), before, snapshot(payment), reason);
        return toResponse(payment);
    }

    /**
     * Hoàn tiền giao dịch đang hiệu lực khi hủy đơn đã thanh toán. Chỉ sửa bảng payments: đơn đang được
     * OrderService giữ trong cùng transaction nên trạng thái thanh toán và lịch sử của đơn do OrderService cập nhật.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundForCancelledOrder(long orderId, String orderCode, String reason, String username) {
        Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED)
                .findFirst()
                .orElse(null);
        if (payment == null) return;
        Map<String, Object> before = snapshot(payment);
        payment.resolve(PaymentResolution.REFUNDED, actorIdResolver.resolveId(username), Instant.now(), reason);
        paymentRepository.saveAndFlush(payment);
        audit(AuditAction.PAYMENT_REFUND, payment, orderCode, before, snapshot(payment), reason);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.from(payment, userNames(Stream.of(payment.getConfirmedBy(), payment.getVoidedBy())
                .filter(Objects::nonNull).collect(Collectors.toSet())));
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private void audit(AuditAction action, Payment payment, String orderCode, Map<String, Object> before,
                       Map<String, Object> after, String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.PAYMENT, payment.getId(), before, after, reason,
                orderCode));
    }

    private static Map<String, Object> snapshot(Payment p) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("invoiceNo", p.getInvoiceNo());
        data.put("amount", p.getAmount());
        data.put("method", p.getMethod().name());
        data.put("status", p.getStatus().name());
        data.put("paidAt", p.getPaidAt() == null ? null : p.getPaidAt().toString());
        return data;
    }

    private static void validatePayable(OrderSnapshot order) {
        if ("CANCELLED".equals(order.status()) || "DELIVERED".equals(order.status())) {
            throw new BusinessException("Không thể xác nhận thanh toán cho đơn đã hủy hoặc đã giao");
        }
        if ("PAID".equals(order.paymentStatus())) {
            throw new ResourceConflictException("Đơn hàng đã được thanh toán");
        }
        if (order.totalAmount() == null || order.totalAmount().signum() <= 0) {
            throw new BusinessException("Tổng thanh toán của đơn không hợp lệ");
        }
    }

    private static String requireConfiguration(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Chưa cấu hình " + variableName);
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
