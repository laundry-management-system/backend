package com.laundry.modules.order.presentation;

import com.laundry.modules.customer.application.CustomerAuthRateLimiter;
import com.laundry.modules.customer.application.CustomerSessionService;
import com.laundry.modules.order.application.OrderService;
import com.laundry.modules.order.application.dto.CustomerApplyVoucherRequest;
import com.laundry.modules.order.application.dto.CustomerOrderDetailResponse;
import com.laundry.modules.order.application.dto.CustomerPortalOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Portal", description = "Khách xem đơn của mình")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-portal/orders")
public class CustomerPortalOrderController {

    private final CustomerSessionService customerSessionService;
    private final OrderService orderService;

    @Operation(summary = "Đơn của khách đang đăng nhập, mới nhất trước")
    @GetMapping
    public Page<CustomerPortalOrderResponse> listMyOrders(Authentication authentication,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.listForCustomer(customerId, page, size);
    }

    @Operation(summary = "Chi tiết một đơn của khách đang đăng nhập; 404 nếu không phải đơn của khách")
    @GetMapping("/{orderCode}")
    public CustomerOrderDetailResponse getMyOrder(Authentication authentication, @PathVariable String orderCode) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.getForCustomer(customerId, orderCode);
    }

    @Operation(summary = "Khách áp một voucher (không đổi / gỡ) cho đơn giao tận nơi đã hoàn thành, chưa thanh toán")
    @PutMapping("/{orderCode}/voucher")
    public CustomerOrderDetailResponse applyVoucher(Authentication authentication, @PathVariable String orderCode,
                                                    @Valid @RequestBody CustomerApplyVoucherRequest request,
                                                    HttpServletRequest httpRequest) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.applyCustomerVoucher(customerId, CustomerAuthRateLimiter.clientKey(httpRequest),
                orderCode, request.voucherCode());
    }

    @Operation(summary = "Khách gỡ voucher khỏi đơn giao tận nơi đã hoàn thành, chưa thanh toán")
    @DeleteMapping("/{orderCode}/voucher")
    public CustomerOrderDetailResponse removeVoucher(Authentication authentication, @PathVariable String orderCode,
                                                     HttpServletRequest httpRequest) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.removeCustomerVoucher(customerId, CustomerAuthRateLimiter.clientKey(httpRequest), orderCode);
    }

    @Operation(summary = "Khách báo đã chuyển khoản (idempotent)")
    @PostMapping("/{orderCode}/payment-claim")
    public CustomerOrderDetailResponse claimPayment(Authentication authentication, @PathVariable String orderCode) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.claimCustomerPayment(customerId, orderCode);
    }

    @Operation(summary = "Khách xác nhận đã nhận được đơn (idempotent)")
    @PostMapping("/{orderCode}/confirm-received")
    public CustomerOrderDetailResponse confirmReceived(Authentication authentication, @PathVariable String orderCode) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return orderService.confirmCustomerReceived(customerId, orderCode);
    }
}
