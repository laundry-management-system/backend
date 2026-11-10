package com.laundry.modules.voucher.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Lý do không áp được voucher. Tên và câu thông báo khớp VOUCHER_REASON_LABEL của FE. */
@Getter
@RequiredArgsConstructor
public enum VoucherCheckReason {
    NOT_FOUND("Mã voucher không tồn tại"),
    DISABLED("Voucher đã bị tắt"),
    NOT_STARTED("Voucher chưa đến ngày áp dụng"),
    EXPIRED("Voucher đã hết hạn"),
    USED_UP("Voucher đã hết lượt sử dụng"),
    BELOW_MIN("Đơn chưa đạt giá trị tối thiểu của voucher");

    private final String message;
}
