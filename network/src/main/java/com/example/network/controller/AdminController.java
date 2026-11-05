package com.example.network.controller;

import com.example.network.entity.request.RoleRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.entity.response.UserSummary;
import com.example.network.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dành riêng cho quản trị viên (ROLE_ADMIN) để quản lý tài khoản người dùng.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> users(Authentication auth) {
        log.info("Admin '{}' lấy danh sách toàn bộ người dùng", auth.getName());
        List<UserSummary> users = adminService.listUsers();
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserSummary> changeRole(@PathVariable Long id,
                                                  @Valid @RequestBody RoleRequest request,
                                                  Authentication auth) {
        log.info("Admin '{}' yêu cầu đổi quyền user id={} thành '{}'", auth.getName(), id, request.getRole());
        UserSummary updated = adminService.changeRole(id, request.getRole(), auth.getName());
        log.info("Đổi quyền thành công cho user id={}", id);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long id, Authentication auth) {
        log.info("Admin '{}' yêu cầu xóa user id={}", auth.getName(), id);
        String deleted = adminService.deleteUser(id, auth.getName());
        log.info("Admin '{}' đã xóa thành công user '{}' (id={})", auth.getName(), deleted, id);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã xóa user " + deleted));
    }
}