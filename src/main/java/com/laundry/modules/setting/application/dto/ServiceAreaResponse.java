package com.laundry.modules.setting.application.dto;

import com.laundry.modules.setting.domain.ServiceArea;
import java.math.BigDecimal;

public record ServiceAreaResponse(Long id, String name, BigDecimal pickupFee, BigDecimal deliveryFee, boolean active) {

    public static ServiceAreaResponse from(ServiceArea area) {
        return new ServiceAreaResponse(area.getId(), area.getName(), area.getPickupFee(), area.getDeliveryFee(), area.isActive());
    }
}
