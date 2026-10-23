package com.laundry.common.exception;

/** Vi phạm quy tắc nghiệp vụ, trả 400 cho client. */
public class BusinessException extends RuntimeException {

    /** Trường (theo tên field của request) gây lỗi, để FE hiện đúng chỗ; null nếu lỗi chung. */
    private final String field;

    public BusinessException(String message) {
        this(message, null);
    }

    public BusinessException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
