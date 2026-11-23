package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address
) {}
