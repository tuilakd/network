package com.example.network.service;

import com.example.network.dto.UserSummary;

import java.util.List;

public interface AdminService {

    List<UserSummary> listUsers();

    UserSummary changeRole(Long userId, String role, String currentUsername);

    /** Trả về username của user vừa bị xóa. */
    String deleteUser(Long userId, String currentUsername);
}