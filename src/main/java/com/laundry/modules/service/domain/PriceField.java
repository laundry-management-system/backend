package com.laundry.modules.service.domain;

import java.util.Arrays;
import java.util.Optional;

/** Các trường của dịch vụ được ghi vào lịch sử thay đổi giá (BR-C02). Thứ tự khai báo là thứ tự hiển thị trong một lần đổi. */
public enum PriceField {
    PRICE("currentPrice"),
    EXPRESS_SURCHARGE("expressSurcharge"),
    STANDARD_HOURS("standardHours"),
    EXPRESS_HOURS("expressHours"),
    STATUS("status");

    private final String key;

    PriceField(String key) { this.key = key; }

    /** Key trong old_data / new_data của nhật ký. */
    public String key() { return key; }

    public static Optional<PriceField> ofKey(String key) {
        return Arrays.stream(values()).filter(f -> f.key.equals(key)).findFirst();
    }
}
