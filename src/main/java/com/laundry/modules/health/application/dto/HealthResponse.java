package com.laundry.modules.health.application.dto;

import com.laundry.modules.health.domain.HealthStatus;
import java.time.Instant;

public record HealthResponse(HealthStatus status, HealthStatus database, Instant time) {
}
