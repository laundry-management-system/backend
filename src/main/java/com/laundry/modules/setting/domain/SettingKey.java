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
    COMPENSATION_MULTIPLIER(SettingType.POSITIVE_INT),
    COMPENSATION_CAP_PER_ITEM(SettingType.POSITIVE_INT),
    STORAGE_DAYS(SettingType.POSITIVE_INT),
    STORE_NAME(SettingType.STRING),
    STORE_ADDRESS(SettingType.STRING),
    WHITES_FEE(SettingType.NON_NEGATIVE_INT),
    SHIFT_WARN_HOURS(SettingType.POSITIVE_INT),
    STORE_PHONE(SettingType.PHONE),
    BOOKING_ENABLED(SettingType.BOOLEAN),
    BOOKING_DELIVERY_ENABLED(SettingType.BOOLEAN),
    BOOKING_MIN_LEAD_HOURS(SettingType.POSITIVE_INT),
    BOOKING_MAX_OPEN_PER_CUSTOMER(SettingType.POSITIVE_INT),
    BOOKING_PICKUP_MIN_AMOUNT(SettingType.NON_NEGATIVE_INT),
    BOOKING_RETURN_GAP_HOURS_NORMAL(SettingType.POSITIVE_INT),
    BOOKING_RETURN_GAP_HOURS_EXPRESS(SettingType.POSITIVE_INT);

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
