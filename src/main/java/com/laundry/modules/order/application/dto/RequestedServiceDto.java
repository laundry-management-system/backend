package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.ItemUnit;

import java.math.BigDecimal;

/**
 * Dịch vụ chọn ở quầy. name/unit null nếu dịch vụ đã bị xóa;
 * unitPrice = đơn giá đã chụp lúc tạo đơn (requested_service_prices), null nếu đơn không có ảnh chụp.
 */
public record RequestedServiceDto(Long id, String name, ItemUnit unit, BigDecimal unitPrice) {
}
