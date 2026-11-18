package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeMyPasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        String newPassword
) {}
