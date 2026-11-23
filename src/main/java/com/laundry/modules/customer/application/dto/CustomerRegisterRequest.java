package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        String phone,

        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        String password
) {}
