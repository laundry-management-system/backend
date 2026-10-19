package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán");

    private final String description;
}

