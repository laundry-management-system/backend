package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeServiceStatusRequest(
        @NotNull(message = "Trạng thái dịch vụ không được để trống")
        ServiceStatus status
) {}
