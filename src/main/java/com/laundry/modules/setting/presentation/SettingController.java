package com.laundry.modules.setting.presentation;

import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.application.dto.SettingResponse;
import com.laundry.modules.setting.application.dto.UpdateSettingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shop settings", description = "Phụ phí giặt riêng đồ trắng (BR-C07)")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @Operation(summary = "Read shop settings (all signed-in roles)")
    @GetMapping
    public SettingResponse getSettings() {
        return settingService.getSettings();
    }

    @Operation(summary = "Update shop settings (Admin)")
    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SettingResponse update(@Valid @RequestBody UpdateSettingRequest request) {
        return settingService.update(request);
    }
}
