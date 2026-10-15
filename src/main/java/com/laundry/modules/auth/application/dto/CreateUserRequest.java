package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Request tạo tài khoản nhân viên")
public record CreateUserRequest(
        @NotBlank(message = "Tên nhân viên không được để trống")
        @Length(max = 50, message = "Tên nhân viên không được vượt quá 50 ký tự")
        @Schema(description = "Tên nhân viên", example = "Nguyễn Văn A")
        String fullName,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Length(max = 10, message = "Số điện thoại không được vượt quá 10 ký tự")
        @Schema(description = "Số điện thoại", example = "0123456789")
        String phone,

        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Length(max = 50, message = "Tên đăng nhập không được vượt quá 50 ký tự")
        @Schema(description = "Tên đăng nhập", example = "username123")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Length(max = 255, message = "Mật khẩu không được vượt quá 255 ký tự")
        @Schema(description = "Mật khẩu", example = "password123")
        String password
) {}
