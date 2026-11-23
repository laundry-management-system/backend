package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.incident.domain.PayoutMethod;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IncidentDetailResponse(
        Long id, String code, Long orderId, String orderCode, String customerName, String customerPhone,
        IncidentType type, IncidentResolution resolution, BigDecimal amount, IncidentStatus status,
        BigDecimal deductedAmount, BigDecimal payoutDue, Instant paidAt, PayoutMethod payoutMethod,
        String createdBy, Instant createdAt, String reviewedBy, Instant reviewedAt,
        Long orderItemId, String itemLabel, ItemUnit itemUnit, BigDecimal itemQuantity,
        BigDecimal unitPrice, BigDecimal lineTotal, int affectedCount, BigDecimal maxCompensation,
        int compensationMultiplier, BigDecimal compensationCap,
        String description, String reviewNote, String overLimitReason,
        String paidBy, BigDecimal payoutAmount, Long payoutShiftId, String payoutShiftCode,
        OrderBlock order, List<ItemOption> items,
        List<ImageDto> initialImages, String initialNote, List<ImageDto> images
) {
    /** Số liệu đơn hiện tại để FE tính trước phần trừ vào đơn / cần chi. */
    public record OrderBlock(OrderStatus status, PaymentStatus paymentStatus, BigDecimal subtotal,
                             BigDecimal discountAmount, BigDecimal incidentDiscount, BigDecimal totalAmount) {
    }

    public record ItemOption(Long id, String itemType, ItemUnit unit, BigDecimal quantity,
                             BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record ImageDto(Long id, String imageUrl, String note) {
    }
}
