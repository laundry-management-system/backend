package com.laundry.modules.setting.application.dto;

import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.domain.ServiceUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Dịch vụ đang hoạt động trên bảng giá công khai, không có id và dữ liệu audit")
public record PublicServiceResponse(
        String name,
        ServiceUnit unit,
        BigDecimal price,
        BigDecimal minQuantity,
        Integer standardHours,
        Boolean expressSupported,
        @Schema(nullable = true) BigDecimal expressSurcharge,
        @Schema(nullable = true) Integer expressHours,
        Boolean whitesApplicable
) {
    public static PublicServiceResponse from(ServiceResponse service) {
        return new PublicServiceResponse(
                service.name(),
                service.unit(),
                service.currentPrice(),
                service.minQuantity(),
                service.standardHours(),
                service.expressSupported(),
                service.expressSurcharge(),
                service.expressHours(),
                service.whitesApplicable()
        );
    }
}
