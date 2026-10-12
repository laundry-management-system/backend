package com.laundry.modules.health.presentation;

import com.laundry.modules.health.application.HealthService;
import com.laundry.modules.health.application.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(summary = "Kiểm tra trạng thái API và database")
    @GetMapping
    public HealthResponse health() {
        return healthService.check();
    }
}
