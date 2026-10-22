package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceUnit;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceResponse(
        Long id,
        String serviceCode,
        String name,
        ServiceUnit unit,
        String unitDisplayName,
        BigDecimal currentPrice,
        Integer standardHours,
        Boolean expressSupported,
        BigDecimal expressSurcharge,
        Integer expressHours,
        ServiceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServiceResponse from(LaundryService service) {
        return new ServiceResponse(
                service.getId(),
                service.getServiceCode(),
                service.getName(),
                service.getUnit(),
                service.getUnit() != null ? service.getUnit().getDisplayName() : null,
                service.getCurrentPrice(),
                service.getStandardHours(),
                service.getExpressSupported(),
                service.getExpressSurcharge(),
                service.getExpressHours(),
                service.getStatus(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
