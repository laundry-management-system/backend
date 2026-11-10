package com.laundry.modules.setting.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.setting.application.dto.SettingBatchUpdateRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingService {

    SettingRepository settingRepository;
    AuditLogService auditLogService;
    ActorIdResolver resolver;

    @Transactional
    public SettingResponse update(SettingKey settingKey, SettingUpdateRequest request, String username){
        settingKey.validate(request.value());
        return SettingResponse.from(apply(settingKey, request.value(), resolver.resolveId(username)));
    }

    /** Lưu nhiều tham số trong một transaction: một giá trị sai thì không lưu giá trị nào. */
    @Transactional
    public List<SettingResponse> updateMany(SettingBatchUpdateRequest request, String username) {
        Set<SettingKey> seen = new HashSet<>();
        for (SettingBatchUpdateRequest.Item item : request.items()) {
            if (!seen.add(item.settingKey())) {
                throw new BusinessException("Tham số " + item.settingKey() + " bị gửi lặp", "items");
            }
            item.settingKey().validate(item.value());
        }
        long actorId = resolver.resolveId(username);
        return request.items().stream()
                .map(item -> SettingResponse.from(apply(item.settingKey(), item.value(), actorId)))
                .toList();
    }

    private Setting apply(SettingKey settingKey, String value, long actorId) {
        Setting setting = settingRepository.findById(settingKey)
                .orElseThrow(() -> NotFoundException.of("Tham số", settingKey.name()));
        String normalized = value.trim();
        if (normalized.equals(setting.getSettingValue())) {
            return setting;
        }
        // update() sửa trực tiếp entity nên phải chụp dữ liệu cũ trước, nếu không log cũ = mới
        Map<String, Object> oldData = toJson(setting);
        Setting saved = settingRepository.saveAndFlush(setting.update(normalized, actorId));
        log(saved, oldData, toJson(saved));
        return saved;
    }

    private Map<String, Object> toJson(Setting data){
        Map<String, Object> result = new HashMap<>();
        result.put("settingKey", data.getSettingKey());
        result.put("settingValue", data.getSettingValue());
        result.put("description", data.getDescription());
        result.put("updatedBy", data.getUpdatedBy());
        // Ghi chuỗi ISO; để Instant thì cột json lưu thành số giây dạng float
        result.put("updatedAt", data.getUpdatedAt() == null ? null : data.getUpdatedAt().toString());

        return result;
    }

    private void log(Setting setting, Map<String, Object> oldData, Map<String, Object> newData){
        auditLogService.log(new AuditEntry(
                AuditAction.SETTING_UPDATED,
                EntityType.SETTING,
                // PK của Setting là chuỗi: entityId giữ số thứ tự enum cho tương thích log cũ, tra cứu bằng entityCode
                (long) setting.getSettingKey().ordinal(),
                oldData,
                newData,
                null,
                setting.getSettingKey().name()
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

    /** Giá trị dạng chuỗi; trả {@code fallback} khi chưa cấu hình (dùng cho thông tin hiển thị như tên tiệm). */
    @Transactional(readOnly = true)
    public String getString(SettingKey key, String fallback) {
        return settingRepository.findById(key)
                .map(Setting::getSettingValue)
                .filter(v -> !v.isBlank())
                .orElse(fallback);
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
