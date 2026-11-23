package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
        String identifier,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        String password
) {}
