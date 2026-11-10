package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Số liệu dashboard Admin (WF-B01)")
public record DashboardSummaryResponse(
        LocalDate date,
        @Schema(description = "Số đơn nhận hôm nay")
        long ordersReceivedToday,
        @Schema(description = "Tiền thu hôm nay (giao dịch đã xác nhận)")
        BigDecimal revenueToday,
        long paymentsToday,
        @Schema(description = "Đơn đang trễ hẹn (chưa hoàn thành mà đã quá ngày hẹn)")
        long lateOrders,
        @Schema(description = "Đồ tồn: đơn hoàn thành quá OVERDUE_DAYS ngày chưa lấy")
        long overdueStorage,
        long receivedOrders,
        long processingOrders,
        @Schema(description = "Đơn hoàn thành, chờ khách lấy")
        long waitingPickupOrders,
        @Schema(description = "Phiếu sự cố chờ duyệt")
        long pendingIncidents
) {
}
