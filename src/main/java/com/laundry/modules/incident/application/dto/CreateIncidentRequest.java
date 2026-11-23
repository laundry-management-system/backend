package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateIncidentRequest(
        @NotNull(message = "Chọn đơn liên quan") Long orderId,
        Long orderItemId,
        @NotNull(message = "Nhập số món bị sự cố")
        @Min(value = 1, message = "Số món bị sự cố phải từ 1 trở lên") Integer affectedCount,
        @NotNull(message = "Chọn loại sự cố") IncidentType type,
        @NotBlank(message = "Nhập mô tả sự cố") @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự") String description,
        @NotNull(message = "Chọn phương án xử lý") IncidentResolution resolution,
        @NotNull(message = "Nhập số tiền") @PositiveOrZero(message = "Số tiền không được âm") BigDecimal amount,
        @Size(max = 6, message = "Tối đa 6 ảnh hiện tại") List<Long> imageIds
) {
}
