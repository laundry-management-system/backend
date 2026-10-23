package com.laundry.modules.payments.application.dto;

import java.time.Instant;

public record OverdueOrderResponse(
        String orderCode,
        String customerName,
        String customerPhone,
        Instant completedAt,
        long daysInStorage,
        int notifyCount,
        String shelfLocation
) {
}
