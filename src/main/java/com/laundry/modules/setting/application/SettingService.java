package com.laundry.modules.setting.application;

import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.setting.application.dto.SettingResponse;
import com.laundry.modules.setting.application.dto.SettingUpdateRequest;
import com.laundry.modules.setting.domain.Setting;
import com.laundry.modules.setting.domain.SettingKey;
import com.laundry.modules.setting.domain.SettingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingService {

    SettingRepository settingRepository;
    AuditLogService auditLogService;
    ActorIdResolver resolver;

    @Transactional
    public SettingResponse update(SettingKey settingKey, SettingUpdateRequest request, String username){

        Setting oldD = settingRepository.findById(settingKey)
                .orElseThrow(() -> NotFoundException.of("Tham số", settingKey.name()));

        oldD.getSettingKey().validate(request.value());
        Setting newD = oldD.update(request.value(), resolver.resolveId(username));
        settingRepository.save(newD);

        log(oldD, newD);

        return SettingResponse.from(newD);
    }

    private Map<String, Object> toJson(Setting data){
        Map<String, Object> result = new HashMap<>();
        result.put("settingKey", data.getSettingKey());
        result.put("settingValue", data.getSettingValue());
        result.put("description", data.getDescription());
        result.put("updatedBy", data.getUpdatedBy());
        result.put("updatedAt", data.getUpdatedAt());

        return result;
    }

    private void log(Setting oldD, Setting newD){
        auditLogService.log(new AuditEntry(
                AuditAction.SETTING_UPDATED,
                EntityType.SETTING,
                (long) oldD.getSettingKey().ordinal(), // Do Setting lưu PK là SettingKey (VARCHAR) nên k thể log entityID là Long như bình thường nên lấy thứ tự của enum thay
                toJson(oldD),
                toJson(newD),
                null
        ));
    }

    @Transactional(readOnly = true)
    public <T> T getValue(SettingKey key, Class<T> clazz) {
        Setting setting = settingRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình setting: " + key.name()));

        Object parsed;
        try {
            parsed = key.getType().parse(setting.getSettingValue());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Giá trị setting " + key.name() + " trong DB không hợp lệ, cần " + key.getType().hint(), e);
        }

        if (!clazz.isInstance(parsed)) {
            throw new IllegalStateException(
                    "Setting " + key.name() + " có kiểu " + key.getType() + ", không thể lấy dưới dạng "
                            + clazz.getSimpleName());
        }
        return clazz.cast(parsed);
    }

    @Transactional(readOnly = true)
    public BigDecimal getWhitesFee() {
        return BigDecimal.valueOf(getValue(SettingKey.WHITES_FEE, Integer.class));
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> getSettings(){
        return settingRepository.findAll()
                .stream()
                .map(SettingResponse::from)
                .toList();
    }
}
