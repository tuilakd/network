package com.example.network.exception;

import com.example.network.exception.code.DomainCode;
import lombok.Getter;

/**
 * Ngoại lệ nghiệp vụ tập trung của toàn hệ thống.
 */
@Getter
public class AppException extends RuntimeException {

    private final DomainCode errorCode;

    public AppException(DomainCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(DomainCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
