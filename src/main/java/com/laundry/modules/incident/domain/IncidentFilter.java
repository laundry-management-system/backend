package com.laundry.modules.incident.domain;

import java.time.Instant;

/** Bộ lọc danh sách / thống kê sự cố; trường null = không lọc. {@code createdTo} là mốc loại trừ. */
public record IncidentFilter(IncidentStatus status, String q, IncidentType type,
                             Instant createdFrom, Instant createdTo, Long orderId) {
}
