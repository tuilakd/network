package com.example.network.exception;

/**
 * Ngoại lệ ném ra khi người dùng không có quyền truy cập hoặc thao tác tài nguyên (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
