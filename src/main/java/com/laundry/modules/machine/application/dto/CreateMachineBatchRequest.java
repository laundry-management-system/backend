package com.laundry.modules.machine.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMachineBatchRequest(
        @NotNull(message = "Thiếu thông tin máy")
        @Valid
        CreateMachineRequest machine,
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 20, message = "Số lượng tối đa là 20")
        int quantity,
        /** Một ô serial cho mỗi máy (khi quantity > 1); ô trống = chưa có serial */
        @Size(max = 20, message = "Số lượng tối đa là 20")
        List<@Size(max = 50, message = "Số serial tối đa 50 ký tự") String> serialNumbers
) {}
