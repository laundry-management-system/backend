package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.payments.application.dto.CheckoutRequest;
import com.laundry.modules.payments.application.dto.ConfirmPaymentRequest;
import com.laundry.modules.payments.application.dto.DeliveryRequest;
import com.laundry.modules.payments.application.dto.DeliveryResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import com.laundry.modules.payments.domain.Payment;
import com.laundry.modules.payments.domain.PaymentRepository;
import com.laundry.modules.payments.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final OrderIntegration orderIntegration;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Transactional
    public DeliveryResponse deliver(String orderCode, DeliveryRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException("Chỉ giao được đơn đã hoàn thành");
        }
        if (!"PAID".equals(order.paymentStatus())) {
            throw new BusinessException("Không thể giao đơn chưa thanh toán");
        }
        Receiver receiver = Receiver.of(request.receiverName(), request.receiverRelation());

        Payment confirmedPayment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.id()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Đơn đang ở trạng thái đã thanh toán nhưng thiếu giao dịch"));
        Instant deliveredAt = Instant.now();
        orderIntegration.deliver(order.id(), deliveredAt, username, receiver.name(), receiver.relation());
        // STATUS_CHANGE (không dùng DELIVERED) để FE hiển thị "Hoàn thành → Đã giao" như các bước đổi trạng thái khác
        orderIntegration.appendHistory(order.id(), "STATUS_CHANGE", "COMPLETED", "DELIVERED",
                receiver.name() != null
                        ? "Giao cho người nhận hộ: " + receiver.name() + " (" + receiver.relation() + ")"
                        : "Giao đồ trực tiếp cho khách hàng",
                username);

        return new DeliveryResponse(order.orderCode(), order.customerName(), order.customerPhone(), order.dueAt(),
                order.totalAmount(), confirmedPayment.getInvoiceNo(), confirmedPayment.getMethod(),
                confirmedPayment.getPaidAt(), "DELIVERED", deliveredAt, username, receiver.name(), receiver.relation());
    }

    /**
     * Thu tiền và giao đồ trong một transaction (WF-E01→E03): bước nào lỗi thì không bước nào được lưu.
     * Kiểm tra đơn đã hoàn thành và thông tin người nhận trước khi thu tiền.
     */
    @Transactional
    public DeliveryResponse checkout(String orderCode, CheckoutRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException("Chỉ thanh toán & giao được đơn đã hoàn thành");
        }
        Receiver.of(request.receiverName(), request.receiverRelation());
        if (!"PAID".equals(order.paymentStatus())) {
            paymentService.confirm(orderCode, new ConfirmPaymentRequest(
                    request.amount(), request.method(), null, request.referenceCode()), username);
        }
        return deliver(orderCode, new DeliveryRequest(request.receiverName(), request.receiverRelation()), username);
    }

    private record Receiver(String name, String relation) {
        static Receiver of(String rawName, String rawRelation) {
            String name = normalize(rawName);
            String relation = normalize(rawRelation);
            if (name != null && relation == null) {
                throw new BusinessException("Cần ghi quan hệ với khách khi có người nhận hộ", "receiverRelation");
            }
            if (name == null && relation != null) {
                throw new BusinessException("Cần ghi tên người nhận hộ khi khai báo quan hệ với khách", "receiverName");
            }
            return new Receiver(name, relation);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
