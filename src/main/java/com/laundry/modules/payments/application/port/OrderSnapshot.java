package com.laundry.modules.payments.application.port;

import java.math.BigDecimal;
import java.time.Instant;

/** Read model shared with the orders module through its PostgreSQL contract. */
public record OrderSnapshot(
        Long id,
        String orderCode,
        Long customerId,
        String customerName,
        String customerPhone,
        Instant receivedAt,
        Instant dueAt,
        BigDecimal totalAmount,
        String status,
        String paymentStatus,
        Instant completedAt,
        String shelfLocation,
        Instant deliveredAt,
        String deliveredBy,
        String receiverName,
        String receiverRelation,
        Instant lastNotifiedAt,
        int notifyCount,
        String note
) {
}
