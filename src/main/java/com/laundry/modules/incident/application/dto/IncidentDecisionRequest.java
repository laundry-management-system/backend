package com.laundry.modules.incident.application.dto;

import jakarta.validation.constraints.Size;

/** Duyệt: ghi chú tùy chọn. Từ chối: lý do bắt buộc. */
public record IncidentDecisionRequest(
        @Size(max = 500, message = "Tối đa 500 ký tự")
        String note
) {
}
