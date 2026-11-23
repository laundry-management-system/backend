package com.laundry.modules.payments.application.dto;

import java.time.Instant;

public record OverdueOrderResponse(
        Long orderId,
        String orderCode,
        String customerName,
        String customerPhone,
        boolean customerVip,
        Instant completedAt,
        long daysInStorage,
        int notifyCount,
        Instant lastNotifiedAt,
        String shelfLocation,
        String paymentStatus
) {
}
