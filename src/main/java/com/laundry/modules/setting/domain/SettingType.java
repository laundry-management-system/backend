package com.laundry.modules.setting.domain;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Kiểu giá trị của một khóa cấu hình. Giá trị trong DB luôn là chuỗi, enum này lo phần diễn giải. */
public enum SettingType {

    STRING("văn bản") {
        @Override
        public Object parse(String raw) {
            return raw;
        }
    },

    POSITIVE_INT("số nguyên dương, ví dụ 7") {
        @Override
        public Object parse(String raw) {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("Phải lớn hơn 0");
            }
            return value;
        }
    },

    NON_NEGATIVE_INT("số nguyên không âm, ví dụ 80000") {
        @Override
        public Object parse(String raw) {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException("Không được âm");
            }
            return value;
        }
    },

    /** Đúng dạng HH:mm hai chữ số: "08:00" hợp lệ, "8:00" không hợp lệ. */
    TIME("giờ dạng HH:mm, ví dụ 08:00") {
        @Override
        public Object parse(String raw) {
            return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        }
    };

    private final String hint;

    SettingType(String hint) {
        this.hint = hint;
    }

    /** Parse chuỗi sang giá trị đúng kiểu; chuỗi sai định dạng thì ném RuntimeException. */
    public abstract Object parse(String raw);

    /** Mô tả định dạng mong muốn, dùng trong thông báo lỗi. */
    public String hint() {
        return hint;
    }
}