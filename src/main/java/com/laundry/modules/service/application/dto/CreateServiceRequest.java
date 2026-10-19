package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "Service name must not be blank")
        @Size(max = 100, message = "Service name must not exceed 100 characters")
        String name,

        @NotNull(message = "Service unit must not be null")
        ServiceUnit unit,

        @NotNull(message = "Current price must not be null")
        @DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
        BigDecimal currentPrice,

        @NotNull(message = "Standard processing time must not be null")
        @Min(value = 1, message = "Standard processing time must be greater than 0 hours")
        Integer standardHours,

        Boolean expressSupported,

        @DecimalMin(value = "0", message = "Express surcharge must be greater than or equal to 0")
        BigDecimal expressSurcharge,

        @Min(value = 1, message = "Express processing time must be greater than 0 hours")
        Integer expressHours
) {}
