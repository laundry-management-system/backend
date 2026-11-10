package com.laundry.modules.setting.application.dto;

import com.laundry.modules.setting.domain.SettingKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request cập nhật nhiều tham số cùng lúc (lưu tất cả hoặc không lưu gì)")
public record SettingBatchUpdateRequest(
        @NotEmpty(message = "Danh sách tham số không được để trống")
        List<@Valid Item> items
) {
    public record Item(
            @NotNull(message = "Thiếu khóa tham số")
            SettingKey settingKey,
            @NotBlank(message = "Giá trị không được để trống")
            String value
    ) {}
}
