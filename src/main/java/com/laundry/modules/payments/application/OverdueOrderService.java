package com.laundry.modules.payments.application;

import com.laundry.modules.payments.application.dto.OverdueOrderListResponse;
import com.laundry.modules.payments.application.dto.OverdueOrderResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OverdueOrderService {

    private final OrderIntegration orderIntegration;
    private final OverdueDaysProvider overdueDaysProvider;

    @Transactional(readOnly = true)
    public OverdueOrderListResponse list() {
        int overdueDays = overdueDaysProvider.getDays();
        Instant now = Instant.now();
        Instant threshold = now.minus(overdueDays, ChronoUnit.DAYS);
        var orders = orderIntegration.findOverdue(threshold).stream()
                .map(overdue -> {
                    var order = overdue.order();
                    return new OverdueOrderResponse(order.id(), order.orderCode(), order.customerName(),
                            order.customerPhone(), overdue.customerVip(), order.completedAt(),
                            Math.max(0, ChronoUnit.DAYS.between(order.completedAt(), now)),
                            order.notifyCount(), order.lastNotifiedAt(), order.shelfLocation(), order.paymentStatus());
                })
                .toList();
        return new OverdueOrderListResponse(overdueDays, orders);
    }
}
