package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Schema(description = "Request cập nhật thông tin nhân viên")
public record UpdateUserRequest(
        @Schema(description = "Họ và tên nhân viên", example = "Nguyễn Văn A")
        @NotBlank(message = "Họ và tên không được để trống")
        @Length(max = 50, message = "Tên nhân viên không được vượt quá 50 ký tự")
        String fullName,

        @Schema(description = "Số điện thoại nhân viên", example = "0123456789")
        @NotBlank(message = "Số điện thoại không được để trống")
        @Length(max = 10, message = "Số điện thoại không được vượt quá 10 ký tự")
        @Pattern(
                regexp = "^0\\d{9}$",
                message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
        )
        String phone,

        @Schema(description = "Ngày vào làm của nhân viên", example = "2026-10-20")
        LocalDate hiredDate
) {}
