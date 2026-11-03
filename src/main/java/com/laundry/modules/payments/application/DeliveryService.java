package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.security.ActorIdResolver;
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
    private final ActorIdResolver actorIdResolver;

    @Transactional
    public DeliveryResponse deliver(String orderCode, DeliveryRequest request, String username) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException("Chỉ giao được đơn đã hoàn thành");
        }
        if (!"PAID".equals(order.paymentStatus())) {
            throw new BusinessException("Không thể giao đơn chưa thanh toán");
        }
        String receiverName = normalize(request.receiverName());
        String receiverRelation = normalize(request.receiverRelation());
        if (receiverName != null && receiverRelation == null) {
            throw new BusinessException("Cần ghi quan hệ với khách khi có người nhận hộ");
        }
        if (receiverName == null && receiverRelation != null) {
            throw new BusinessException("Cần ghi tên người nhận hộ khi khai báo quan hệ với khách");
        }

        Payment confirmedPayment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.id()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Đơn đang ở trạng thái đã thanh toán nhưng thiếu giao dịch"));
        Instant deliveredAt = Instant.now();
        long deliveredBy = actorIdResolver.resolveId(username);
        orderIntegration.deliver(order.id(), deliveredAt, deliveredBy, receiverName, receiverRelation);

        return new DeliveryResponse(order.orderCode(), order.customerName(), order.customerPhone(), order.dueAt(),
                order.totalAmount(), confirmedPayment.getInvoiceNo(), confirmedPayment.getMethod(),
                confirmedPayment.getPaidAt(), "DELIVERED", deliveredAt, deliveredBy, receiverName, receiverRelation);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
