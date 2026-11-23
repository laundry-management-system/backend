package com.laundry.modules.machine.application.dto;

import com.laundry.modules.machine.domain.MachineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeMachineStatusRequest(
        @NotNull(message = "Machine status must not be null")
        MachineStatus status,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {}
