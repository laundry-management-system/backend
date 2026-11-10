package com.laundry.modules.setting.presentation;

import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.application.dto.SettingBatchUpdateRequest;
import com.laundry.modules.setting.application.dto.SettingResponse;
import com.laundry.modules.setting.application.dto.SettingUpdateRequest;
import com.laundry.modules.setting.domain.SettingKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
public class SettingController {

    SettingService settingService;

    @Operation(summary = "Cập nhật giá trị một tham số", description = "Chỉ ADMIN")
    @PatchMapping("/{settingKey}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public SettingResponse updateSetting(@PathVariable SettingKey settingKey,
                                         @Valid @RequestBody SettingUpdateRequest request,
                                         Authentication authentication){
        return settingService.update(settingKey, request, authentication.getName());
    }

    @Operation(summary = "Cập nhật nhiều tham số cùng lúc", description = "Chỉ ADMIN. Một giá trị sai thì không lưu giá trị nào")
    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public List<SettingResponse> updateSettings(@Valid @RequestBody SettingBatchUpdateRequest request,
                                                Authentication authentication){
        return settingService.updateMany(request, authentication.getName());
    }

    @Operation(summary = "Lấy danh sách tham số hệ thống", description = "Mọi người dùng đã đăng nhập (NV cần giờ mở cửa, tên tiệm...)")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SettingResponse> getSetting(){
        return settingService.getSettings();
    }
}
