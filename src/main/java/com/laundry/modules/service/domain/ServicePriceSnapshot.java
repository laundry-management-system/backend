package com.laundry.modules.service.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Giá trị các trường theo dõi của dịch vụ tại một thời điểm, để biết lần lưu nào cần ghi lịch sử giá. */
public record ServicePriceSnapshot(
        BigDecimal currentPrice,
        BigDecimal expressSurcharge,
        Integer standardHours,
        Integer expressHours,
        ServiceStatus status
) {

    public record Diff(Map<String, Object> oldData, Map<String, Object> newData) {
        public boolean isEmpty() { return newData.isEmpty(); }
    }

    public static ServicePriceSnapshot of(LaundryService s) {
        return new ServicePriceSnapshot(
                s.getCurrentPrice(), s.getExpressSurcharge(), s.getStandardHours(), s.getExpressHours(), s.getStatus());
    }

    /** Chỉ gồm các key khác nhau, theo thứ tự {@link PriceField}. */
    public Diff diff(ServicePriceSnapshot after) {
        Map<String, Object> oldData = new LinkedHashMap<>();
        Map<String, Object> newData = new LinkedHashMap<>();
        for (PriceField field : PriceField.values()) {
            Object before = value(field);
            Object now = after.value(field);
            if (!same(before, now)) {
                oldData.put(field.key(), before);
                newData.put(field.key(), now);
            }
        }
        return new Diff(oldData, newData);
    }

    private Object value(PriceField field) {
        return switch (field) {
            case PRICE -> compact(currentPrice);
            case EXPRESS_SURCHARGE -> compact(expressSurcharge);
            case STANDARD_HOURS -> standardHours;
            case EXPRESS_HOURS -> expressHours;
            case STATUS -> status != null ? status.name() : null;
        };
    }

    // 25000 và 25000.00 là cùng giá; lưu dạng gọn để JSON ra 25000 chứ không phải 2.5E+4
    private static BigDecimal compact(BigDecimal v) {
        return v == null ? null : new BigDecimal(v.stripTrailingZeros().toPlainString());
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }
}
