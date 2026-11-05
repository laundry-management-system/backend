package com.laundry.modules.report.domain;

import com.laundry.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Getter;

/** Đơn vị gom nhóm của báo cáo doanh thu. */
@Getter
public enum Granularity {
    DAY("YYYY-MM-DD"),
    MONTH("YYYY-MM");

    /**
     * -- GETTER --
     * Mẫu to_char của PostgreSQL, khớp với nhãn kỳ do
     *  sinh ra.
     */
    private final String sqlPattern;

    Granularity(String sqlPattern) {
        this.sqlPattern = sqlPattern;
    }

    /** Không truyền thì mặc định DAY; sai giá trị thì báo lỗi 400. */
    public static Granularity parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAY;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("granularity chỉ nhận DAY hoặc MONTH", "granularity");
        }
    }

    /** Danh sách nhãn kỳ liên tục từ from đến to, để điền 0 cho kỳ không có dữ liệu. */
    public List<String> periods(LocalDate from, LocalDate to) {
        List<String> result = new ArrayList<>();
        if (this == DAY) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                result.add(d.toString());
            }
        } else {
            YearMonth end = YearMonth.from(to);
            for (YearMonth m = YearMonth.from(from); !m.isAfter(end); m = m.plusMonths(1)) {
                result.add(m.toString());
            }
        }
        return result;
    }
}
