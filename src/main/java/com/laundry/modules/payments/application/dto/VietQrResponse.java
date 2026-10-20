package com.laundry.modules.payments.application.dto;

import java.math.BigDecimal;

public record VietQrResponse(
        String orderCode,
        BigDecimal amount,
        String bankId,
        String accountNo,
        String accountName,
        String transferContent,
        String qrImageUrl
) {
}
