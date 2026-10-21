package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    NORMAL("Đơn thường"),
    EXPRESS("Giặt nhanh");

    private final String description;
}

