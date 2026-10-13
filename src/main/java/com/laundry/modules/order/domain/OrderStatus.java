package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    RECEIVED("Mới nhận"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Hoàn thành / Chờ lấy"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy");

    private final String description;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public static List<OrderStatus> getStandardFlow() {
        return List.of(RECEIVED, PROCESSING, COMPLETED, DELIVERED);
    }
}
