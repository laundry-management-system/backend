package com.laundry.modules.setting.domain;

import com.laundry.common.exception.BusinessException;
import lombok.Getter;

@Getter
public enum SettingKey {
    OPENING_TIME(SettingType.TIME),
    CLOSING_TIME(SettingType.TIME),
    OVERDUE_DAYS(SettingType.POSITIVE_INT),
    MAX_LOGIN_FAIL(SettingType.POSITIVE_INT),
    LOCK_MINUTES(SettingType.POSITIVE_INT),
    BANK_BIN(SettingType.POSITIVE_INT),
    BANK_ACCOUNT_NO(SettingType.STRING),
    BANK_ACCOUNT_NAME(SettingType.STRING),
    COMPENSATION_TERMS(SettingType.STRING),
    STORAGE_DAYS(SettingType.POSITIVE_INT),
    STORE_NAME(SettingType.STRING),
    STORE_ADDRESS(SettingType.STRING),;

    private final SettingType type;

    SettingKey(SettingType type) {
        this.type = type;
    }

    public void validate(String raw){
        if(raw == null){
            throw new BusinessException("Giá trị của " + name() + " không được để trống");
        }

        try{
            type.parse(raw);
        }catch (Exception e){
            throw new BusinessException("Giá trị của " + name() + " không hợp lệ, cần " + type.hint());
        }
    }
}
