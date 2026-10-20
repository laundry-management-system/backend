package com.laundry.modules.payments.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDiscrepancyResponse(
        String orderCode,
        BigDecimal expectedAmount,
        BigDecimal receivedAmount,
        String note,
        Instant recordedAt
) {
}
