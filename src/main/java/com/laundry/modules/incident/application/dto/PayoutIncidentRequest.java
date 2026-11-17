package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.PayoutMethod;
import jakarta.validation.constraints.NotNull;

public record PayoutIncidentRequest(@NotNull(message = "Chọn hình thức chi") PayoutMethod method) {
}
