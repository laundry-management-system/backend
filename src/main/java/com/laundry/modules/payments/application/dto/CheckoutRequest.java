package com.laundry.modules.payments.application.dto;

import com.laundry.modules.payments.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Thu tiền và giao đồ trong một lần. Đơn đã thanh toán trước thì chỉ giao, bỏ qua phần thu tiền")
public record CheckoutRequest(
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên tối đa 12 chữ số")
        BigDecimal amount,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        @Size(max = 50, message = "Mã tham chiếu tối đa 50 ký tự")
        String referenceCode,

        @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
        String receiverName,

        @Size(max = 50, message = "Quan hệ với khách tối đa 50 ký tự")
        String receiverRelation
) {
}
