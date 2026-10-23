package com.laundry.modules.setting.application;

import com.laundry.modules.setting.application.dto.SettingResponse;
import com.laundry.modules.setting.application.dto.UpdateSettingRequest;
import com.laundry.modules.setting.domain.Setting;
import com.laundry.modules.setting.domain.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettingService {

    static final long DEFAULT_WHITES_FEE = 80_000;

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public BigDecimal getWhitesFee() {
        return BigDecimal.valueOf(readWhitesFee());
    }

    @Transactional(readOnly = true)
    public SettingResponse getSettings() {
        return new SettingResponse(readWhitesFee());
    }

    @Transactional
    public SettingResponse update(UpdateSettingRequest request) {
        settingRepository.save(new Setting(Setting.WHITES_FEE, String.valueOf(request.whitesFee())));
        return new SettingResponse(request.whitesFee());
    }

    private long readWhitesFee() {
        return settingRepository.findById(Setting.WHITES_FEE)
                .map(s -> parseOrDefault(s.getSettingValue()))
                .orElse(DEFAULT_WHITES_FEE);
    }

    private static long parseOrDefault(String value) {
        try {
            long v = Long.parseLong(value.trim());
            return v >= 0 ? v : DEFAULT_WHITES_FEE;
        } catch (NumberFormatException | NullPointerException e) {
            return DEFAULT_WHITES_FEE;
        }
    }
}
