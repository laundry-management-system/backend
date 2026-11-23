package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceUnit;
import com.laundry.modules.service.domain.ServiceWashModePrice;
import com.laundry.modules.service.domain.WashMode;
import java.math.BigDecimal;
import java.util.List;

public record WashModeDetailResponse(
        Long id,
        String code,
        String name,
        Integer completionHours,
        Boolean active,
        Boolean isDefault,
        List<ModeServiceLine> services
) {
    public record ModeServiceLine(Long serviceId, String serviceCode, String serviceName, ServiceUnit unit, BigDecimal price) {}

    public static WashModeDetailResponse from(WashMode m, List<ServiceWashModePrice> links) {
        return new WashModeDetailResponse(
                m.getId(), m.getCode(), m.getName(), m.getCompletionHours(), m.getActive(), m.getIsDefault(),
                links.stream()
                        .map(l -> new ModeServiceLine(l.getService().getId(), l.getService().getServiceCode(),
                                l.getService().getName(), l.getService().getUnit(), l.getPrice()))
                        .toList());
    }
}
