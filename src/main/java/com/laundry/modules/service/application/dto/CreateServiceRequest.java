package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @Size(max = 10, message = "Mã dịch vụ tối đa 10 ký tự")
        String serviceCode,

        @NotBlank(message = "Tên dịch vụ không được để trống")
        @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
        String name,

        @NotNull(message = "Đơn vị tính không được để trống (BR-C04)")
        ServiceUnit unit,

        @NotNull(message = "Đơn giá hiện hành không được để trống (BR-C01)")
        @DecimalMin(value = "0", message = "Đơn giá phải lớn hơn hoặc bằng 0")
        BigDecimal currentPrice,

        @NotNull(message = "Thời gian xử lý chuẩn không được để trống")
        @Min(value = 1, message = "Thời gian chuẩn phải lớn hơn 0 giờ")
        Integer standardHours,

        Boolean expressSupported,

        @DecimalMin(value = "0", message = "Phụ phí giặt nhanh phải lớn hơn hoặc bằng 0")
        BigDecimal expressSurcharge,

        @Min(value = 1, message = "Thời gian giặt nhanh phải lớn hơn 0 giờ")
        Integer expressHours
) {}
