package com.laundry.modules.service.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/** Giá theo hình thức (washModeId -> giá) và trạng thái của dịch vụ tại một thời điểm, để biết lần lưu nào cần ghi lịch sử giá. */
public record ServicePriceSnapshot(SortedMap<Long, BigDecimal> modePrices, ServiceStatus status) {

    public record Diff(Map<String, Object> oldData, Map<String, Object> newData) {
        public boolean isEmpty() { return newData.isEmpty(); }
    }

    public static ServicePriceSnapshot of(LaundryService s) {
        SortedMap<Long, BigDecimal> prices = new TreeMap<>();
        s.getPrices().forEach(p -> prices.put(p.getWashMode().getId(), compact(p.getPrice())));
        return new ServicePriceSnapshot(prices, s.getStatus());
    }

    /** Chỉ gồm các key khác nhau: {@code modePrice:<id>} theo id (thêm liên kết: cũ null, bỏ liên kết: mới null), rồi status. */
    public Diff diff(ServicePriceSnapshot after) {
        Map<String, Object> oldData = new LinkedHashMap<>();
        Map<String, Object> newData = new LinkedHashMap<>();
        TreeSet<Long> modeIds = new TreeSet<>(modePrices.keySet());
        modeIds.addAll(after.modePrices.keySet());
        for (Long id : modeIds) {
            put(oldData, newData, PriceField.modePriceKey(id), modePrices.get(id), after.modePrices.get(id));
        }
        put(oldData, newData, PriceField.STATUS.key(),
                status != null ? status.name() : null, after.status != null ? after.status.name() : null);
        return new Diff(oldData, newData);
    }

    private static void put(Map<String, Object> oldData, Map<String, Object> newData, String key, Object before, Object now) {
        if (same(before, now)) return;
        oldData.put(key, before);
        newData.put(key, now);
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
