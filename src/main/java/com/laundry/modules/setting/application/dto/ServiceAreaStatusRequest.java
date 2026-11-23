package com.laundry.modules.setting.application.dto;

import jakarta.validation.constraints.NotNull;

public record ServiceAreaStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái")
        Boolean active
) {}
