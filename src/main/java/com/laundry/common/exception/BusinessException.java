package com.laundry.common.exception;

/** Vi phạm quy tắc nghiệp vụ, trả 400 cho client. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
