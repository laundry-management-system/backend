package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.Payment;
import com.laundry.modules.payments.domain.PaymentMethod;
import com.laundry.modules.payments.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
        String voidReason,
        @Schema(description = "Họ tên người xác nhận thanh toán")
        String confirmedByName,
        @Schema(description = "Họ tên người hủy xác nhận / hoàn tiền")
        String voidedByName
) {
    /** {@code names}: id người dùng → họ tên. */
    public static PaymentResponse from(Payment payment, Map<Long, String> names) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getInvoiceNo(), payment.getAmount(),
                payment.getMethod(), payment.getPaidAt(), payment.getConfirmedBy(), payment.getReferenceCode(),
                payment.getStatus(), payment.getVoidedBy(), payment.getVoidedAt(), payment.getVoidReason(),
                names.get(payment.getConfirmedBy()), payment.getVoidedBy() == null ? null : names.get(payment.getVoidedBy()));
    }
}
