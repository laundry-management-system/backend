package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DropoffStatus {
    AWAITING_CUSTOMER_DROPOFF("Chờ khách mang đồ tới"),
    CUSTOMER_DROPPED_OFF("Khách đã tới đưa đồ"),
    AWAITING_PICKUP("Chờ lấy đồ"),
    PICKED_UP("Đã lấy đồ từ khách");

    private final String description;
}
