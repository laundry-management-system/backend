package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Báo cáo khách hàng: khách mới, khách quay lại, top khách chi tiêu")
public record CustomerReportResponse(
        LocalDate from,
        LocalDate to,

        @Schema(description = "Khách có đơn nhận trong kỳ mà chưa có đơn nào trước kỳ")
        long newCustomers,

        @Schema(description = "Khách có đơn nhận trong kỳ và đã có đơn trước kỳ")
        long returningCustomers,

        @Schema(description = "Tỉ lệ quay lại (%) = returning / (new + returning), 1 chữ số thập phân; null khi mẫu 0", example = "62.5")
        BigDecimal returnRatePercent,

        @Schema(description = "Số đăng ký khách trong kỳ, không tính đơn bị từ chối")
        long registrations,

        Previous previous,

        @Schema(description = "Top khách, tối đa 10 dòng, sắp theo spent giảm dần rồi mã khách")
        List<CustomerSpendingRow> topCustomers
) {

    @Schema(description = "Số liệu kỳ liền trước")
    public record Previous(long newCustomers) {}

    @Schema(description = "Chi tiêu của một khách trong kỳ")
    public record CustomerSpendingRow(
            Long customerId,
            String customerCode,
            String customerName,
            @Schema(description = "Số đơn nhận trong kỳ")
            long orderCount,
            @Schema(description = "Tổng tiền đã thu (CONFIRMED) trong kỳ")
            BigDecimal spent,
            @Schema(description = "Thời điểm nhận đơn gần nhất")
            Instant lastOrderAt
    ) {}
}
