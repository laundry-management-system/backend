package com.laundry.modules.report.presentation;

import com.laundry.modules.report.application.ReportService;
import com.laundry.modules.report.application.dto.DashboardSummaryResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Report", description = "Báo cáo doanh thu, đơn hàng & dịch vụ, dashboard (UC26, UC27 / BM11, BM12)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Báo cáo doanh thu theo ngày / tháng và phương thức thanh toán (WF-I01)")
    @GetMapping("/reports/revenue")
    public RevenueReportResponse revenue(@RequestParam LocalDate from,
                                         @RequestParam LocalDate to,
                                         @RequestParam(required = false) ReportService.GroupBy groupBy) {
        return reportService.revenue(from, to, groupBy);
    }

    @Operation(summary = "Báo cáo đơn hàng & dịch vụ (WF-I02)")
    @GetMapping("/reports/orders")
    public OrderReportResponse orders(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return reportService.orders(from, to);
    }

    @Operation(summary = "Số liệu dashboard Admin hôm nay (WF-B01)")
    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse dashboard() {
        return reportService.dashboard();
    }
}
