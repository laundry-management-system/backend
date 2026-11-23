package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dashboard theo kỳ (giờ Việt Nam): số đơn, doanh thu, đơn trễ, đồ tồn, so với kỳ liền trước")
public record DashboardResponse(
        @Schema(description = "Ngày đầu kỳ", example = "2026-11-10")
        LocalDate from,

        @Schema(description = "Ngày cuối kỳ (gồm cả ngày này)", example = "2026-11-16")
        LocalDate to,

        Orders orders,

        @Schema(description = "Doanh thu trong kỳ, cùng cách tính với báo cáo doanh thu (BM11)")
        RevenueReportResponse.Summary revenue,

        Alerts alerts,

        Previous previous
) {

    @Schema(description = "Số đơn")
    public record Orders(
            @Schema(description = "Đơn nhận trong kỳ, gồm cả đơn đã hủy")
            long received,

            @Schema(description = "Trong số đơn nhận trong kỳ, số đã hủy")
            long cancelled,

            @Schema(description = "Đơn giao cho khách trong kỳ")
            long delivered,

            @Schema(description = "Số đơn hiện đang ở trạng thái Mới nhận (chưa xử lý), không giới hạn trong ngày")
            long waiting,

            @Schema(description = "Số đơn hiện đang ở trạng thái Đang xử lý, không giới hạn trong ngày")
            long processing,

            @Schema(description = "Số đơn hiện đang ở trạng thái Hoàn thành, chờ khách đến lấy, không giới hạn trong ngày")
            long readyForPickup,

            @Schema(description = "Số đơn online hiện ở Chờ xác nhận")
            long pendingConfirmation,

            @Schema(description = "Số đơn online hiện ở Chờ nhận đồ")
            long awaitingDropoff
    ) {}

    @Schema(description = "Cảnh báo cần xử lý")
    public record Alerts(
            @Schema(description = "Đơn đang trễ: trạng thái Mới nhận hoặc Đang xử lý mà đã quá ngày hẹn trả")
            long lateOrders,

            @Schema(description = "Đồ tồn: đơn Hoàn thành quá storedAfterDays ngày mà khách chưa đến lấy")
            long storedOrders,

            @Schema(description = "Số ngày dùng để xác định đồ tồn (cấu hình OVERDUE_DAYS)", example = "7")
            int storedAfterDays
    ) {}

    @Schema(description = "Kỳ liền trước cùng số ngày, để so sánh")
    public record Previous(
            LocalDate from,
            LocalDate to,
            @Schema(description = "Đơn nhận trong kỳ trước, gồm cả đơn đã hủy") long received,
            @Schema(description = "Tổng doanh thu kỳ trước") BigDecimal revenueTotal
    ) {}
}
