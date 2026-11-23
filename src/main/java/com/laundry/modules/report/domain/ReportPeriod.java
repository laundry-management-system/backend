package com.laundry.modules.report.domain;

import com.laundry.common.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Khoảng ngày [from, to] (gồm cả hai đầu) theo giờ Việt Nam. */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int MAX_DAYS = 366;

    public static ReportPeriod of(String rawFrom, String rawTo) {
        LocalDate from = parse(rawFrom, "from");
        LocalDate to = parse(rawTo, "to");
        if (from.isAfter(to)) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc", "from");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new BusinessException("Khoảng thời gian tối đa " + MAX_DAYS + " ngày", "to");
        }
        return new ReportPeriod(from, to);
    }

    /** Không truyền khoảng ngày → hôm nay; chỉ truyền một đầu → lỗi. */
    public static ReportPeriod ofOptional(String rawFrom, String rawTo) {
        boolean noFrom = rawFrom == null || rawFrom.isBlank();
        boolean noTo = rawTo == null || rawTo.isBlank();
        if (noFrom && noTo) {
            return today();
        }
        if (noFrom || noTo) {
            throw new BusinessException("Cần cả from và to", noFrom ? "from" : "to");
        }
        return of(rawFrom, rawTo);
    }

    /** Kỳ liền trước, cùng số ngày. */
    public ReportPeriod previous() {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return new ReportPeriod(from.minusDays(days), from.minusDays(1));
    }

    /** Ngày hôm nay theo giờ Việt Nam. */
    public static ReportPeriod today() {
        LocalDate today = LocalDate.now(ZONE);
        return new ReportPeriod(today, today);
    }

    private static LocalDate parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Thiếu tham số " + field, field);
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Tham số " + field + " phải có dạng yyyy-MM-dd", field);
        }
    }

    /** Đầu khoảng, tính cả (00:00 của from). */
    public Instant startInstant() {
        return from.atStartOfDay(ZONE).toInstant();
    }

    /** Cuối khoảng, không tính (00:00 của ngày sau to). */
    public Instant endInstant() {
        return to.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
