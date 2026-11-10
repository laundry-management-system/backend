package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.VoucherCheckReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Kết quả kiểm tra mã voucher cho ô áp mã (WF-C03)")
public record VoucherCheckResponse(
        @Schema(description = "true nếu áp được")
        boolean ok,
        @Schema(description = "Số tiền giảm dự kiến (khi ok)")
        BigDecimal discount,
        @Schema(description = "Lý do không áp được (khi không ok)")
        VoucherCheckReason reason,
        @Schema(description = "Câu thông báo tiếng Việt")
        String message,
        @Schema(description = "Thông tin voucher; null khi mã không tồn tại")
        VoucherResponse voucher
) {
}
