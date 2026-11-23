package com.laundry.modules.report.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Tổng quan: số liệu hiện tại và số liệu kỳ so với kỳ liền trước")
public record OverviewReportResponse(
        LocalDate from,
        LocalDate to,
        Live live,
        Counts period,
        Counts previous
) {

    @Schema(description = "Số liệu hiện tại, không phụ thuộc kỳ")
    public record Live(
            long waiting,
            long processing,
            long readyForPickup,
            long pendingConfirmation,
            long awaitingDropoff,
            @Schema(description = "Đơn trễ hiện tại")
            long lateOrders,
            long storedOrders,
            int storedAfterDays,
            @Schema(description = "Đơn sắp đến hạn trong 24 giờ, tối đa 10")
            List<DueSoon> dueSoon,
            @Schema(description = "Đồ tồn, chờ lâu nhất trước, tối đa 10")
            List<Stored> stored
    ) {}

    public record DueSoon(Long orderId, String orderCode, String customerName, String status, Instant dueAt) {}

    public record Stored(Long orderId, String orderCode, String customerName, Instant readyAt, long daysWaiting) {}

    @Schema(description = "Bốn chỉ số so sánh theo kỳ")
    public record Counts(long receivedOrders, BigDecimal revenueTotal, long newCustomers, long newIncidents) {}
}
