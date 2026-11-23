package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.VoucherStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeVoucherStatusRequest(
        @NotNull(message = "Status must not be null")
        VoucherStatus status
) {}
