package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeCustomerVipRequest(
        @NotNull(message = "Vui lòng chọn bật hoặc tắt VIP")
        Boolean vip
) {
}
