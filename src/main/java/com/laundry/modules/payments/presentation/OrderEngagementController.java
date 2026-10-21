package com.laundry.modules.payments.presentation;

import com.laundry.modules.payments.application.CustomerNotificationService;
import com.laundry.modules.payments.application.DeliveryService;
import com.laundry.modules.payments.application.OverdueOrderService;
import com.laundry.modules.payments.application.PaymentDiscrepancyService;
import com.laundry.modules.payments.application.dto.ComposeNotificationRequest;
import com.laundry.modules.payments.application.dto.DeliveryRequest;
import com.laundry.modules.payments.application.dto.DeliveryResponse;
import com.laundry.modules.payments.application.dto.NotificationResponse;
import com.laundry.modules.payments.application.dto.OverdueOrderResponse;
import com.laundry.modules.payments.application.dto.PaymentDiscrepancyRequest;
import com.laundry.modules.payments.application.dto.PaymentDiscrepancyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Order delivery and customer contact")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'EMPLOYEE')")
public class OrderEngagementController {

    private final DeliveryService deliveryService;
    private final CustomerNotificationService notificationService;
    private final OverdueOrderService overdueOrderService;
    private final PaymentDiscrepancyService paymentDiscrepancyService;

    @PostMapping("/{orderCode}/delivery")
    public DeliveryResponse deliver(@PathVariable String orderCode,
                                    @Valid @RequestBody DeliveryRequest request,
                                    Authentication authentication) {
        return deliveryService.deliver(orderCode, request, authentication.getName());
    }

    @PostMapping("/{orderCode}/notifications/compose")
    public NotificationResponse composeNotification(@PathVariable String orderCode,
                                                    @Valid @RequestBody ComposeNotificationRequest request) {
        return notificationService.compose(orderCode, request.channel());
    }

    @GetMapping("/overdue")
    public List<OverdueOrderResponse> overdueOrders() {
        return overdueOrderService.list();
    }

    @PostMapping("/{orderCode}/payment-discrepancies")
    public PaymentDiscrepancyResponse recordPaymentDiscrepancy(
            @PathVariable String orderCode,
            @Valid @RequestBody PaymentDiscrepancyRequest request) {
        return paymentDiscrepancyService.record(orderCode, request);
    }
}
