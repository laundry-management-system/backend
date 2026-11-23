package com.laundry.modules.service.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Các trường của dịch vụ được ghi vào lịch sử thay đổi giá (BR-C02). Thứ tự khai báo là thứ tự hiển thị trong một lần đổi.
 * PRICE, EXPRESS_SURCHARGE, STANDARD_HOURS, EXPRESS_HOURS chỉ còn để đọc bản ghi cũ.
 */
public enum PriceField {
    MODE_PRICE("modePrice"),
    PRICE("currentPrice"),
    EXPRESS_SURCHARGE("expressSurcharge"),
    STANDARD_HOURS("standardHours"),
    EXPRESS_HOURS("expressHours"),
    STATUS("status");

    /** Key giá theo hình thức: {@code modePrice:<washModeId>}. */
    public static final String MODE_PRICE_PREFIX = "modePrice:";

    private final String key;

    PriceField(String key) { this.key = key; }

    /** Key trong old_data / new_data của nhật ký (MODE_PRICE dùng {@link #modePriceKey}). */
    public String key() { return key; }

    public static String modePriceKey(Long washModeId) { return MODE_PRICE_PREFIX + washModeId; }

    public static Optional<PriceField> ofKey(String key) {
        if (key != null && key.startsWith(MODE_PRICE_PREFIX)) return Optional.of(MODE_PRICE);
        return Arrays.stream(values()).filter(f -> f != MODE_PRICE && f.key.equals(key)).findFirst();
    }
}
