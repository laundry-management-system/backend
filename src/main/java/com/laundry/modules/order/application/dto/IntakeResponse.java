package com.laundry.modules.order.application.dto;

import java.util.List;

/** created=false khi requestId đã được dùng: trả lại lượt cũ thay vì tạo mới. */
public record IntakeResponse(String intakeCode, Long customerId, List<OrderDetailResponse> orders, boolean created) {
}
