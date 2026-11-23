package com.laundry.modules.shift.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Một dòng giao dịch trong ca: thu (COLLECT), hủy xác nhận (VOID), hoàn tiền (REFUND) hoặc chi bồi thường (INCIDENT_PAYOUT). */
public record ShiftTransactionResponse(Instant at, Long orderId, String orderCode, String type, String method,
                                       BigDecimal amount, String actorName, String incidentCode) {
}
