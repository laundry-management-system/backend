package com.laundry.modules.service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record WashModeRequest(
        @NotBlank(message = "Vui lòng nhập tên hình thức giặt")
        @Size(max = 50, message = "Tên hình thức giặt tối đa 50 ký tự")
        String name,

        // BigDecimal để 1.5 / 0 / 721 báo lỗi đúng field thay vì bị Jackson ép về int
        @NotNull(message = "Vui lòng nhập thời gian hoàn thành")
        @Digits(integer = 3, fraction = 0, message = "Thời gian hoàn thành phải là số giờ nguyên")
        @DecimalMin(value = "1", message = "Thời gian hoàn thành phải từ 1 đến 720 giờ")
        @DecimalMax(value = "720", message = "Thời gian hoàn thành phải từ 1 đến 720 giờ")
        BigDecimal completionHours,

        Boolean active,

        Boolean isDefault,

        @Valid
        List<ServiceLine> services
) {
    public record ServiceLine(
            @NotNull(message = "Vui lòng chọn dịch vụ")
            Long serviceId,

            @NotNull(message = "Vui lòng nhập giá")
            @DecimalMin(value = "0", message = "Giá không được âm")
            @Digits(integer = 12, fraction = 0, message = "Giá phải là số đồng nguyên")
            BigDecimal price
    ) {}
}
