package com.laundry.modules.payments.presentation;

import com.laundry.modules.payments.application.PaymentService;
import com.laundry.modules.payments.application.dto.ConfirmPaymentRequest;
import com.laundry.modules.payments.application.dto.PaymentResponse;
import com.laundry.modules.payments.application.dto.ResolvePaymentRequest;
import com.laundry.modules.payments.application.dto.VietQrResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/orders/{orderCode}/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'EMPLOYEE')")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/vietqr")
    public VietQrResponse createVietQr(@PathVariable String orderCode) {
        return paymentService.generateVietQr(orderCode);
    }

    @GetMapping
    public List<PaymentResponse> list(@PathVariable String orderCode) {
        return paymentService.list(orderCode);
    }

    @PostMapping
    public PaymentResponse confirm(@PathVariable String orderCode,
                                   @Valid @RequestBody ConfirmPaymentRequest request,
                                   Authentication authentication) {
        return paymentService.confirm(orderCode, request, authentication.getName());
    }

    @PostMapping("/{paymentId}/resolution")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse resolve(@PathVariable String orderCode,
                                   @PathVariable long paymentId,
                                   @Valid @RequestBody ResolvePaymentRequest request,
                                   Authentication authentication,
                                   HttpServletRequest httpRequest) {
        return paymentService.resolve(orderCode, paymentId, request, authentication.getName(),
                httpRequest.getRemoteAddr());
    }
}
