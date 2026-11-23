package com.laundry.modules.report.presentation;

import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.report.application.ReportService;
import com.laundry.modules.report.application.dto.CustomerReportResponse;
import com.laundry.modules.report.application.dto.OperationsReportResponse;
import com.laundry.modules.report.application.dto.OrderReportResponse;
import com.laundry.modules.report.application.dto.OverviewReportResponse;
import com.laundry.modules.report.application.dto.PromotionReportResponse;
import com.laundry.modules.report.application.dto.RevenuePaymentResponse;
import com.laundry.modules.report.application.dto.RevenueReportResponse;
import com.laundry.modules.report.domain.ReportPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
            @RequestParam(required = false) String granularity,
            @Parameter(description = "ONLINE hoặc OFFLINE; bỏ trống = tất cả", example = "ONLINE")
            @RequestParam(required = false) OrderChannel channel) {
        return reportService.getRevenueReport(from, to, granularity, channel);
    }

    @Operation(
            summary = "Giao dịch tạo nên doanh thu",
            description = "Các giao dịch đã xác nhận có thời điểm thanh toán trong khoảng ngày (giờ Việt Nam), cùng điều kiện "
                    + "với báo cáo doanh thu nên tổng số tiền khớp với báo cáo. Mới nhất trước, tối đa 1000 dòng."
    )
    @GetMapping("/revenue/payments")
    @ResponseStatus(HttpStatus.OK)
    public List<RevenuePaymentResponse> getRevenuePayments(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-11-05")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-11-05")
            @RequestParam(required = false) String to,
            @Parameter(description = "ONLINE hoặc OFFLINE; bỏ trống = tất cả", example = "ONLINE")
            @RequestParam(required = false) OrderChannel channel) {
        return reportService.getRevenuePayments(from, to, channel);
    }

    @Operation(
            summary = "Tổng quan",
            description = "from/to (yyyy-MM-dd, giờ Việt Nam, gồm cả hai đầu); bỏ cả hai = hôm nay, tối đa 366 ngày. "
                    + "live: số đơn hiện tại theo trạng thái, đơn trễ, đồ tồn, đơn sắp đến hạn (không theo kỳ). "
                    + "period / previous: số đơn nhận, doanh thu, khách mới, sự cố của kỳ và kỳ liền trước cùng số ngày."
    )
    @GetMapping("/overview")
    @ResponseStatus(HttpStatus.OK)
    public OverviewReportResponse getOverview(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", example = "2026-11-10")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", example = "2026-11-16")
            @RequestParam(required = false) String to) {
        return reportService.getOverview(from, to);
    }

    @Operation(
            summary = "Báo cáo đơn hàng và dịch vụ (BM12)",
            description = "Số đơn theo trạng thái, tỉ lệ trễ và doanh thu theo dịch vụ, tính trên các đơn nhận "
                    + "trong khoảng ngày (giờ Việt Nam). Đơn trễ: đơn không hủy mà thời điểm hoàn thành "
                    + "(hoặc hiện tại nếu chưa hoàn thành) vượt ngày hẹn. Doanh thu theo dịch vụ là tổng thành tiền "
                    + "các dòng đơn, chưa gồm giảm giá và phụ phí cấp đơn; kèm tổng khối lượng / số lượng và đơn vị. "
                    + "rows: số đơn nhận theo ngày hoặc tháng (granularity). Khoảng tối đa 366 ngày."
    )
    @GetMapping("/orders")
    @ResponseStatus(HttpStatus.OK)
    public OrderReportResponse getOrderReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to,
            @Parameter(description = "DAY hoặc MONTH, mặc định DAY", example = "DAY")
            @RequestParam(required = false) String granularity,
            @Parameter(description = "ONLINE hoặc OFFLINE; bỏ trống = tất cả", example = "ONLINE")
            @RequestParam(required = false) OrderChannel channel) {
        return reportService.getOrderReport(from, to, granularity, channel);
    }

    @Operation(
            summary = "Báo cáo vận hành",
            description = "Số đơn theo nhân viên, tổng hợp và danh sách ca làm đã đóng, sự cố theo kỳ/loại/trạng thái "
                    + "và tiền chi sự cố trong khoảng ngày (giờ Việt Nam), kèm số sự cố của kỳ liền trước."
    )
    @GetMapping("/operations")
    @ResponseStatus(HttpStatus.OK)
    public OperationsReportResponse getOperationsReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to,
            @Parameter(description = "DAY hoặc MONTH, mặc định DAY", example = "DAY")
            @RequestParam(required = false) String granularity) {
        return reportService.getOperationsReport(from, to, granularity);
    }

    @Operation(
            summary = "Báo cáo khách hàng",
            description = "Khách mới, khách quay lại, tỉ lệ quay lại, số đăng ký và top 10 khách chi tiêu nhiều nhất "
                    + "trong khoảng ngày (giờ Việt Nam), kèm số khách mới của kỳ liền trước. Khoảng tối đa 366 ngày."
    )
    @GetMapping("/customers")
    @ResponseStatus(HttpStatus.OK)
    public CustomerReportResponse getCustomerReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to) {
        return reportService.getCustomerReport(from, to);
    }

    @Operation(
            summary = "Chi tiêu của mọi khách (xuất CSV)",
            description = "Mọi khách có thanh toán trong khoảng ngày, không giới hạn 10 dòng như top khách; "
                    + "tối đa 50.000 dòng. Chỉ dùng khi xuất CSV."
    )
    @GetMapping("/customers/spending")
    @ResponseStatus(HttpStatus.OK)
    public List<CustomerReportResponse.CustomerSpendingRow> getCustomerSpending(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to) {
        return reportService.getCustomerSpending(from, to);
    }

    @Operation(
            summary = "Báo cáo khuyến mãi",
            description = "Lượt dùng, tổng giảm giá và doanh thu theo mã voucher đã lưu trên đơn, trong khoảng ngày "
                    + "(giờ Việt Nam). Sắp theo doanh thu giảm dần, rồi lượt dùng. Khoảng tối đa 366 ngày."
    )
    @GetMapping("/promotions")
    @ResponseStatus(HttpStatus.OK)
    public PromotionReportResponse getPromotionReport(
            @Parameter(description = "Ngày bắt đầu, dạng yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ngày kết thúc (gồm cả ngày này), dạng yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) String to,
            @Parameter(description = "ONLINE hoặc OFFLINE; bỏ trống = tất cả", example = "ONLINE")
            @RequestParam(required = false) OrderChannel channel) {
        return reportService.getPromotionReport(from, to, channel);
    }
}
