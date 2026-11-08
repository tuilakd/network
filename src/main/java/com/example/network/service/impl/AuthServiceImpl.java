package com.example.network.service.impl;

import com.example.network.entity.response.AuthResponse;
import com.example.network.entity.request.LoginRequest;
import com.example.network.entity.request.RegisterRequest;
import com.example.network.entity.response.UserResponse;
import com.example.network.entity.User;
import com.example.network.exception.AppException;
import com.example.network.exception.ErrorCode;
import com.example.network.repository.UserRepository;
import com.example.network.security.JwtUtil;
import com.example.network.service.AuthService;
import com.example.network.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ProfileService profileService;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);

        // Khởi tạo hồ sơ cá nhân ban đầu cho tài khoản mới
        profileService.createInitialProfile(saved.getUsername());

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByUsername(request.getUsername());
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }
}