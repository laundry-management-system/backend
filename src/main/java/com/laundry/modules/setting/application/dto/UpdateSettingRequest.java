package com.laundry.modules.setting.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSettingRequest(
        @NotNull(message = "Vui lòng nhập phụ phí giặt riêng đồ trắng")
        @Min(value = 0, message = "Phụ phí không được âm")
        Long whitesFee
) {
}
