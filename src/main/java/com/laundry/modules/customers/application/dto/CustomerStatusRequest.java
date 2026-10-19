package com.laundry.modules.customers.application.dto;

import com.laundry.modules.customers.domain.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record CustomerStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        CustomerStatus status
) {
}
