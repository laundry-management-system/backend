package com.laundry.modules.payments.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_invoice_no", columnNames = "invoice_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "invoice_no", nullable = false, length = 20, unique = true)
    private String invoiceNo;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;

    @Column(name = "reference_code", length = 50)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.CONFIRMED;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    public static Payment confirmed(Long orderId, BigDecimal amount, PaymentMethod method, Instant paidAt,
                                    Long confirmedBy, String referenceCode, String invoiceNo) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.method = method;
        payment.paidAt = paidAt;
        payment.confirmedBy = confirmedBy;
        payment.referenceCode = referenceCode;
        payment.invoiceNo = invoiceNo;
        return payment;
    }

    public void resolve(PaymentResolution resolution, Long adminId, Instant at, String reason) {
        if (status != PaymentStatus.CONFIRMED) {
            throw new BusinessException("Chỉ có thể hủy hoặc hoàn một giao dịch đã xác nhận");
        }
        status = PaymentStatus.valueOf(resolution.name());
        voidedBy = adminId;
        voidedAt = at;
        voidReason = reason;
    }
}
