package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryResponse(
        String orderCode,
        String customerName,
        String customerPhone,
        Instant dueAt,
        BigDecimal totalAmount,
        String invoiceNo,
        PaymentMethod paymentMethod,
        Instant paidAt,
        String status,
        Instant deliveredAt,
        String deliveredBy,
        String receiverName,
        String receiverRelation
) {
}
