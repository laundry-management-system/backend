package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.payments.application.dto.PaymentDiscrepancyRequest;
import com.laundry.modules.payments.application.dto.PaymentDiscrepancyResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentDiscrepancyService {

    private final OrderIntegration orderIntegration;

    @Transactional
    public PaymentDiscrepancyResponse record(String orderCode, PaymentDiscrepancyRequest request) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        if ("CANCELLED".equals(order.status()) || "PAID".equals(order.paymentStatus())) {
            throw new BusinessException("Chỉ ghi nhận chênh lệch cho đơn chưa thanh toán và chưa hủy");
        }
        if (request.receivedAmount().compareTo(order.totalAmount()) == 0) {
            throw new BusinessException("Số tiền nhận được bằng tổng đơn, không có chênh lệch");
        }
        Instant recordedAt = Instant.now();
        String note = "Payment discrepancy at " + recordedAt + ": received " + request.receivedAmount()
                + ", expected " + order.totalAmount() + ". " + request.note().trim();
        orderIntegration.appendNote(order.id(), note);
        return new PaymentDiscrepancyResponse(order.orderCode(), order.totalAmount(), request.receivedAmount(),
                request.note().trim(), recordedAt);
    }
}
