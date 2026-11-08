package com.example.network.exception;

import com.example.network.entity.response.ErrorResponse;
import com.example.network.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung toàn ứng dụng:
 * Chuẩn hóa ErrorResponse trả về client và tự động ghi log lỗi vào database (bảng error_logs).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogService errorLogService;

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e, HttpServletRequest req) {
        log.warn("Lỗi 400 Bad Request tại {}: {}", req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, HttpStatus.BAD_REQUEST.value(), e.getMessage());
        ErrorResponse res = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e, HttpServletRequest req) {
        log.warn("Lỗi 403 Forbidden tại {}: {}", req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, HttpStatus.FORBIDDEN.value(), e.getMessage());
        ErrorResponse res = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest req) {
        log.warn("Lỗi 404 Not Found tại {}: {}", req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, HttpStatus.NOT_FOUND.value(), e.getMessage());
        ErrorResponse res = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e, HttpServletRequest req) {
        log.warn("Lỗi 409 Conflict tại {}: {}", req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, HttpStatus.CONFLICT.value(), e.getMessage());
        ErrorResponse res = new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();
        int status = ec.getStatus().value();
        log.warn("Ngoại lệ nghiệp vụ [{}] tại {}: {}", ec.getCode(), req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, status, e.getMessage());
        ErrorResponse res = new ErrorResponse(status, ec.getCode(), e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(ec.getStatus()).body(res);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("Lỗi IllegalArgumentException tại {}: {}", req.getRequestURI(), e.getMessage());
        errorLogService.logError(req, e, HttpStatus.BAD_REQUEST.value(), e.getMessage());
        ErrorResponse res = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e, HttpServletRequest req) {
        int status = e.getStatusCode().value();
        String reason = e.getReason() != null ? e.getReason() : "Lỗi xử lý yêu cầu";
        log.warn("Lỗi ResponseStatusException ({}) tại {}: {}", status, req.getRequestURI(), reason);
        errorLogService.logError(req, e, status, reason);
        ErrorResponse res = new ErrorResponse(status, "HTTP_" + status, reason, req.getRequestURI());
        return ResponseEntity.status(e.getStatusCode()).body(res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> details = new HashMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            details.put(f.getField(), f.getDefaultMessage());
        }
        log.warn("Lỗi validation dữ liệu tại {}: {}", req.getRequestURI(), details);
        errorLogService.logError(req, e, HttpStatus.BAD_REQUEST.value(), "Lỗi validation: " + details);
        ErrorResponse res = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "INVALID_INPUT", "Dữ liệu không hợp lệ", req.getRequestURI(), details);
        return ResponseEntity.badRequest().body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e, HttpServletRequest req) {
        log.error("Lỗi máy chủ không lường trước (500) tại " + req.getRequestURI(), e);
        // Lưu vào error_logs kèm stack trace 2000 ký tự
        errorLogService.logError(req, e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        // Phản hồi JSON an toàn, không để lộ thông tin nội bộ
        ErrorResponse res = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }
}