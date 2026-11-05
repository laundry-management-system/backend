package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Báo cáo đơn hàng và doanh thu theo dịch vụ , tính trên các đơn nhận trong khoảng ngày")
public record OrderReportResponse(
        @Schema(description = "Ngày bắt đầu", example = "2026-10-01")
        LocalDate from,

        @Schema(description = "Ngày kết thúc", example = "2026-10-31")
        LocalDate to,

        Summary summary,

        @Schema(description = "Số đơn theo từng trạng thái (đủ mọi trạng thái, không có thì 0)")
        List<StatusCount> byStatus,

        @Schema(description = "Doanh thu theo dịch vụ, sắp giảm dần theo doanh thu")
        List<ServiceRevenue> byService
) {

    @Schema(description = "Tổng hợp đơn hàng trong kỳ")
    public record Summary(
            @Schema(description = "Tổng số đơn nhận trong kỳ, gồm cả đơn đã hủy")
            long totalOrders,

            @Schema(description = "Số đơn đã hủy")
            long cancelledOrders,

            @Schema(description = "Số đơn không bị hủy (cơ sở tính tỉ lệ trễ)")
            long activeOrders,

            @Schema(description = "Số đơn trễ: đơn không hủy mà thời điểm hoàn thành (hoặc hiện tại nếu chưa hoàn thành) vượt ngày hẹn")
            long lateOrders,

            @Schema(description = "Tỉ lệ trễ (%) = lateOrders / activeOrders, 2 chữ số thập phân", example = "12.50")
            BigDecimal lateRatePercent,

            @Schema(description = "Tổng tiền các đơn không hủy (đã gồm giảm giá, phụ phí)")
            BigDecimal ordersTotalAmount,

            @Schema(description = "Tổng thành tiền các dòng dịch vụ (chưa gồm giảm giá, phụ phí cấp đơn)")
            BigDecimal itemsRevenue
    ) {}

    @Schema(description = "Số đơn của một trạng thái")
    public record StatusCount(
            String status,
            String description,
            long count
    ) {}

    @Schema(description = "Doanh thu của một dịch vụ")
    public record ServiceRevenue(
            @Schema(description = "Id dịch vụ, null với dòng không gắn dịch vụ")
            Long serviceId,

            String serviceCode,

            @Schema(description = "Tên dịch vụ; dòng không gắn dịch vụ có tên \"Khác\"")
            String serviceName,

            @Schema(description = "Số đơn có dùng dịch vụ này")
            long orderCount,

            @Schema(description = "Tổng thành tiền các dòng của dịch vụ")
            BigDecimal revenue
    ) {}
}
