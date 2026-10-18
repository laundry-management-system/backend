package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request đăng nhập")
public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được bỏ trống")
        @Schema(description = "Tên đăng nhập", example = "username123")
        String username,
        @NotBlank(message = "Mật khẩu không được bỏ trống")
        @Schema(description = "Mật khẩu", example = "password123")
        String password) {}
