package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum ServiceType {
    WASH_DRY("Giặt sấy tiêu chuẩn", BigDecimal.valueOf(15000), 24),
    DRY_CLEAN("Giặt hấp cao cấp", BigDecimal.valueOf(35000), 48),
    IRON_ONLY("Chỉ ủi phẳng", BigDecimal.valueOf(10000), 12),
    EXPRESS("Giặt sấy hỏa tốc (Lấy liền)", BigDecimal.valueOf(30000), 4),
    SPECIAL("Giặt đồ đặc biệt (Chăn mền / Gấu bông / Giày)", BigDecimal.valueOf(25000), 36);

    private final String displayName;
    private final BigDecimal defaultPricePerKg;
    private final int standardHours;
}

