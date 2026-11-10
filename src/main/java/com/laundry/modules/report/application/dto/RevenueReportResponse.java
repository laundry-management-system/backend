package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Báo cáo doanh thu (BM11 / QĐ13): chỉ giao dịch đã xác nhận, theo thời điểm thanh toán, bỏ đơn đã hủy")
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        @Schema(description = "DAY hoặc MONTH")
        String groupBy,
        BigDecimal totalRevenue,
        long totalPayments,
        @Schema(description = "Tổng theo phương thức thanh toán")
        List<MethodTotal> byMethod,
        @Schema(description = "Theo từng ngày / tháng (chỉ các kỳ có phát sinh)")
        List<Period> periods
) {
    public record MethodTotal(String method, long payments, BigDecimal amount) {
    }

    /** {@code period}: yyyy-MM-dd khi nhóm theo ngày, yyyy-MM khi nhóm theo tháng. */
    public record Period(String period, long payments, BigDecimal amount, Map<String, BigDecimal> byMethod) {
    }
}
