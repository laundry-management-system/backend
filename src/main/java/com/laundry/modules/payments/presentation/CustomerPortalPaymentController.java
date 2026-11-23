package com.laundry.modules.payments.presentation;

import com.laundry.modules.customer.application.CustomerSessionService;
import com.laundry.modules.payments.application.PaymentService;
import com.laundry.modules.payments.application.dto.VietQrResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Covered by the /api/customer-portal/** CUSTOMER rule in SecurityConfig. */
@Tag(name = "Customer Portal", description = "Khách xem đơn của mình")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-portal/orders/{orderCode}/payment-qr")
public class CustomerPortalPaymentController {

    private final CustomerSessionService customerSessionService;
    private final PaymentService paymentService;

    @Operation(summary = "Mã VietQR để khách tự chuyển khoản (chỉ đọc)")
    @GetMapping
    public VietQrResponse paymentQr(Authentication authentication, @PathVariable String orderCode) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        return paymentService.generateCustomerVietQr(orderCode, customerId);
    }
}
