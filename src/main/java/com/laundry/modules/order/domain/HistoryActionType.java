package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HistoryActionType {
    CREATED("Tạo đơn hàng"),
    STATUS_CHANGE("Cập nhật trạng thái"),
    REVERT_STATUS("Lùi trạng thái"),
    RESCHEDULE("Dời ngày hẹn trả"),
    PRICE_ADJUST("Điều chỉnh giá"),
    ITEMS_UPDATED("Cập nhật danh sách món đồ"),
    CANCELLED("Hủy đơn hàng");

    private final String description;
}

