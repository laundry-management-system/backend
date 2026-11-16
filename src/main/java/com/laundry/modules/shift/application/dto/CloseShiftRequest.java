package com.laundry.modules.shift.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record CloseShiftRequest(
        /** Số tiền mặt dự kiến người dùng đang thấy; khác số server tính → 409 để xem lại. */
        @NotNull(message = "Thiếu tiền mặt dự kiến")
        BigDecimal expectedCash,

        @NotNull(message = "Vui lòng nhập tiền mặt thực đếm")
        @PositiveOrZero(message = "Tiền mặt thực đếm phải từ 0 trở lên")
        @Digits(integer = 12, fraction = 0, message = "Tiền mặt thực đếm phải là số nguyên tối đa 12 chữ số")
        BigDecimal countedCash,

        /** Số tờ theo mệnh giá, bắt buộc; tổng phải bằng countedCash. */
        Map<Integer, Integer> denominations,

        @Size(max = 255, message = "Lý do chênh lệch tối đa 255 ký tự")
        String differenceReason,

        boolean bankReconciled,

        /** Tổng tiền về trong app ngân hàng; null thì coi như khớp với hệ thống. */
        @PositiveOrZero(message = "Tổng tiền trong ứng dụng ngân hàng phải từ 0 trở lên")
        @Digits(integer = 12, fraction = 0, message = "Tổng tiền trong ứng dụng ngân hàng phải là số nguyên")
        BigDecimal bankTotal,

        @Size(max = 255, message = "Lý do lệch chuyển khoản tối đa 255 ký tự")
        String transferDifferenceReason,

        /** Số tờ để lại két theo mệnh giá; mỗi mệnh giá ≤ số tờ đã đếm, tổng = keepCash. */
        Map<Integer, Integer> keepDenominations,

        @NotNull(message = "Vui lòng nhập tiền để lại két")
        @PositiveOrZero(message = "Tiền để lại két phải từ 0 trở lên")
        @Digits(integer = 12, fraction = 0, message = "Tiền để lại két phải là số nguyên tối đa 12 chữ số")
        BigDecimal keepCash,

        @Size(max = 255, message = "Lý do kết ca thay tối đa 255 ký tự")
        String closeOnBehalfReason
) {
}
