package com.example.network.entity.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO chuẩn hóa phản hồi lỗi cho toàn bộ API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;

    public ErrorResponse(int status, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.errorCode = String.valueOf(status);
        this.error = message;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(int status, String errorCode, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.errorCode = errorCode;
        this.error = message;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(int status, String errorCode, String message, String path, Map<String, String> details) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.errorCode = errorCode;
        this.error = message;
        this.message = message;
        this.path = path;
        this.details = details;
    }
}
