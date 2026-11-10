package com.laundry.modules.voucher.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Phiếu voucher (BM10 / QĐ12, BR-G01→G04).
 * Cấu trúc bảng giữ đúng bản đã có trên release/qc (cột, độ dài, kiểu created_by) để DB đang chạy không phải đổi.
 */
@Entity
@Table(
        name = "vouchers",
        indexes = {
                @Index(name = "idx_vouchers_code", columnList = "code"),
                @Index(name = "idx_vouchers_status", columnList = "status"),
                @Index(name = "idx_vouchers_dates", columnList = "start_date, end_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Voucher extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** PERCENT: 1–100 (%); AMOUNT: số tiền (đ). */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Ngày hiệu lực, tính cả hai đầu, theo giờ Việt Nam. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** So với tiền hàng (subtotal) của đơn. */
    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    /** Chỉ đổi bằng câu UPDATE atomic trong VoucherRepository; lưu entity không ghi đè cột này. */
    @Column(name = "used_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(length = 255)
    private String description;

    /** Username người tạo (giữ kiểu chuỗi như bảng hiện có). */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /** Lý do không áp được voucher; null nếu áp được. {@code alreadyHeld}: đơn đang giữ một lượt của voucher này. */
    public VoucherCheckReason evaluate(BigDecimal subtotal, LocalDate today, boolean alreadyHeld) {
        if (status == VoucherStatus.DISABLED) return VoucherCheckReason.DISABLED;
        if (today.isBefore(startDate)) return VoucherCheckReason.NOT_STARTED;
        if (today.isAfter(endDate)) return VoucherCheckReason.EXPIRED;
        if (!alreadyHeld && usedCount >= maxUses) return VoucherCheckReason.USED_UP;
        if (subtotal.compareTo(minOrderValue) < 0) return VoucherCheckReason.BELOW_MIN;
        return null;
    }

    /** Số tiền giảm, làm tròn tới đồng, không vượt tiền hàng (BR-G04). */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal raw = discountType == DiscountType.PERCENT
                ? subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : discountValue;
        return raw.min(subtotal).max(BigDecimal.ZERO);
    }

    /** Trạng thái hiển thị cho màn quản lý. */
    public String displayStatus(LocalDate today) {
        if (status == VoucherStatus.DISABLED) return "DISABLED";
        if (today.isBefore(startDate)) return "NOT_STARTED";
        if (today.isAfter(endDate)) return "EXPIRED";
        if (usedCount >= maxUses) return "USED_UP";
        return "ACTIVE";
    }
}
