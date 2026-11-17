package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Áp voucher ở bước thanh toán; null/rỗng = gỡ voucher")
public record ApplyOrderVoucherRequest(
        @Size(max = 20, message = "Mã voucher tối đa 20 ký tự")
        String voucherCode
) {
}
