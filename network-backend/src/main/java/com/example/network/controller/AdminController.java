package com.example.network.controller;

import com.example.network.entity.ErrorLog;
import com.example.network.entity.request.RoleRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.entity.response.UserSummary;
import com.example.network.service.AdminService;
import com.example.network.service.ErrorLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller dành riêng cho quản trị viên (ROLE_ADMIN) để quản lý tài khoản người dùng và xem nhật ký lỗi.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ErrorLogService errorLogService;

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

    @GetMapping("/error-logs")
    public ResponseEntity<Page<ErrorLog>> getErrorLogs(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        log.info("Admin '{}' truy vấn danh sách lỗi ngoại lệ (status={}, userId={}, page={}, size={})",
                auth.getName(), status, userId, page, size);
        Page<ErrorLog> errorLogs = errorLogService.getLogs(status, userId, from, to, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(errorLogs);
    }
}