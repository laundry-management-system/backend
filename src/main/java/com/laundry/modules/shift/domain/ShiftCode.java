package com.laundry.modules.shift.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Mã ca dạng CA-YYYYMMDD-NN theo ngày mở ca (giờ Việt Nam). */
public final class ShiftCode {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private ShiftCode() {
    }

    public static String next(LocalDate date, long countToday) {
        return "CA-" + date.format(DATE) + "-" + String.format("%02d", countToday + 1);
    }
}
