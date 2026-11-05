package com.laundry.modules.report.presentation;

import com.laundry.modules.report.application.ReportService;
import com.laundry.modules.report.application.dto.DashboardResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tham số ngày và granularity nhận dạng chuỗi rồi tự kiểm tra trong service: lỗi chuyển kiểu của Spring
 * sẽ rơi vào handler Exception.class của GlobalExceptionHandler và trả 500 thay vì 400.
 */
@Tag(name = "Report Management", description = "API báo cáo dành cho ADMIN")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    ReportService reportService;

    @Operation(
            summary = "Báo cáo doanh thu (BM11)",
            description = "Doanh thu theo ngày hoặc tháng, tách tiền mặt và chuyển khoản. "
                    + "Chỉ tính giao dịch đã xác nhận (CONFIRMED), theo thời điểm thanh toán, giờ Việt Nam; "
                    + "giao dịch đã hủy hoặc hoàn tiền không được tính. Khoảng tối đa 366 ngày."
    )
    @GetMapping("/revenue")
    @ResponseStatus(HttpStatus.OK)
    public RevenueReportResponse getRevenueReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to,
            @Parameter(description = "DAY hoặc MONTH, mặc định DAY", example = "DAY")
            @RequestParam(required = false) String granularity) {
        return reportService.getRevenueReport(from, to, granularity);
    }

    @Operation(
            summary = "Dashboard trong ngày",
            description = "Số liệu của ngày hôm nay theo giờ Việt Nam. orders: đơn nhận hôm nay (gồm đã hủy), "
                    + "số đã hủy trong đó, đơn giao hôm nay; waiting / processing / readyForPickup là số đơn "
                    + "hiện đang ở Mới nhận / Đang xử lý / Hoàn thành chờ lấy (không giới hạn trong ngày). "
                    + "revenue: giao dịch đã xác nhận có thời điểm thanh toán hôm nay, cùng cách tính với báo cáo doanh thu. "
                    + "alerts.lateOrders: đơn Mới nhận hoặc Đang xử lý đã quá ngày hẹn; alerts.storedOrders: đơn Hoàn thành "
                    + "quá storedAfterDays ngày (cấu hình OVERDUE_DAYS) mà khách chưa lấy, cùng định nghĩa với danh sách đồ tồn."
    )
    @GetMapping("/dashboard")
    @ResponseStatus(HttpStatus.OK)
    public DashboardResponse getDashboard() {
        return reportService.getDashboard();
    }

    @Operation(
            summary = "Báo cáo đơn hàng và dịch vụ (BM12)",
            description = "Số đơn theo trạng thái, tỉ lệ trễ và doanh thu theo dịch vụ, tính trên các đơn nhận "
                    + "trong khoảng ngày (giờ Việt Nam). Đơn trễ: đơn không hủy mà thời điểm hoàn thành "
                    + "(hoặc hiện tại nếu chưa hoàn thành) vượt ngày hẹn. Doanh thu theo dịch vụ là tổng thành tiền "
                    + "các dòng đơn, chưa gồm giảm giá và phụ phí cấp đơn. Khoảng tối đa 366 ngày."
    )
    @GetMapping("/orders")
    @ResponseStatus(HttpStatus.OK)
    public OrderReportResponse getOrderReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to) {
        return reportService.getOrderReport(from, to);
    }
}
