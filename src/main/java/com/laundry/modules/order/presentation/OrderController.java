package com.laundry.modules.order.presentation;

import com.laundry.modules.order.application.OrderService;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.CustomerTrackingResponse;
import com.laundry.modules.order.application.dto.DeliverOrderRequest;
import com.laundry.modules.order.application.dto.EstimateReturnTimeRequest;
import com.laundry.modules.order.application.dto.EstimateReturnTimeResponse;
import com.laundry.modules.order.application.dto.OrderDetailResponse;
import com.laundry.modules.order.application.dto.OrderImageDto;
import com.laundry.modules.order.application.dto.OrderReceiptResponse;
import com.laundry.modules.order.application.dto.OrderResponse;
import com.laundry.modules.order.application.dto.QuickCreateOrderRequest;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderStatus;
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

import java.util.List;

@Tag(name = "Order Management", description = "Quản lý đơn hàng giặt ủi (M3 - Thành viên 3: UC04-UC09, UC21, UC22)")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Tiếp nhận đồ & tạo đơn (UC04 / BM3 - Fast Check-in)")
    @PostMapping("/quick-create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse quickCreate(
            @Valid @RequestBody QuickCreateOrderRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.quickCreate(request, staff);
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

    @Operation(summary = "Tính tiền & gợi ý ngày hẹn (UC04 - include)")
    @PostMapping("/estimate-return-time")
    public EstimateReturnTimeResponse estimateReturnTime(
            @Valid @RequestBody EstimateReturnTimeRequest request
    ) {
        return orderService.estimateReturnTime(request);
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

    @Operation(summary = "Xem danh sách công việc & lọc đơn (UC07 / BM4)")
    @GetMapping
    public Page<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return orderService.getOrders(status, priority, phone, search, pageable);
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

    @Operation(summary = "Giao đồ cho khách hoặc người nhận hộ (UC12 / QĐ9)")
    @PostMapping("/{id}/deliver")
    public OrderDetailResponse deliverOrder(
            @PathVariable Long id,
            @Valid @RequestBody DeliverOrderRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return orderService.deliverOrder(id, request, staff);
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

    private String getUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "STAFF";
        }
        return authentication.getName();
    }
}
