package com.laundry.modules.shift.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ShiftSummaryResponse(OpenShift openShift, long pendingApproval, BigDecimal differenceTotal,
                                   long shortCount, long overCount, BigDecimal handInTotal) {

    public record OpenShift(Long id, String code, String openedByName, Instant openedAt, boolean overdue) {
    }
}
