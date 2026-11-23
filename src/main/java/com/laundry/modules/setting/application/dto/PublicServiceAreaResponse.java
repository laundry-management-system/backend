package com.laundry.modules.setting.application.dto;

import com.laundry.modules.setting.domain.ServiceArea;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Khu vực đang phục vụ trên trang công khai; id dùng làm pickupAreaId / deliveryAreaId khi đặt đơn, không có trạng thái")
public record PublicServiceAreaResponse(Long id, String name, BigDecimal pickupFee, BigDecimal deliveryFee) {

    public static PublicServiceAreaResponse from(ServiceArea area) {
        return new PublicServiceAreaResponse(area.getId(), area.getName(), area.getPickupFee(), area.getDeliveryFee());
    }
}
