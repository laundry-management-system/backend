package com.laundry.modules.machine.application.dto;

import com.laundry.modules.machine.domain.MachineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Chuyển máy giữa Sẵn sàng và Bảo trì")
public record ChangeMachineStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        @Schema(description = "READY hoặc MAINTENANCE")
        MachineStatus status,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        @Schema(description = "Bắt buộc khi chuyển sang MAINTENANCE", example = "Kêu to khi vắt")
        String reason
) {
}
