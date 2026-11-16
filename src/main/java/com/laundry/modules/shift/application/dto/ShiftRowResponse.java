package com.laundry.modules.shift.application.dto;

import com.laundry.modules.shift.domain.ShiftStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Một dòng danh sách ca; ca OPEN chưa có số liệu kết ca nên các cột tiền là null. */
public record ShiftRowResponse(Long id, String code, ShiftStatus status, String openedByName, Instant openedAt,
                               Instant closedAt, BigDecimal expectedCash, BigDecimal countedCash,
                               BigDecimal difference, BigDecimal transferTotal, BigDecimal transferDifference,
                               boolean overdue) {
}
