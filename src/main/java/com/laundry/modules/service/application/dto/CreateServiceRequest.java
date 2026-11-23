package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateServiceRequest(
        @NotBlank(message = "Vui lòng nhập tên dịch vụ")
        @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
        String name,

        @NotNull(message = "Vui lòng chọn đơn vị tính")
        ServiceUnit unit,

        @NotEmpty(message = "Vui lòng nhập giá theo hình thức giặt")
        @Valid
        List<PriceLine> prices,

        @DecimalMin(value = "0", message = "Tối thiểu không được âm")
        BigDecimal minQuantity,

        Boolean whitesApplicable
) {
    public record PriceLine(
            @NotNull(message = "Vui lòng chọn hình thức giặt")
            Long washModeId,

            @NotNull(message = "Vui lòng nhập giá")
            @DecimalMin(value = "0", message = "Giá không được âm")
            @Digits(integer = 12, fraction = 0, message = "Giá phải là số đồng nguyên")
            BigDecimal price
    ) {}
}
