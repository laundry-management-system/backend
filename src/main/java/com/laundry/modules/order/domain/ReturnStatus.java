package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReturnStatus {
    AWAITING_CUSTOMER_COLLECT("Chờ khách tới nhận"),
    CUSTOMER_COLLECTED("Khách đã nhận đồ"),
    AWAITING_DELIVERY("Chờ giao"),
    DELIVERING("Đang giao"),
    DELIVERED("Đã giao thành công");

    private final String description;
}
