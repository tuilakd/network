package com.example.network.exception;

import lombok.Getter;

/**
 * Ngoại lệ nghiệp vụ tập trung của toàn hệ thống.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}
