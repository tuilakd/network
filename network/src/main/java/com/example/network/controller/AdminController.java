package com.example.network.controller;

import com.example.network.entity.response.UserSummary;
import com.example.network.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<UserSummary> users() {
        return adminService.listUsers();
    }

    @PutMapping("/users/{id}/role")
    public UserSummary changeRole(@PathVariable Long id,
                                  @RequestBody Map<String, String> body,
                                  Authentication auth) {
        return adminService.changeRole(id, body.get("role"), auth.getName());
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) {
        String deleted = adminService.deleteUser(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã xóa user " + deleted);
        return response;
    }
}