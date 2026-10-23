package com.laundry.modules.payments.application;

import com.laundry.modules.payments.application.dto.OverdueOrderResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OverdueOrderService {

    private final OrderIntegration orderIntegration;
    private final OverdueDaysProvider overdueDaysProvider;

    @Transactional(readOnly = true)
    public List<OverdueOrderResponse> list() {
        int overdueDays = overdueDaysProvider.getDays();
        Instant now = Instant.now();
        Instant threshold = now.minus(overdueDays, ChronoUnit.DAYS);
        return orderIntegration.findOverdue(threshold).stream()
                .map(order -> new OverdueOrderResponse(order.orderCode(), order.customerName(), order.customerPhone(),
                        order.completedAt(), Math.max(0, ChronoUnit.DAYS.between(order.completedAt(), now)),
                        order.notifyCount(), order.shelfLocation()))
                .toList();
    }
}
