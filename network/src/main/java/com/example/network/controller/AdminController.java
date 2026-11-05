package com.example.network.controller;

import com.example.network.entity.request.RoleRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.entity.response.UserSummary;
import com.example.network.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> users() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserSummary> changeRole(@PathVariable Long id,
                                                  @Valid @RequestBody RoleRequest request,
                                                  Authentication auth) {
        return ResponseEntity.ok(adminService.changeRole(id, request.getRole(), auth.getName()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(adminService.deleteUser(id, auth.getName()));
    }
}