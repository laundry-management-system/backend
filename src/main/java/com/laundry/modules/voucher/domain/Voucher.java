package com.laundry.modules.voucher.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
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

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    public void validateBusinessRules() {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Discount value must be greater than 0");
        }
        if (discountType == DiscountType.PERCENT && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Percentage discount cannot exceed 100%");
        }
        if (startDate == null || endDate == null) {
            throw new BusinessException("Start date and end date must not be null (BR-G01)");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("End date must be greater than or equal to start date (BR-G01)");
        }
        if (minOrderValue == null || minOrderValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Minimum order value must be greater than or equal to 0 (BR-G02)");
        }
        if (maxUses == null || maxUses <= 0) {
            throw new BusinessException("Maximum usage count must be greater than 0 (BR-G03)");
        }
        if (usedCount == null || usedCount < 0) {
            usedCount = 0;
        }
    }

    public boolean isActive() {
        return this.status == VoucherStatus.ACTIVE;
    }

    public boolean isUsableOn(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean hasAvailableUses() {
        return usedCount < maxUses;
    }

    public boolean satisfiesMinOrderValue(BigDecimal orderValue) {
        if (orderValue == null) {
            return false;
        }
        return orderValue.compareTo(minOrderValue != null ? minOrderValue : BigDecimal.ZERO) >= 0;
    }

    public BigDecimal calculateDiscount(BigDecimal orderValue) {
        if (orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = orderValue.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }

        return discount.min(orderValue);
    }

    public void incrementUsedCount() {
        if (!hasAvailableUses()) {
            throw new BusinessException("Voucher '" + code + "' has reached its maximum usage limit (BR-G03)");
        }
        this.usedCount++;
    }

    public void decrementUsedCount() {
        if (this.usedCount > 0) {
            this.usedCount--;
        }
    }

    public void disable() {
        if (this.status == VoucherStatus.DISABLED) {
            throw new BusinessException("Voucher is already disabled");
        }
        this.status = VoucherStatus.DISABLED;
    }

    public void activate() {
        if (this.status == VoucherStatus.ACTIVE) {
            throw new BusinessException("Voucher is already active");
        }
        this.status = VoucherStatus.ACTIVE;
    }

    public void updateDetails(
            DiscountType discountType,
            BigDecimal discountValue,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minOrderValue,
            Integer maxUses
    ) {
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minOrderValue = minOrderValue != null ? minOrderValue : BigDecimal.ZERO;
        this.maxUses = maxUses;

        validateBusinessRules();

        if (this.usedCount > this.maxUses) {
            throw new BusinessException("Maximum uses cannot be less than already used count (" + this.usedCount + ")");
        }
    }
}
