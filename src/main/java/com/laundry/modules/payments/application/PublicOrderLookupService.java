package com.laundry.modules.payments.application;

import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.payments.application.dto.PublicOrderLookupRequest;
import com.laundry.modules.payments.application.dto.PublicOrderLookupResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import com.laundry.modules.payments.infrastructure.PublicLookupRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicOrderLookupService {

    private final OrderIntegration orderIntegration;
    private final PublicLookupRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public PublicOrderLookupResponse lookup(PublicOrderLookupRequest request, String clientAddress) {
        rateLimiter.check(clientAddress);
        OrderSnapshot order = orderIntegration.findByCode(request.orderCode())
                .filter(found -> found.customerPhone().equals(request.phone()))
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", request.orderCode()));
        return new PublicOrderLookupResponse(order.status(), order.dueAt(), order.totalAmount(), order.paymentStatus());
    }
}
