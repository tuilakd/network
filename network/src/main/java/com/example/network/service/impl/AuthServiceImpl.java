package com.example.network.service.impl;

import com.example.network.dto.AuthResponse;
import com.example.network.dto.LoginRequest;
import com.example.network.dto.RegisterRequest;
import com.example.network.dto.UserResponse;
import com.example.network.entity.User;
import com.example.network.repository.UserRepository;
import com.example.network.security.JwtUtil;
import com.example.network.service.AuthService;
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

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByUsername(request.getUsername());
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }
}