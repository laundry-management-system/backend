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
    CONFIRMED("Xác nhận đơn online"),
    DROPOFF_STATUS_CHANGE("Cập nhật chiều gửi"),
    RETURN_STATUS_CHANGE("Cập nhật chiều trả"),
    TRACKING_CODE_CHANGE("Cập nhật mã vận đơn"),
    PAYMENT_CLAIM("Khách báo đã thanh toán"),
    PAYMENT_CLAIM_REJECTED("Từ chối báo thanh toán");

    private final String description;
}

