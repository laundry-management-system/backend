package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceWashModePrice;
import com.laundry.modules.service.domain.ServiceUnit;
import com.laundry.modules.service.domain.WashMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ServiceResponse(
        Long id,
        String serviceCode,
        String name,
        ServiceUnit unit,
        String unitDisplayName,
        BigDecimal minQuantity,
        Boolean whitesApplicable,
        ServiceStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<ServicePrice> prices
) {
    public record ServicePrice(Long washModeId, String washModeName, BigDecimal price) {}

    public static ServiceResponse from(LaundryService service) {
        return new ServiceResponse(
                service.getId(),
                service.getServiceCode(),
                service.getName(),
                service.getUnit(),
                service.getUnit() != null ? service.getUnit().getDisplayName() : null,
                service.getMinQuantity(),
                service.getWhitesApplicable(),
                service.getStatus(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                service.getPrices().stream()
                        .sorted(Comparator.comparing(ServiceWashModePrice::getWashMode, WashMode.DISPLAY_ORDER))
                        .map(p -> new ServicePrice(p.getWashMode().getId(), p.getWashMode().getName(), p.getPrice()))
                        .toList()
        );
    }
}
