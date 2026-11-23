package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerResetPasswordRequest(
        @NotBlank(message = "Link không hợp lệ hoặc đã hết hạn")
        String token,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        String newPassword
) {}
