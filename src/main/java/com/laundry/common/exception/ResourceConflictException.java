package com.laundry.common.exception;

/** A requested resource conflicts with an existing business record. */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
