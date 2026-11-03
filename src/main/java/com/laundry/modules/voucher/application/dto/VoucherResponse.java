package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import com.laundry.modules.voucher.domain.Voucher;
import com.laundry.modules.voucher.domain.VoucherStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record VoucherResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minOrderValue,
        Integer maxUses,
        Integer usedCount,
        Integer remainingUses,
        VoucherStatus status,
        boolean isUsableNow,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static VoucherResponse from(Voucher voucher) {
        LocalDate today = LocalDate.now();
        boolean usable = voucher.isActive() && voucher.isUsableOn(today) && voucher.hasAvailableUses();
        int remaining = Math.max(0, voucher.getMaxUses() - (voucher.getUsedCount() != null ? voucher.getUsedCount() : 0));

        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getStartDate(),
                voucher.getEndDate(),
                voucher.getMinOrderValue(),
                voucher.getMaxUses(),
                voucher.getUsedCount(),
                remaining,
                voucher.getStatus(),
                usable,
                voucher.getCreatedBy(),
                voucher.getCreatedAt(),
                voucher.getUpdatedAt()
        );
    }
}
