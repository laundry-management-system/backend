package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request chứa mật khẩu mới khi kích hoạt tài khoản lần đầu tiên")
public record SetupPasswordRequest(
        @NotBlank(message = "Mật khẩu không được bỏ trống")
        @Size(min = 8, max = 255, message = "Mật khẩu từ 8 đến 255 ký tự")
        String password
) {}
