package com.laundry.modules.machine.application.dto;

import jakarta.validation.constraints.Size;

public record RetireMachineRequest(
        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        String reason
) {
}
