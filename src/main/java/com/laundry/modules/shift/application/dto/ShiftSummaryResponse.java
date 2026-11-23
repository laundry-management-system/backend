package com.laundry.modules.shift.application.dto;

import java.time.Instant;

public record ShiftSummaryResponse(OpenShift openShift, long pendingApproval) {

    public record OpenShift(Long id, String code, String openedByName, Instant openedAt, boolean overdue) {
    }
}
