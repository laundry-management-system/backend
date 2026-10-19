package com.laundry.modules.auth.application.dto;

import com.laundry.modules.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
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

        @Schema(description = "Email", example = "example@gmail.com")
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng dịnh dạng")
        String email,

        @Schema(description = "Thời gian vào làm", example = "2026-10-19")
        LocalDate hiredDate,

        @Schema(description = "Vai trò", example = "ADMIN")
        Role role
) {}
