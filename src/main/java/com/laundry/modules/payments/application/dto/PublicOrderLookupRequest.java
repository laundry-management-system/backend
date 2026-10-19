package com.laundry.modules.payments.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicOrderLookupRequest(
        @NotBlank(message = "Mã đơn không được để trống")
        @Size(max = 15, message = "Mã đơn tối đa 15 ký tự")
        @Pattern(regexp = "^LD-[0-9]{8}-[0-9]{3}$", message = "Mã đơn không đúng định dạng")
        String orderCode,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String phone
) {
}
