package com.example.network.exception;

import lombok.NonNull;

import java.util.Arrays;

/**
 * Ngoại lệ ném ra khi xảy ra xung đột dữ liệu như trùng username, email, tên phòng chat (HTTP 409).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(@NonNull NetError... netErros) {
        super(Arrays.toString(netErros));
    }
}
