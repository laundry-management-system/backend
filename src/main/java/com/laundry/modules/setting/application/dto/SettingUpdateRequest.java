package com.laundry.modules.setting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request cập nhật giá trị tham số của hệ thống")
public record SettingUpdateRequest(
        @Schema(description = "Giá trị mới của tham số")
        @NotBlank
        String value
) {}
