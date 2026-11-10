package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.VoucherStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeVoucherStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        VoucherStatus status
) {
}
