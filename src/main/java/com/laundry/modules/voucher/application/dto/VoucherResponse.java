package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import com.laundry.modules.voucher.domain.Voucher;
import com.laundry.modules.voucher.domain.VoucherStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Số lượt còn lại")
        Integer remainingUses,
        VoucherStatus status,
        @Schema(description = "Đang bật, trong hạn và còn lượt")
        boolean isUsableNow,
        @Schema(description = "ACTIVE (còn hiệu lực) / NOT_STARTED / EXPIRED / USED_UP / DISABLED")
        String displayStatus,
        String description,
        @Schema(description = "Username người tạo")
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static VoucherResponse from(Voucher v, LocalDate today) {
        String display = v.displayStatus(today);
        return new VoucherResponse(v.getId(), v.getCode(), v.getDiscountType(), v.getDiscountValue(),
                v.getStartDate(), v.getEndDate(), v.getMinOrderValue(), v.getMaxUses(), v.getUsedCount(),
                Math.max(0, v.getMaxUses() - v.getUsedCount()), v.getStatus(), "ACTIVE".equals(display), display,
                v.getDescription(), v.getCreatedBy(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
