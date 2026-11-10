package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Dashboard trong ngày (giờ Việt Nam): số đơn, doanh thu, đơn trễ, đồ tồn")
public record DashboardResponse(
        @Schema(description = "Ngày của dashboard", example = "2026-11-04")
        LocalDate date,

        Orders orders,

        @Schema(description = "Doanh thu hôm nay, cùng cách tính với báo cáo doanh thu (BM11)")
        RevenueReportResponse.Summary revenue,

        Alerts alerts
) {

    @Schema(description = "Số đơn")
    public record Orders(
            @Schema(description = "Đơn nhận hôm nay, gồm cả đơn đã hủy")
            long receivedToday,

            @Schema(description = "Trong số đơn nhận hôm nay, số đã hủy")
            long cancelledToday,

            @Schema(description = "Đơn giao cho khách hôm nay")
            long deliveredToday,

            @Schema(description = "Số đơn hiện đang ở trạng thái Mới nhận (chưa xử lý), không giới hạn trong ngày")
            long waiting,

            @Schema(description = "Số đơn hiện đang ở trạng thái Đang xử lý, không giới hạn trong ngày")
            long processing,

            @Schema(description = "Số đơn hiện đang ở trạng thái Hoàn thành, chờ khách đến lấy, không giới hạn trong ngày")
            long readyForPickup
    ) {}

    @Schema(description = "Cảnh báo cần xử lý")
    public record Alerts(
            @Schema(description = "Đơn đang trễ: trạng thái Mới nhận hoặc Đang xử lý mà đã quá ngày hẹn trả")
            long lateOrders,

            @Schema(description = "Đồ tồn: đơn Hoàn thành quá storedAfterDays ngày mà khách chưa đến lấy")
            long storedOrders,

            @Schema(description = "Số ngày dùng để xác định đồ tồn (cấu hình OVERDUE_DAYS)", example = "7")
            int storedAfterDays,

            @Schema(description = "Phiếu sự cố đang chờ Admin duyệt")
            long pendingIncidents
    ) {}
}
