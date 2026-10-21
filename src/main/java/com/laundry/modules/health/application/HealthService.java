package com.laundry.modules.health.application;

import com.laundry.modules.health.application.dto.HealthResponse;
import com.laundry.modules.health.domain.HealthStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthResponse check() {
        return new HealthResponse(HealthStatus.UP, checkDatabase(), Instant.now());
    }

    private HealthStatus checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return HealthStatus.UP;
        } catch (DataAccessException e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return HealthStatus.DOWN;
        }
    }
}
