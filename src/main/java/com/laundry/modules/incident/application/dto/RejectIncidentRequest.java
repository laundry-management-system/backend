package com.laundry.modules.incident.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectIncidentRequest(
        @NotBlank(message = "Nhập lý do từ chối") @Size(max = 500, message = "Lý do tối đa 500 ký tự") String note
) {
}
