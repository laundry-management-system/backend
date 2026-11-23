package com.laundry.common.exception;

import java.util.Map;

/** Vi phạm quy tắc nghiệp vụ, trả 400 cho client. */
public class BusinessException extends RuntimeException {

    /** Trường (theo tên field của request) gây lỗi, để FE hiện đúng chỗ; null nếu lỗi chung. */
    private final String field;

    /** Nhiều lỗi theo field trong một lần trả về; null nếu không dùng. */
    private final Map<String, String> errors;

    public BusinessException(String message) {
        this(message, (String) null);
    }

    public BusinessException(String message, String field) {
        super(message);
        this.field = field;
        this.errors = null;
    }

    public BusinessException(String message, Map<String, String> errors) {
        super(message);
        this.field = null;
        this.errors = errors;
    }

    public String getField() {
        return field;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
