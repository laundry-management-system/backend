package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.HistoryActionType;
import com.laundry.modules.order.domain.OrderStatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Lịch sử thay đổi và tiến trình của đơn hàng")
public record OrderStatusHistoryResponse(
        Long id,
        HistoryActionType actionType,
        String actionDescription,
        String oldValue,
        String newValue,
        String reason,
        String performedBy,
        Instant createdAt
) {
    public static OrderStatusHistoryResponse from(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getActionType(),
                history.getActionType().getDescription(),
                history.getOldValue(),
                history.getNewValue(),
                history.getReason(),
                history.getPerformedBy(),
                history.getCreatedAt()
        );
    }
}

