package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Báo cáo khuyến mãi: lượt dùng, giảm giá và doanh thu theo mã voucher")
public record PromotionReportResponse(
        LocalDate from,
        LocalDate to,

        @Schema(description = "Tổng lượt dùng = Σ byVoucher.uses")
        long uses,

        @Schema(description = "Tổng giảm giá = Σ byVoucher.discountTotal")
        BigDecimal discountTotal,

        @Schema(description = "Tổng doanh thu = Σ byVoucher.revenue")
        BigDecimal revenue,

        @Schema(description = "Theo mã voucher đã lưu trên đơn, sắp revenue rồi uses giảm dần")
        List<VoucherRow> byVoucher
) {

    @Schema(description = "Hiệu quả của một mã voucher")
    public record VoucherRow(
            String voucherCode,
            @Schema(description = "MAX(orders.voucher_id), có thể null")
            Long voucherId,
            long uses,
            BigDecimal discountTotal,
            BigDecimal revenue
    ) {}
}
