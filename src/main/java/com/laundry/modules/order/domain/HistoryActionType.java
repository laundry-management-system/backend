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
    CANCELLED("Hủy đơn hàng"),
    PAYMENT_CONFIRMED("Xác nhận thanh toán"),
    PAYMENT_VOIDED("Hủy / hoàn giao dịch thanh toán"),
    DELIVERED("Giao đồ cho khách"),
    INCIDENT_REOPEN("Mở lần xử lý mới theo phiếu sự cố");

    private final String description;
}

