package com.laundry.modules.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record IntakeRequest(
        @NotNull(message = "Thiếu mã yêu cầu")
        UUID requestId,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0")
        String customerPhone,

        @NotBlank(message = "Vui lòng nhập họ tên khách")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String customerName,

        @NotEmpty(message = "Chọn ít nhất một dịch vụ")
        @Size(max = 2, message = "Mỗi lượt nhận tối đa 2 đơn")
        List<@Valid IntakeOrderRequest> orders
) {
}
