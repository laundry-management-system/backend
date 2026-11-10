package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Báo cáo đơn hàng & dịch vụ (BM12 / QĐ14) cho các đơn nhận trong khoảng ngày")
public record OrderReportResponse(
        LocalDate from,
        LocalDate to,
        long totalOrders,
        long cancelledOrders,
        @Schema(description = "Đơn trễ hẹn: hoàn thành sau ngày hẹn, hoặc chưa xong mà đã quá hạn")
        long lateOrders,
        @Schema(description = "Số đơn theo trạng thái hiện tại")
        Map<String, Long> byStatus,
        @Schema(description = "Số đơn và giá trị trung bình theo loại đơn (không tính đơn hủy)")
        List<OrderTypeStat> byOrderType,
        @Schema(description = "Doanh thu theo dịch vụ (đơn đã thanh toán)")
        List<ServiceRevenue> byService,
        @Schema(description = "Phụ phí và giảm giá (đơn đã thanh toán)")
        Surcharges surcharges
) {
    public record OrderTypeStat(String orderType, long orders, BigDecimal totalValue, BigDecimal averageValue) {
    }

    public record ServiceRevenue(Long serviceId, String serviceName, long orders, BigDecimal quantity,
                                 BigDecimal revenue) {
    }

    public record Surcharges(BigDecimal expressFee, BigDecimal whitesFee, BigDecimal discount) {
    }
}
