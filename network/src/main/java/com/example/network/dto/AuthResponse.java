package com.example.network.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthResponse {

    private final String token;
    private final String username;
    private final String role;
    private final String type = "Bearer";
}