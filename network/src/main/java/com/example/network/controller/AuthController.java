package com.example.network.controller;

import com.example.network.entity.request.LoginRequest;
import com.example.network.entity.request.RegisterRequest;
import com.example.network.entity.response.AuthResponse;
import com.example.network.entity.response.UserResponse;
import com.example.network.service.AuthService;
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
 * Controller xử lý đăng ký và đăng nhập tài khoản.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Yêu cầu đăng ký tài khoản mới: username={}", request.getUsername());
        UserResponse response = authService.register(request);
        log.info("Đăng ký thành công tài khoản: username={}, id={}", response.getUsername(), response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Yêu cầu đăng nhập tài khoản: username={}", request.getUsername());
        AuthResponse response = authService.login(request);
        log.info("Đăng nhập thành công: username={}, role={}", response.getUsername(), response.getRole());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}