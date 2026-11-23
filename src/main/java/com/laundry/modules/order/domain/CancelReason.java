package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Lý do hủy / từ chối đơn; thứ tự khai báo = thứ tự hiển thị và phá hoà trong báo cáo (FR-032). */
@Getter
@RequiredArgsConstructor
public enum CancelReason {
    CUSTOMER_CHANGED_MIND("Khách đổi ý"),
    CUSTOMER_UNREACHABLE("Không liên lạc được khách"),
    WRONG_ORDER_INFO("Sai thông tin đơn"),
    OUT_OF_SERVICE_AREA("Ngoài khu vực phục vụ"),
    DUPLICATE_ORDER("Trùng đơn"),
    OTHER("Khác");

    private final String label;
}
