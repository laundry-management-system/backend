package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.WashMode;

public record WashModeResponse(
        Long id,
        String code,
        String name,
        Integer completionHours,
        Boolean active,
        Boolean isDefault
) {
    public static WashModeResponse from(WashMode m) {
        return new WashModeResponse(
                m.getId(), m.getCode(), m.getName(), m.getCompletionHours(), m.getActive(), m.getIsDefault());
    }
}
