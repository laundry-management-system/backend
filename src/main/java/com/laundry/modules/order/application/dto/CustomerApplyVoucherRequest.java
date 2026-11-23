package com.laundry.modules.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerApplyVoucherRequest(
        @NotBlank(message = "Nhập mã voucher")
        @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
        String voucherCode
) {
}
