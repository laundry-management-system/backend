package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeServiceStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái")
        ServiceStatus status,

        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
        String reason
) {}
