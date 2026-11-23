package com.laundry.modules.order.presentation;

import com.laundry.modules.order.application.OrderService;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.CompleteDropoffRequest;
import com.laundry.modules.order.application.dto.StartDeliveryRequest;
import com.laundry.modules.order.application.dto.UndoDeliveryRequest;
import com.laundry.modules.order.application.dto.UpdateTrackingCodesRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.ApplyOrderVoucherRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.CustomerTrackingResponse;
import com.laundry.modules.order.application.dto.IntakeRequest;
import com.laundry.modules.order.application.dto.IntakeResponse;
import com.laundry.modules.order.application.dto.OrderDetailResponse;
import com.laundry.modules.order.application.dto.OrderFilter;
import com.laundry.modules.order.application.dto.OrderImageDto;
import com.laundry.modules.order.application.dto.OrderReceiptResponse;
import com.laundry.modules.order.application.dto.OrderResponse;
import com.laundry.modules.order.application.dto.RejectOrderRequest;
import com.laundry.modules.order.application.dto.RejectPaymentClaimRequest;
import com.laundry.modules.order.application.dto.RescheduleAppointmentRequest;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.application.dto.WorkCountsResponse;
import com.laundry.modules.order.domain.OrderChannel;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order Management", description = "Quản lý đơn hàng giặt ủi (M3 - Thành viên 3: UC04-UC09, UC21, UC22)")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Tiếp nhận đồ v3: tạo lượt nhận 1–2 đơn (Tiêu chuẩn / Giặt nhanh), chưa có dòng đồ và giá")
    @PostMapping("/intake")
    public ResponseEntity<IntakeResponse> createIntake(
            @Valid @RequestBody IntakeRequest request,
            Authentication authentication
    ) {
        IntakeResponse result = orderService.createIntake(request, getUsername(authentication));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @Operation(summary = "In phiếu nhận đồ 2 liên kèm mã QR tra cứu (BM3)")
    @GetMapping("/{id}/receipt")
    public OrderReceiptResponse getReceipt(
            @PathVariable Long id,
            @RequestParam(required = false) String webUrl,
            HttpServletRequest request
    ) {
        String baseUrl = (webUrl != null && !webUrl.isBlank()) ? webUrl : request.getHeader("Origin");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:5173";
        }
        return orderService.getReceipt(id, baseUrl);
    }

    @Operation(summary = "Nhập danh sách chi tiết các món đồ sau khi kiểm đếm (UC04)")
    @PutMapping("/{id}/items")
    public OrderDetailResponse updateItems(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderItemsRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.updateItems(id, request, staff);
    }

    @Operation(summary = "Áp / đổi / gỡ voucher ở bước thanh toán (đơn đã hoàn thành, chưa thu tiền)")
    @PutMapping("/{id}/voucher")
    public OrderDetailResponse applyVoucher(
            @PathVariable Long id,
            @Valid @RequestBody ApplyOrderVoucherRequest request,
            Authentication authentication
    ) {
        return orderService.applyVoucher(id, request, getUsername(authentication));
    }

    @Operation(summary = "Đính kèm ảnh tình trạng món đồ có vấn đề (ố, rách - UC04 extend)")
    @PostMapping("/{orderId}/items/{itemId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderImageDto addOrderItemImage(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody AddOrderItemImageRequest request
    ) {
        return orderService.addOrderItemImage(orderId, itemId, request);
    }

    @Operation(summary = "Khách hàng tra cứu tiến độ đơn qua Số điện thoại (UC29 / BM6 - Public)")
    @GetMapping("/track")
    public List<CustomerTrackingResponse> trackByPhone(@RequestParam String phone) {
        return orderService.trackByPhone(phone);
    }

    @Operation(
            summary = "Xem danh sách công việc & lọc đơn (UC07 / BM4)",
            description = "statuses: nhiều trạng thái, phân cách dấu phẩy. classified=false: đơn chưa có dòng đồ. "
                    + "late=true: đơn Mới nhận / Đang xử lý đã quá hẹn. dueFrom/dueTo (yyyy-MM-dd, giờ VN, gồm cả hai đầu): "
                    + "lọc theo ngày hẹn trả. Sắp xếp nhiều khóa: sort=dueAt,asc&sort=receivedAt,asc."
    )
    @GetMapping
    public Page<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) Long washModeId,
            @RequestParam(required = false) Boolean classified,
            @RequestParam(required = false) Boolean late,
            @RequestParam(required = false) String dueFrom,
            @RequestParam(required = false) String dueTo,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String intakeCode,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        OrderFilter filter = OrderFilter.of(status, statuses, channel, priority, phone, search, intakeCode,
                washModeId, classified, late, dueFrom, dueTo);
        return orderService.getOrders(filter, pageable);
    }

    @Operation(
            summary = "Số đơn theo tab của Danh sách công việc",
            description = "Đếm trên đơn Mới nhận + Đang xử lý, cùng bộ lọc từ khóa (search/phone) và ngày hẹn "
                    + "(dueFrom/dueTo) với GET /orders. Các tab chồng nhau, không cộng thành tổng."
    )
    @GetMapping("/work-counts")
    public WorkCountsResponse getWorkCounts(
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dueFrom,
            @RequestParam(required = false) String dueTo
    ) {
        return orderService.getWorkCounts(
                OrderFilter.of(null, null, channel, null, phone, search, null, null, null, null, dueFrom, dueTo));
    }

    @Operation(summary = "Xem chi tiết đầy đủ đơn hàng (BM3, BM5)")
    @GetMapping("/{id}")
    public OrderDetailResponse getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @Operation(summary = "Cập nhật trạng thái đơn (UC08 / BM5)")
    @PatchMapping("/{id}/status")
    public OrderDetailResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.changeStatus(id, request, staff);
    }

    @Operation(summary = "Hoàn thành xử lý đồ & gán vị trí kệ (UC08 / QĐ6)")
    @PostMapping("/{id}/complete")
    public OrderDetailResponse completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody CompleteOrderRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.completeOrder(id, request, staff);
    }

    @Operation(summary = "Lùi trạng thái đơn kèm lý do bắt buộc (UC22 / BR-D06)")
    @PostMapping("/{id}/revert-status")
    public OrderDetailResponse revertStatus(
            @PathVariable Long id,
            @Valid @RequestBody RevertStatusRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.revertStatus(id, request, staff);
    }

    @Operation(summary = "Dời ngày hẹn trả kèm lý do bắt buộc (UC06 / BR-D08)")
    @PatchMapping("/{id}/reschedule")
    public OrderDetailResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleOrderRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.reschedule(id, request, staff);
    }

    @Operation(summary = "Sửa đơn giá / tổng tiền đơn kèm lý do bắt buộc (UC21 / BR-D12)")
    @PatchMapping("/{id}/price")
    public OrderDetailResponse adjustPrice(
            @PathVariable Long id,
            @Valid @RequestBody AdjustPriceRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.adjustPrice(id, request, staff);
    }

    @Operation(summary = "Hủy đơn hàng khi Mới nhận kèm lý do (UC05 / BR-D05)")
    @PostMapping("/{id}/cancel")
    public OrderDetailResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.cancel(id, request, staff);
    }

    @Operation(summary = "Xác nhận đơn online (Chờ xác nhận -> Chờ nhận đồ)")
    @PostMapping("/{id}/confirm")
    public OrderDetailResponse confirm(@PathVariable Long id, Authentication authentication) {
        return orderService.confirm(id, getUsername(authentication));
    }

    @Operation(summary = "Từ chối đơn online ở Chờ xác nhận kèm lý do gửi khách")
    @PostMapping("/{id}/reject")
    public OrderDetailResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectOrderRequest request,
            Authentication authentication
    ) {
        return orderService.reject(id, request, getUsername(authentication));
    }

    @Operation(summary = "Từ chối báo thanh toán của khách kèm lý do")
    @PostMapping("/{id}/payment-claim/reject")
    public OrderDetailResponse rejectPaymentClaim(
            @PathVariable Long id,
            @Valid @RequestBody RejectPaymentClaimRequest request,
            Authentication authentication
    ) {
        return orderService.rejectPaymentClaim(id, request.reason(), getUsername(authentication));
    }

    @Operation(summary = "Sửa giờ hẹn gửi / giờ hẹn nhận của đơn chưa nhận đồ")
    @PatchMapping("/{id}/appointment")
    public OrderDetailResponse rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request,
            Authentication authentication
    ) {
        return orderService.rescheduleAppointment(id, request, getUsername(authentication));
    }

    @Operation(summary = "Đã nhận đồ khách mang tới / shipper lấy về")
    @PostMapping("/{id}/fulfillment/dropoff")
    public OrderDetailResponse completeDropoff(
            @PathVariable Long id,
            @Valid @RequestBody CompleteDropoffRequest request,
            Authentication authentication
    ) {
        return orderService.completeDropoff(id, request, getUsername(authentication));
    }

    @Operation(summary = "Chuyển đơn hoàn thành sang Đang giao")
    @PostMapping("/{id}/fulfillment/start-delivery")
    public OrderDetailResponse startDelivery(
            @PathVariable Long id,
            @Valid @RequestBody StartDeliveryRequest request,
            Authentication authentication
    ) {
        return orderService.startDelivery(id, request, getUsername(authentication));
    }

    @Operation(summary = "Hoàn tác Đang giao về Chờ giao")
    @PostMapping("/{id}/fulfillment/undo-delivery")
    public OrderDetailResponse undoStartDelivery(
            @PathVariable Long id,
            @Valid @RequestBody UndoDeliveryRequest request,
            Authentication authentication
    ) {
        return orderService.undoStartDelivery(id, request, getUsername(authentication));
    }

    @Operation(summary = "Sửa mã vận đơn")
    @PatchMapping("/{id}/fulfillment/tracking")
    public OrderDetailResponse updateTrackingCodes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrackingCodesRequest request,
            Authentication authentication
    ) {
        return orderService.updateTrackingCodes(id, request, getUsername(authentication));
    }

    private String getUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "STAFF";
        }
        return authentication.getName();
    }
}
