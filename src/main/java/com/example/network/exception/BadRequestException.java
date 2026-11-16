package com.example.network.exception;

import lombok.NonNull;

import java.util.Arrays;

/**
 * Ngoại lệ ném ra khi yêu cầu chứa dữ liệu hoặc tham số không hợp lệ (HTTP 400).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(@NonNull NetError... netErros) {
        super(Arrays.toString(netErros));
    }
}
