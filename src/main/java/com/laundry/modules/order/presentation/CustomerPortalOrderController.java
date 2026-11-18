package com.laundry.modules.order.presentation;

import com.laundry.modules.customer.application.CustomerSessionService;
import com.laundry.modules.order.application.OrderService;
import com.laundry.modules.order.application.dto.CustomerPortalOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
