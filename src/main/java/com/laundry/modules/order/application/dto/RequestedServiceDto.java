package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.ItemUnit;

/** Dịch vụ chọn ở quầy (ghi chú tham khảo, không có giá). name/unit null nếu dịch vụ đã bị xóa. */
public record RequestedServiceDto(Long id, String name, ItemUnit unit) {
}
