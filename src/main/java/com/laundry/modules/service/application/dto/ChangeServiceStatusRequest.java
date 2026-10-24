package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeServiceStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái")
        ServiceStatus status
) {}
