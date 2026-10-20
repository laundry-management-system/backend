package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.Payment;
import com.laundry.modules.payments.domain.PaymentMethod;
import com.laundry.modules.payments.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        String invoiceNo,
        BigDecimal amount,
        PaymentMethod method,
        Instant paidAt,
        Long confirmedBy,
        String referenceCode,
        PaymentStatus status,
        Long voidedBy,
        Instant voidedAt,
        String voidReason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getInvoiceNo(), payment.getAmount(),
                payment.getMethod(), payment.getPaidAt(), payment.getConfirmedBy(), payment.getReferenceCode(),
                payment.getStatus(), payment.getVoidedBy(), payment.getVoidedAt(), payment.getVoidReason());
    }
}
