package com.laundry.modules.shift.domain;

import com.laundry.common.exception.ResourceConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Ca làm việc (BM15). Chỉ một ca OPEN tại một thời điểm, ràng buộc bằng index một phần trong data.sql. */
@Entity
@Table(name = "shifts", indexes = @Index(name = "idx_shifts_opened_at", columnList = "opened_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "opened_by", nullable = false)
    private Long openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opening_cash", nullable = false, precision = 12, scale = 0)
    private BigDecimal openingCash;

    @Column(name = "suggested_opening_cash", precision = 12, scale = 0)
    private BigDecimal suggestedOpeningCash;

    @Column(name = "opening_reason", length = 255)
    private String openingReason;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "close_on_behalf_reason", length = 255)
    private String closeOnBehalfReason;

    @Column(name = "cash_in", precision = 12, scale = 0)
    private BigDecimal cashIn;

    @Column(name = "cash_in_count")
    private Integer cashInCount;

    @Column(name = "cash_out", precision = 12, scale = 0)
    private BigDecimal cashOut;

    @Column(name = "cash_out_count")
    private Integer cashOutCount;

    @Column(name = "expected_cash", precision = 12, scale = 0)
    private BigDecimal expectedCash;

    @Column(name = "counted_cash", precision = 12, scale = 0)
    private BigDecimal countedCash;

    @Column(precision = 12, scale = 0)
    private BigDecimal difference;

    @Column(name = "difference_reason", length = 255)
    private String differenceReason;

    @Column(columnDefinition = "text")
    private String denominations;

    @Column(name = "transfer_total", precision = 12, scale = 0)
    private BigDecimal transferTotal;

    @Column(name = "transfer_count")
    private Integer transferCount;

    @Column(name = "bank_reconciled", nullable = false)
    private boolean bankReconciled;

    /** Tổng tiền về trong ứng dụng ngân hàng lúc kết ca; bằng transferTotal khi khớp. */
    @Column(name = "bank_total", precision = 12, scale = 0)
    private BigDecimal bankTotal;

    @Column(name = "transfer_difference", precision = 12, scale = 0)
    private BigDecimal transferDifference;

    @Column(name = "transfer_difference_reason", length = 255)
    private String transferDifferenceReason;

    @Column(name = "keep_cash", precision = 12, scale = 0)
    private BigDecimal keepCash;

    /** Số tờ để lại két theo mệnh giá (JSON), để in phiếu. */
    @Column(name = "keep_denominations", columnDefinition = "text")
    private String keepDenominations;

    @Column(name = "hand_in_cash", precision = 12, scale = 0)
    private BigDecimal handInCash;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approval_note", length = 500)
    private String approvalNote;

    public static Shift open(String code, Long openedBy, Instant openedAt, BigDecimal openingCash,
                             BigDecimal suggested, String reason) {
        Shift shift = new Shift();
        shift.code = code;
        shift.openedBy = openedBy;
        shift.openedAt = openedAt;
        shift.openingCash = openingCash;
        shift.suggestedOpeningCash = suggested;
        shift.openingReason = reason == null || reason.isBlank() ? null : reason.trim();
        return shift;
    }

    public void close(ShiftMath.ClosingSnapshot s, ShiftMath.TransferCheck transfer, String keepDenominationsJson,
                      Long closedBy, Instant closedAt, String onBehalfReason) {
        if (status != ShiftStatus.OPEN) {
            throw new ResourceConflictException("Ca không còn mở");
        }
        status = ShiftStatus.CLOSED;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        closeOnBehalfReason = onBehalfReason;
        cashIn = s.totals().cashIn();
        cashInCount = s.totals().cashInCount();
        cashOut = s.totals().cashOut();
        cashOutCount = s.totals().cashOutCount();
        transferTotal = s.totals().transferTotal();
        transferCount = s.totals().transferCount();
        expectedCash = s.expected();
        countedCash = s.counted();
        difference = s.difference();
        differenceReason = s.differenceReason();
        denominations = s.denominationsJson();
        bankReconciled = true;
        bankTotal = transfer.bankTotal();
        transferDifference = transfer.difference();
        transferDifferenceReason = transfer.reason();
        keepCash = s.keep();
        keepDenominations = keepDenominationsJson;
        handInCash = s.handIn();
    }

    public void approve(Long adminId, Instant at, String note) {
        if (status != ShiftStatus.CLOSED) {
            throw new ResourceConflictException("Chỉ duyệt được ca đã kết");
        }
        status = ShiftStatus.APPROVED;
        approvedBy = adminId;
        approvedAt = at;
        approvalNote = note == null || note.isBlank() ? null : note.trim();
    }
}
