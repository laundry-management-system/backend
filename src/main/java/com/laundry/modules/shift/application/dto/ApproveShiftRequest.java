package com.laundry.modules.shift.application.dto;

import jakarta.validation.constraints.Size;

public record ApproveShiftRequest(
        @Size(max = 500, message = "Ghi chú duyệt tối đa 500 ký tự")
        String note
) {
}
