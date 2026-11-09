package com.example.network.exception;

/**
 * Ngoại lệ ném ra khi xảy ra xung đột dữ liệu như trùng username, email, tên phòng chat (HTTP 409).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
