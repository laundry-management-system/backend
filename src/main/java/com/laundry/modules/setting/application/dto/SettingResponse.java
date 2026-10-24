package com.laundry.modules.setting.application.dto;

import com.laundry.modules.setting.domain.Setting;
import com.laundry.modules.setting.domain.SettingKey;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response chưa thông tin tham số của hệ thống")
public record SettingResponse(
        @Schema(description = "Mã khóa duy nhất của tham số")
        SettingKey settingKey,

        @Schema(description = "Giá trị của tham số")
        String value,

        @Schema(description = "Mô tả của tham số")
        String description,

        @Schema(description = "Mã khóa duy nhất của người cập nhật")
        Long updatedBy,

        @Schema(description = "Thời gian cập nhật lần cuối")
        Instant updatedAt
) {
    public static SettingResponse from(Setting setting) {
        return new SettingResponse(
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getUpdatedBy(),
                setting.getUpdatedAt()
        );
    }
}
