package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request cập nhật thông tin nhân viên")
public record UpdateUserRequest(
        @Schema(description = "Họ và tên nhân viên", example = "Nguyễn Văn A")
        @NotBlank(message = "Họ và tên không được để trống")
        String fullName,

        @Schema(description = "Số điện thoại nhân viên", example = "0123456789")
        @NotBlank(message = "Số điện thoại không được để trống")
        String phone
) {}
