package com.laundry.modules.shift.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Một dòng giao dịch trong ca: thu (COLLECT), hủy xác nhận (VOID) hoặc hoàn tiền (REFUND). */
public record ShiftTransactionResponse(Instant at, Long orderId, String orderCode, String type, String method,
                                       BigDecimal amount, String actorName) {
}
