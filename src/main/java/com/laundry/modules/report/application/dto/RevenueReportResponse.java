package com.laundry.modules.report.application.dto;

import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.report.domain.Granularity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Báo cáo doanh thu (BM11) theo ngày hoặc tháng, tách theo phương thức thanh toán")
public record RevenueReportResponse(
        @Schema(description = "Ngày bắt đầu", example = "2026-10-01")
        LocalDate from,

        @Schema(description = "Ngày kết thúc", example = "2026-10-31")
        LocalDate to,

        @Schema(description = "Đơn vị gom nhóm", example = "DAY")
        Granularity granularity,

        @Schema(description = "Doanh thu từng kỳ, đã điền 0 cho kỳ không có giao dịch")
        List<Row> rows,

        @Schema(description = "Tổng cộng toàn khoảng")
        Summary summary
) {

    @Schema(description = "Doanh thu của một kỳ")
    public record Row(
            @Schema(description = "Nhãn kỳ: yyyy-MM-dd (DAY) hoặc yyyy-MM (MONTH)", example = "2026-10-01")
            String period,

            @Schema(description = "Doanh thu tiền mặt")
            BigDecimal cash,

            @Schema(description = "Doanh thu chuyển khoản")
            BigDecimal bankTransfer,

            @Schema(description = "Doanh thu đơn online")
            BigDecimal online,

            @Schema(description = "Doanh thu đơn tại quầy")
            BigDecimal offline,

            @Schema(description = "Tổng doanh thu của kỳ")
            BigDecimal total,

            @Schema(description = "Số giao dịch trong kỳ")
            long paymentCount
    ) {}

    @Schema(description = "Tổng doanh thu toàn khoảng")
    public record Summary(
            BigDecimal cash,
            BigDecimal bankTransfer,
            BigDecimal total,
            long paymentCount,

            @Schema(description = "Số giao dịch tiền mặt")
            long cashCount,

            @Schema(description = "Số giao dịch chuyển khoản")
            long bankTransferCount,

            @Schema(description = "Tiền thực thu theo kênh của đơn, luôn đủ ONLINE và OFFLINE, bỏ qua bộ lọc kênh")
            List<ChannelRevenue> byChannel
    ) {}

    @Schema(description = "Doanh thu của một kênh")
    public record ChannelRevenue(OrderChannel channel, BigDecimal total, long paymentCount) {}
}
