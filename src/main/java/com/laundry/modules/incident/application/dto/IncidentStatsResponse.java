package com.laundry.modules.incident.application.dto;

public record IncidentStatsResponse(long pending, long approved, long rejected, long all) {
}
