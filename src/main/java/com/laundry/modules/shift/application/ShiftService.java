package com.laundry.modules.shift.application;

import com.laundry.common.exception.ResourceConflictException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.shift.application.dto.ApproveShiftRequest;
import com.laundry.modules.shift.application.dto.CloseShiftRequest;
import com.laundry.modules.shift.application.dto.CurrentShiftResponse;
import com.laundry.modules.shift.application.dto.OpenShiftRequest;
import com.laundry.modules.shift.application.dto.ShiftDetailResponse;
import com.laundry.modules.shift.application.dto.ShiftRowResponse;
import com.laundry.modules.shift.application.dto.ShiftSummaryResponse;
import com.laundry.modules.shift.application.dto.ShiftTransactionResponse;
import com.laundry.modules.shift.application.port.AuditLogPort;
import com.laundry.modules.shift.domain.Shift;
import com.laundry.modules.shift.domain.ShiftCode;
import com.laundry.modules.shift.domain.ShiftLookupQuery;
import com.laundry.modules.shift.domain.ShiftMath;
import com.laundry.modules.shift.domain.ShiftMath.ShiftTotals;
import com.laundry.modules.shift.domain.ShiftRepository;
import com.laundry.modules.shift.domain.ShiftStatus;
import com.laundry.modules.shift.domain.ShiftTotalsQuery;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShiftRepository shiftRepository;
    private final ShiftLookupQuery lookupQuery;
    private final ShiftTotalsQuery totalsQuery;
    private final ActorIdResolver actorIdResolver;
    private final AuditLogPort auditLogPort;

    @Transactional(readOnly = true)
    public CurrentShiftResponse current() {
        CurrentShiftResponse.Current current = shiftRepository.findByStatus(ShiftStatus.OPEN)
                .map(this::toCurrent)
                .orElse(null);
        CurrentShiftResponse.Previous previous = shiftRepository.findFirstByClosedAtNotNullOrderByClosedAtDesc()
                .map(s -> new CurrentShiftResponse.Previous(s.getCode(), lookupQuery.userName(s.getClosedBy()),
                        s.getClosedAt(), s.getKeepCash()))
                .orElse(null);
        return new CurrentShiftResponse(current, previous);
    }

    @Transactional
    public ShiftDetailResponse open(OpenShiftRequest request, String username, String ipAddress) {
        shiftRepository.findByStatus(ShiftStatus.OPEN).ifPresent(s -> {
            throw alreadyOpen(s);
        });
        long actorId = actorIdResolver.resolveId(username);
        BigDecimal suggested = shiftRepository.findFirstByClosedAtNotNullOrderByClosedAtDesc()
                .map(Shift::getKeepCash)
                .orElse(null);
        ShiftMath.validateOpening(request.openingCash(), suggested, request.openingReason());

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        long countToday = shiftRepository.countByOpenedAtGreaterThanEqualAndOpenedAtLessThan(
                today.atStartOfDay(ZONE).toInstant(), today.plusDays(1).atStartOfDay(ZONE).toInstant());
        Shift shift = Shift.open(ShiftCode.next(today, countToday), actorId, now, request.openingCash(),
                suggested, request.openingReason());
        try {
            shift = shiftRepository.saveAndFlush(shift);
        } catch (DataIntegrityViolationException ex) {
            // Hai người mở cùng lúc: index một phần chỉ cho một ca OPEN
            throw new ResourceConflictException("Đã có ca đang mở, vui lòng tải lại trang");
        }
        auditLogPort.record(actorId, "SHIFT_OPEN", "SHIFT", shift.getId(), null,
                "{\"code\":\"" + shift.getCode() + "\",\"openingCash\":" + shift.getOpeningCash()
                        + ",\"suggested\":" + suggested + "}",
                shift.getOpeningReason(), ipAddress);
        // Ca vừa mở chưa có giao dịch nào
        return ShiftDetailResponse.from(shift, ShiftTotals.EMPTY, lookupQuery::userName);
    }

    @Transactional(readOnly = true)
    public ShiftDetailResponse detail(long id, String username, boolean admin) {
        Shift shift = readable(id, username, admin);
        return toDetail(shift);
    }

    @Transactional(readOnly = true)
    public Page<ShiftTransactionResponse> transactions(long id, Pageable pageable, String username, boolean admin) {
        readable(id, username, admin);
        return totalsQuery.transactions(id, pageable);
    }

    @Transactional
    public ShiftDetailResponse close(long id, CloseShiftRequest request, String username, boolean admin,
                                     String ipAddress) {
        // Khóa ca: thanh toán đang chờ cũng khóa ca này, nên không có giao dịch lọt vào sau khi tính tổng
        Shift shift = shiftRepository.findWithLockById(id)
                .orElseThrow(() -> NotFoundException.of("Ca làm việc", id));
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new ResourceConflictException("Ca " + shift.getCode() + " đã kết");
        }
        long actorId = actorIdResolver.resolveId(username);
        boolean onBehalf = actorId != shift.getOpenedBy();
        if (onBehalf && !admin) {
            throw new AccessDeniedException("Chỉ người mở ca hoặc Admin được kết ca");
        }
        String onBehalfReason = trimToNull(request.closeOnBehalfReason());
        if (onBehalf && onBehalfReason == null) {
            throw new BusinessException("Kết ca thay phải ghi lý do", "closeOnBehalfReason");
        }

        ShiftMath.validateCountedByDenominations(request.denominations(), request.countedCash());
        ShiftTotals totals = totalsQuery.totals(id);
        ShiftMath.ClosingSnapshot snapshot = ShiftMath.validateClose(shift.getOpeningCash(), totals,
                request.expectedCash(), request.countedCash(), request.keepCash(), request.bankReconciled(),
                request.differenceReason(), ShiftMath.denominationsJson(request.denominations()));
        ShiftMath.TransferCheck transfer = ShiftMath.transferCheck(totals.transferTotal(), request.bankTotal(),
                request.transferDifferenceReason());
        String keepJson = ShiftMath.validateKeepByDenominations(request.keepDenominations(),
                request.denominations(), request.keepCash());
        shift.close(snapshot, transfer, keepJson, actorId, Instant.now(), onBehalf ? onBehalfReason : null);
        shiftRepository.saveAndFlush(shift);

        auditLogPort.record(actorId, onBehalf ? "SHIFT_CLOSE_ON_BEHALF" : "SHIFT_CLOSE", "SHIFT", shift.getId(),
                "{\"status\":\"OPEN\"}",
                "{\"code\":\"" + shift.getCode() + "\",\"expected\":" + snapshot.expected()
                        + ",\"counted\":" + snapshot.counted() + ",\"difference\":" + snapshot.difference()
                        + ",\"handIn\":" + snapshot.handIn()
                        + ",\"transferDifference\":" + transfer.difference() + "}",
                onBehalf ? onBehalfReason : joinReasons(snapshot.differenceReason(), transfer.reason()), ipAddress);
        return toDetail(shift);
    }

    @Transactional(readOnly = true)
    public Page<ShiftRowResponse> list(String from, String to, ShiftStatus status, Pageable pageable) {
        Instant[] range = dayRange(from, to);
        // Danh sách luôn mới nhất trước, bỏ qua sort client gửi
        Pageable page = PageRequest.of(pageable.getPageNumber(),
                pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "openedAt"));
        Page<Shift> shifts = status == null
                ? shiftRepository.findByOpenedAtGreaterThanEqualAndOpenedAtLessThan(range[0], range[1], page)
                : shiftRepository.findByOpenedAtGreaterThanEqualAndOpenedAtLessThanAndStatus(range[0], range[1],
                        status, page);
        int warnAfterHours = lookupQuery.warnAfterHours();
        return shifts.map(s -> new ShiftRowResponse(s.getId(), s.getCode(), s.getStatus(),
                lookupQuery.userName(s.getOpenedBy()), s.getOpenedAt(), s.getClosedAt(), s.getExpectedCash(),
                s.getCountedCash(), s.getDifference(), s.getTransferTotal(), s.getTransferDifference(),
                isOverdue(s, warnAfterHours)));
    }

    @Transactional(readOnly = true)
    public ShiftSummaryResponse summary(String from, String to) {
        Instant[] range = dayRange(from, to);
        int warnAfterHours = lookupQuery.warnAfterHours();
        ShiftSummaryResponse.OpenShift open = shiftRepository.findByStatus(ShiftStatus.OPEN)
                .map(s -> new ShiftSummaryResponse.OpenShift(s.getId(), s.getCode(),
                        lookupQuery.userName(s.getOpenedBy()), s.getOpenedAt(), isOverdue(s, warnAfterHours)))
                .orElse(null);
        // shortcut: cộng trong bộ nhớ các ca đã kết của kỳ (vài chục ca/tháng); nhiều hơn thì đổi sang SUM ở DB
        List<Shift> closed = shiftRepository.findByOpenedAtGreaterThanEqualAndOpenedAtLessThanAndStatusIn(
                range[0], range[1], EnumSet.of(ShiftStatus.CLOSED, ShiftStatus.APPROVED));
        BigDecimal differenceTotal = BigDecimal.ZERO;
        BigDecimal handInTotal = BigDecimal.ZERO;
        long shortCount = 0;
        long overCount = 0;
        for (Shift s : closed) {
            differenceTotal = differenceTotal.add(s.getDifference());
            handInTotal = handInTotal.add(s.getHandInCash());
            if (s.getDifference().signum() < 0) {
                shortCount++;
            } else if (s.getDifference().signum() > 0) {
                overCount++;
            }
        }
        return new ShiftSummaryResponse(open, shiftRepository.countByStatus(ShiftStatus.CLOSED), differenceTotal,
                shortCount, overCount, handInTotal);
    }

    @Transactional
    public ShiftDetailResponse approve(long id, ApproveShiftRequest request, String username, String ipAddress) {
        Shift shift = shiftRepository.findWithLockById(id)
                .orElseThrow(() -> NotFoundException.of("Ca làm việc", id));
        long adminId = actorIdResolver.resolveId(username);
        shift.approve(adminId, Instant.now(), request.note());
        shiftRepository.saveAndFlush(shift);
        auditLogPort.record(adminId, "SHIFT_APPROVE", "SHIFT", shift.getId(), "{\"status\":\"CLOSED\"}",
                "{\"code\":\"" + shift.getCode() + "\",\"status\":\"APPROVED\"}", shift.getApprovalNote(),
                ipAddress);
        return toDetail(shift);
    }

    /** Khoảng [from 00:00, to + 1 ngày 00:00) theo giờ Việt Nam. */
    private static Instant[] dayRange(String from, String to) {
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(from);
            end = LocalDate.parse(to);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Ngày phải có dạng yyyy-MM-dd", "from");
        }
        if (start.isAfter(end)) {
            throw new BusinessException("Từ ngày không được sau đến ngày", "from");
        }
        return new Instant[] {start.atStartOfDay(ZONE).toInstant(), end.plusDays(1).atStartOfDay(ZONE).toInstant()};
    }

    private static boolean isOverdue(Shift s, int warnAfterHours) {
        return s.getStatus() == ShiftStatus.OPEN
                && Duration.between(s.getOpenedAt(), Instant.now()).toMinutes() >= warnAfterHours * 60L;
    }

    /** Admin đọc mọi ca; người mở ca chỉ đọc ca của mình khi ca còn mở (research R9). */
    private Shift readable(long id, String username, boolean admin) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> NotFoundException.of("Ca làm việc", id));
        if (!admin && (shift.getStatus() != ShiftStatus.OPEN
                || actorIdResolver.resolveId(username) != shift.getOpenedBy())) {
            throw new AccessDeniedException("Không có quyền xem ca này");
        }
        return shift;
    }

    private ShiftDetailResponse toDetail(Shift shift) {
        ShiftTotals live = shift.getStatus() == ShiftStatus.OPEN ? totalsQuery.totals(shift.getId()) : null;
        return ShiftDetailResponse.from(shift, live, lookupQuery::userName);
    }

    private static String joinReasons(String cash, String transfer) {
        if (transfer == null) {
            return cash;
        }
        return cash == null ? "Chuyển khoản: " + transfer : cash + " | Chuyển khoản: " + transfer;
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private CurrentShiftResponse.Current toCurrent(Shift s) {
        int warnAfterHours = lookupQuery.warnAfterHours();
        double openHours = Duration.between(s.getOpenedAt(), Instant.now()).toMinutes() / 60.0;
        return new CurrentShiftResponse.Current(s.getId(), s.getCode(), s.getStatus(), s.getOpenedBy(),
                lookupQuery.userName(s.getOpenedBy()), s.getOpenedAt(), Math.round(openHours * 10) / 10.0,
                warnAfterHours, openHours >= warnAfterHours);
    }

    private ResourceConflictException alreadyOpen(Shift s) {
        return new ResourceConflictException("Đã có ca đang mở: " + s.getCode() + " – "
                + lookupQuery.userName(s.getOpenedBy()));
    }
}
