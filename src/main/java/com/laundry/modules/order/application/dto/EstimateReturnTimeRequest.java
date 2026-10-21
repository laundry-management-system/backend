package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request gợi ý ngày giờ hẹn trả đồ")
public record EstimateReturnTimeRequest(
        @NotNull(message = "Loại hình giặt không được để trống")
        @Schema(description = "Loại hình giặt sấy", example = "WASH_DRY")
        ServiceType serviceType,

        @Schema(description = "Độ ưu tiên xử lý", example = "NORMAL")
        OrderPriority priority,

        @Schema(description = "Số kg đồ", example = "5.5")
        BigDecimal totalWeight
) {
}

