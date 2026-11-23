package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.Incident;
import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.incident.domain.PayoutMethod;
import com.laundry.modules.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;

public record IncidentSummaryResponse(
        Long id, String code, Long orderId, String orderCode, String customerName, String customerPhone,
        IncidentType type, IncidentResolution resolution, BigDecimal amount, IncidentStatus status,
        BigDecimal deductedAmount, BigDecimal payoutDue, Instant paidAt, PayoutMethod payoutMethod,
        String createdBy, Instant createdAt, String reviewedBy, Instant reviewedAt
) {
    public static IncidentSummaryResponse from(Incident i) {
        Order o = i.getOrder();
        return new IncidentSummaryResponse(i.getId(), i.getCode(), o.getId(), o.getOrderCode(), o.getCustomerName(),
                o.getCustomerPhone(), i.getType(), i.getResolution(), i.getAmount(), i.getStatus(),
                i.getDeductedAmount(), i.getPayoutDue(), i.getPaidAt(), i.getPayoutMethod(), i.getCreatedBy(),
                i.getCreatedAt(), i.getReviewedBy(), i.getReviewedAt());
    }
}
