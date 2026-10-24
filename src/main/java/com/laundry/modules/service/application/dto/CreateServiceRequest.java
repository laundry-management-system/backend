package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "Vui lòng nhập tên dịch vụ")
        @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
        String name,

        @NotNull(message = "Vui lòng chọn đơn vị tính")
        ServiceUnit unit,

        @NotNull(message = "Vui lòng nhập đơn giá")
        @DecimalMin(value = "0", message = "Đơn giá không được âm")
        BigDecimal currentPrice,

        @NotNull(message = "Vui lòng nhập thời gian xử lý chuẩn")
        @Min(value = 1, message = "Thời gian xử lý chuẩn phải lớn hơn 0 giờ")
        Integer standardHours,

        Boolean expressSupported,

        @DecimalMin(value = "0", message = "Phụ phí giặt nhanh không được âm")
        BigDecimal expressSurcharge,

        @Min(value = 1, message = "Thời gian giặt nhanh phải lớn hơn 0 giờ")
        Integer expressHours,


        @DecimalMin(value = "0", message = "Tối thiểu không được âm")
        BigDecimal minQuantity,

        Boolean whitesApplicable
) {}
