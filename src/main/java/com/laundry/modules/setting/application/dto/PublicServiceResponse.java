package com.laundry.modules.setting.application.dto;

import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.domain.ServiceUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Dịch vụ đang hoạt động trên bảng giá công khai, không có dữ liệu audit")
public record PublicServiceResponse(
        Long id,
        String name,
        ServiceUnit unit,
        BigDecimal minQuantity,
        Boolean whitesApplicable,
        List<ModePrice> prices
) {
    public record ModePrice(Long washModeId, BigDecimal price) {}

    public static PublicServiceResponse from(ServiceResponse service, java.util.Set<Long> activeModeIds) {
        return new PublicServiceResponse(
                service.id(),
                service.name(),
                service.unit(),
                service.minQuantity(),
                service.whitesApplicable(),
                service.prices().stream().filter(p -> activeModeIds.contains(p.washModeId()))
                        .map(p -> new ModePrice(p.washModeId(), p.price())).toList()
        );
    }
}
