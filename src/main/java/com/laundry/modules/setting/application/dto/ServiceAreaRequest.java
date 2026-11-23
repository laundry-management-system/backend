package com.laundry.modules.setting.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ServiceAreaRequest(
        @NotBlank(message = "Vui lòng nhập tên khu vực")
        @Size(max = 100, message = "Tên khu vực tối đa 100 ký tự")
        String name,

        @NotNull(message = "Phí lượt lấy phải là số nguyên không âm")
        @PositiveOrZero(message = "Phí lượt lấy phải là số nguyên không âm")
        @Digits(integer = 12, fraction = 0, message = "Phí lượt lấy phải là số nguyên không âm")
        BigDecimal pickupFee,

        @NotNull(message = "Phí lượt giao phải là số nguyên không âm")
        @PositiveOrZero(message = "Phí lượt giao phải là số nguyên không âm")
        @Digits(integer = 12, fraction = 0, message = "Phí lượt giao phải là số nguyên không âm")
        BigDecimal deliveryFee
) {}
