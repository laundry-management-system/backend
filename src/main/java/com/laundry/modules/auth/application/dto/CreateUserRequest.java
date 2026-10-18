package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Request tạo tài khoản nhân viên")
public record CreateUserRequest(
        @Schema(description = "Tên nhân viên", example = "Nguyễn Văn A")
        @NotBlank(message = "Tên nhân viên không được để trống")
        @Length(max = 50, message = "Tên nhân viên không được vượt quá 50 ký tự")
        String fullName,

        @Schema(description = "Số điện thoại", example = "0123456789")
        @NotBlank(message = "Số điện thoại không được để trống")
        @Length(max = 10, message = "Số điện thoại không được vượt quá 10 ký tự")
        @Pattern(
                regexp = "^0\\d{9}$",
                message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
        )
        String phone,

        @Schema(description = "Tên đăng nhập", example = "username123")
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Length(min = 10, max = 50, message = "Tên đăng nhập không được vượt quá 50 ký tự")
        String username,

        @Schema(description = "Mật khẩu", example = "password123")
        @NotBlank(message = "Mật khẩu không được để trống")
        @Length(min = 8, max = 255, message = "Mật khẩu không được vượt quá 255 ký tự")
        String password
) {}
