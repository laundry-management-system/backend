package com.laundry.modules.incident.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentResolution {
    /** Giặt lại: mở lần xử lý mới cho đơn. */
    REWASH("Giặt lại"),
    /** Giảm giá: trừ vào tổng tiền đơn chưa thanh toán. */
    DISCOUNT("Giảm giá"),
    /** Bồi thường bằng tiền, tối đa theo điều khoản COMPENSATION_TERMS. */
    COMPENSATE("Bồi thường");

    private final String description;
}
