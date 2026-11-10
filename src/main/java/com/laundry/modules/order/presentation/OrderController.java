package com.laundry.modules.order.presentation;

import com.laundry.modules.order.application.OrderService;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.IntakeRequest;
import com.laundry.modules.order.application.dto.IntakeResponse;
import com.laundry.modules.order.application.dto.OrderDetailResponse;
import com.laundry.modules.order.application.dto.OrderImageDto;
import com.laundry.modules.order.application.dto.OrderReceiptResponse;
import com.laundry.modules.order.application.dto.OrderResponse;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.application.dto.OrderSearchFilter;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.PaymentStatus;
import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "Tiếp nhận đồ: tạo lượt nhận 1–2 đơn (Tiêu chuẩn / Giặt nhanh), chưa có dòng đồ và giá")
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

    @Operation(summary = "Phân loại & chốt giá: nhập dòng đồ, áp voucher (UC04)")
    @PutMapping("/{id}/items")
    public OrderDetailResponse updateItems(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderItemsRequest request,
            Authentication authentication
    ) {
        return orderService.updateItems(id, request, getUsername(authentication));
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

    @Operation(summary = "Xem danh sách công việc & lọc đơn (UC07 / BM4)",
            description = "phone: đủ 10 số thì khớp chính xác (đơn của một khách), ít hơn thì tìm gần đúng; "
                    + "search: tìm gần đúng theo mã đơn, tên, SĐT; "
                    + "receivedTo là mốc loại trừ; classified=false: đơn chưa phân loại")
    @GetMapping
    public Page<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String intakeCode,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Instant receivedFrom,
            @RequestParam(required = false) Instant receivedTo,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) Boolean classified,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return orderService.getOrders(new OrderSearchFilter(status, priority, phone, search, intakeCode, customerId,
                receivedFrom, receivedTo, paymentStatus, orderType, classified), pageable);
    }

    @Operation(summary = "Đếm đơn theo trạng thái, kèm UNCLASSIFIED (chưa phân loại) và LATE (trễ hẹn)")
    @GetMapping("/counts")
    public Map<String, Long> countOrders() {
        return orderService.countOrders();
    }

    @Operation(summary = "Xem chi tiết đầy đủ đơn hàng (BM3, BM5)")
    @GetMapping("/{id}")
    public OrderDetailResponse getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @Operation(summary = "Bắt đầu xử lý: Mới nhận → Đang xử lý (UC08). Các trạng thái khác dùng API riêng")
    @PatchMapping("/{id}/status")
    public OrderDetailResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication
    ) {
        return orderService.changeStatus(id, request, getUsername(authentication));
    }

    @Operation(summary = "Hoàn thành xử lý đồ & gán vị trí kệ (UC08 / QĐ6)")
    @PostMapping("/{id}/complete")
    public OrderDetailResponse completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody CompleteOrderRequest request,
            Authentication authentication
    ) {
        return orderService.completeOrder(id, request, getUsername(authentication));
    }

    @Operation(summary = "Lùi trạng thái đơn kèm lý do bắt buộc (Admin, UC22 / BR-D06)")
    @PostMapping("/{id}/revert-status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDetailResponse revertStatus(
            @PathVariable Long id,
            @Valid @RequestBody RevertStatusRequest request,
            Authentication authentication
    ) {
        return orderService.revertStatus(id, request, getUsername(authentication));
    }

    @Operation(summary = "Dời ngày hẹn trả kèm lý do bắt buộc (UC06 / BR-D08)")
    @PatchMapping("/{id}/reschedule")
    public OrderDetailResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleOrderRequest request,
            Authentication authentication
    ) {
        return orderService.reschedule(id, request, getUsername(authentication));
    }

    @Operation(summary = "Sửa tổng tiền đơn kèm lý do bắt buộc (Admin, UC21 / BR-D12). Gỡ voucher nếu có")
    @PatchMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDetailResponse adjustPrice(
            @PathVariable Long id,
            @Valid @RequestBody AdjustPriceRequest request,
            Authentication authentication
    ) {
        return orderService.adjustPrice(id, request, getUsername(authentication));
    }

    @Operation(summary = "Hủy đơn Mới nhận kèm lý do (UC05 / BR-D05). Đơn đã thanh toán được hoàn tiền, voucher được trả lượt")
    @PostMapping("/{id}/cancel")
    public OrderDetailResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request,
            Authentication authentication
    ) {
        return orderService.cancel(id, request, getUsername(authentication));
    }

    private String getUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "STAFF";
        }
        return authentication.getName();
    }
}
