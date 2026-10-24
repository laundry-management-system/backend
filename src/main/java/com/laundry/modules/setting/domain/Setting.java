package com.laundry.modules.setting.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "settings")
public class Setting {

    @Enumerated(value = EnumType.STRING)
    @Id
    @Column(name = "setting_key", length = 50)
    SettingKey settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    String settingValue;

    @Column(name = "description")
    String description;

    @Column(name = "updated_by")
    Long updatedBy;

    @UpdateTimestamp
    // DEFAULT now(): ddl-auto mới thêm được cột NOT NULL vào bảng settings cũ đã có dữ liệu
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp(6) with time zone default now()")
    Instant updatedAt;

    public Setting update(String value, Long updatedBy) {
        this.settingValue = value;
        this.updatedBy = updatedBy;

        return this;
    }
}
