package com.example.network.service;

import com.example.network.dto.AuthResponse;
import com.example.network.dto.LoginRequest;
import com.example.network.dto.RegisterRequest;
import com.example.network.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}