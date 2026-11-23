package com.laundry.modules.setting.application.dto;

import com.laundry.modules.service.domain.WashMode;

public record PublicWashModeResponse(Long id, String code, String name, Integer completionHours, Boolean isDefault) {
    public static PublicWashModeResponse from(WashMode m) {
        return new PublicWashModeResponse(m.getId(), m.getCode(), m.getName(), m.getCompletionHours(), m.getIsDefault());
    }
}
