package com.laundry.modules.payments.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PublicOrderLookupResponse(
        String status,
        Instant dueAt,
        BigDecimal totalAmount,
        String paymentStatus
) {
}
