package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.IncidentResolution;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ApproveIncidentRequest(
        @NotNull(message = "Chọn phương án xử lý") IncidentResolution resolution,
        @NotNull(message = "Nhập số tiền") @PositiveOrZero(message = "Số tiền không được âm") BigDecimal amount,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note,
        @Size(max = 500, message = "Lý do tối đa 500 ký tự") String overLimitReason
) {
}
