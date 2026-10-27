package com.laundry.modules.setting.presentation;

import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.application.dto.SettingResponse;
import com.laundry.modules.setting.application.dto.SettingUpdateRequest;
import com.laundry.modules.setting.domain.SettingKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/settings")
@Tag(name = "Setting Management", description = "API quản lý tham số hệ thống")
@PreAuthorize("hasRole('ADMIN')")
public class SettingController {

    SettingService settingService;

    @Operation(summary = "Cập nhật giá trị tham số", description = "API cập nhật giá trị tham số bằng quyền ADMIN")
    @PatchMapping("/{settingKey}")
    @ResponseStatus(HttpStatus.OK)
    public SettingResponse updateSetting(@PathVariable SettingKey settingKey,
                                         @RequestBody SettingUpdateRequest request,
                                         Authentication authentication){
        return settingService.update(settingKey, request, authentication.getName());
    }

    @Operation(summary = "Lấy danh sách tham số hệ thống", description = "API lấy danh sách tham số hệ thống bằng quyền ADMIN")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SettingResponse> getSetting(){
        return settingService.getSettings();
    }
}
