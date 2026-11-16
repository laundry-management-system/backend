package com.laundry.modules.order.application.dto;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Bộ lọc danh sách đơn (UC07 / BM4). Ngày hẹn tính theo giờ Việt Nam, gồm cả hai đầu. */
public record OrderFilter(
        OrderStatus status,
        List<OrderStatus> statuses,
        OrderPriority priority,
        String phone,
        String search,
        String intakeCode,
        OrderType orderType,
        Boolean classified,
        Boolean late,
        Instant dueStart,
        Instant dueEnd
) {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static OrderFilter of(OrderStatus status, List<OrderStatus> statuses, OrderPriority priority,
                                 String phone, String search, String intakeCode, OrderType orderType,
                                 Boolean classified, Boolean late, String dueFrom, String dueTo) {
        LocalDate from = parseDate(dueFrom, "dueFrom");
        LocalDate to = parseDate(dueTo, "dueTo");
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Ngày hẹn từ không được sau ngày hẹn đến", "dueFrom");
        }
        return new OrderFilter(
                status,
                statuses == null ? List.of() : List.copyOf(statuses),
                priority,
                clean(phone),
                clean(search),
                clean(intakeCode),
                orderType,
                classified,
                late,
                from == null ? null : from.atStartOfDay(ZONE).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    private static String clean(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    private static LocalDate parseDate(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Tham số " + field + " phải có dạng yyyy-MM-dd", field);
        }
    }
}
