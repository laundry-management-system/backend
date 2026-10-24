package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    NORMAL("Tiêu chuẩn"),
    EXPRESS("Giặt nhanh");

    private final String description;
}

