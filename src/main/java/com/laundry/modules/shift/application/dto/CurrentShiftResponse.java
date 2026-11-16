package com.laundry.modules.shift.application.dto;

import com.laundry.modules.shift.domain.ShiftStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CurrentShiftResponse(Current current, Previous previous) {

    public record Current(Long id, String code, ShiftStatus status, Long openedById, String openedByName,
                          Instant openedAt, double openHours, int warnAfterHours, boolean overdue) {
    }

    /** Ca kết gần nhất; keepCash là số gợi ý tiền đầu ca. */
    public record Previous(String code, String closedByName, Instant closedAt, BigDecimal keepCash) {
    }
}
