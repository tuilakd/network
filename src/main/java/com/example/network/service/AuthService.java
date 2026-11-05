package com.example.network.service;

import com.example.network.entity.response.AuthResponse;
import com.example.network.entity.request.LoginRequest;
import com.example.network.entity.request.RegisterRequest;
import com.example.network.entity.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}