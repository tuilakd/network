package com.example.network.entity.response;

import com.example.network.entity.enums.UserRole;

public record AuthResponse(String token, String username, UserRole role) {

}