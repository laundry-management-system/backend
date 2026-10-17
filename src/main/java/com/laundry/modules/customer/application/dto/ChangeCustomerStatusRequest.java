package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.domain.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerStatusRequest(
        @NotNull(message = "Status must not be null")
        CustomerStatus status
) {}
