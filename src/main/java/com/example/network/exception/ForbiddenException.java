package com.example.network.exception;

import lombok.NonNull;

import java.util.Arrays;

/**
 * Ngoại lệ ném ra khi người dùng không có quyền truy cập hoặc thao tác tài nguyên (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(@NonNull NetError... netErrors) {
        super(Arrays.toString(netErrors));
    }
}
