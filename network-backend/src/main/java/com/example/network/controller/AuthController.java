package com.example.network.controller;

import com.example.network.entity.request.ForgotPasswordRequest;
import com.example.network.entity.request.LoginRequest;
import com.example.network.entity.request.RegisterRequest;
import com.example.network.entity.request.ResetPasswordRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.entity.response.AuthResponse;
import com.example.network.entity.response.UserResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.security.LoginRateLimiter;
import com.example.network.service.AuthService;
import com.example.network.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý đăng ký, đăng nhập tài khoản và đặt lại mật khẩu qua OTP.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Yêu cầu đăng ký tài khoản mới: username={}", request.getUsername());
        UserResponse response = authService.register(request);
        log.info("Đăng ký thành công tài khoản: username={}, id={}", response.getUsername(), response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http) {
        log.info("Yêu cầu đăng nhập tài khoản: username={}", request.getUsername());
        String key = http.getRemoteAddr() + "|" + request.getUsername().trim().toLowerCase();
        loginRateLimiter.checkAllowed(key);
        AuthResponse response;
        try {
            response = authService.login(request);
        } catch (AppException e) {
            if (e.getErrorCode() == DomainCode.INVALID_CREDENTIALS) {
                loginRateLimiter.recordFailure(key);
            }
            throw e;
        }
        loginRateLimiter.recordSuccess(key);
        log.info("Đăng nhập thành công: username={}, role={}", response.username(), response.role());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Yêu cầu gửi mã OTP quên mật khẩu");
        ApiMessageResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Yêu cầu đặt lại mật khẩu với mã OTP");
        ApiMessageResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}