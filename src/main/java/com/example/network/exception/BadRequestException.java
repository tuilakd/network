package com.example.network.exception;

/**
 * Ngoại lệ ném ra khi yêu cầu chứa dữ liệu hoặc tham số không hợp lệ (HTTP 400).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
