package com.example.network.service;

import com.example.network.entity.enums.UserRole;
import com.example.network.entity.response.UserSummary;

import java.util.List;

public interface AdminService {

    List<UserSummary> listUsers();

    UserSummary changeRole(Long userId, UserRole role, String currentUsername);

    /**
     * Trả về username của user vừa bị xóa.
     *
     * @return
     */
    String deleteUser(Long userId, String currentUsername);
}