package com.laundry.modules.shift.application.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.laundry.modules.shift.domain.Shift;
import com.laundry.modules.shift.domain.ShiftMath;
import com.laundry.modules.shift.domain.ShiftMath.ShiftTotals;
import com.laundry.modules.shift.domain.ShiftStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.LongFunction;

public record ShiftDetailResponse(
        Long id,
        String code,
        ShiftStatus status,
        Long openedById,
        String openedByName,
        Instant openedAt,
        BigDecimal openingCash,
        BigDecimal suggestedOpeningCash,
        String openingReason,
        Long closedById,
        String closedByName,
        Instant closedAt,
        String closeOnBehalfReason,
        BigDecimal cashIn,
        int cashInCount,
        BigDecimal cashOut,
        int cashOutCount,
        BigDecimal expectedCash,
        BigDecimal transferTotal,
        int transferCount,
        BigDecimal countedCash,
        BigDecimal difference,
        String differenceReason,
        @JsonRawValue String denominations,
        boolean bankReconciled,
        BigDecimal bankTotal,
        BigDecimal transferDifference,
        String transferDifferenceReason,
        BigDecimal keepCash,
        @JsonRawValue String keepDenominations,
        BigDecimal handInCash,
        String approvedByName,
        Instant approvedAt,
        String approvalNote
) {

    /**
     * Ca OPEN dùng {@code liveTotals} tính trực tiếp; ca đã kết dùng số đã chụp lúc kết.
     * {@code names} tra họ tên theo id người dùng.
     */
    public static ShiftDetailResponse from(Shift s, ShiftTotals liveTotals, LongFunction<String> names) {
        boolean open = s.getStatus() == ShiftStatus.OPEN;
        ShiftTotals t = open ? liveTotals : new ShiftTotals(s.getCashIn(), s.getCashInCount(), s.getCashOut(),
                s.getCashOutCount(), s.getTransferTotal(), s.getTransferCount());
        BigDecimal expected = open ? ShiftMath.expected(s.getOpeningCash(), t) : s.getExpectedCash();
        return new ShiftDetailResponse(s.getId(), s.getCode(), s.getStatus(), s.getOpenedBy(),
                names.apply(s.getOpenedBy()), s.getOpenedAt(), s.getOpeningCash(), s.getSuggestedOpeningCash(),
                s.getOpeningReason(), s.getClosedBy(), name(names, s.getClosedBy()), s.getClosedAt(),
                s.getCloseOnBehalfReason(), t.cashIn(), t.cashInCount(), t.cashOut(), t.cashOutCount(), expected,
                t.transferTotal(), t.transferCount(), s.getCountedCash(), s.getDifference(), s.getDifferenceReason(),
                s.getDenominations(), s.isBankReconciled(), s.getBankTotal(), s.getTransferDifference(),
                s.getTransferDifferenceReason(), s.getKeepCash(), s.getKeepDenominations(), s.getHandInCash(),
                name(names, s.getApprovedBy()), s.getApprovedAt(), s.getApprovalNote());
    }

    private static String name(LongFunction<String> names, Long id) {
        return id == null ? null : names.apply(id);
    }
}
