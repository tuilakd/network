package com.example.network.controller;

import com.example.network.entity.RequestLog;
import com.example.network.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller dành riêng cho quản trị viên (ROLE_ADMIN) để xem nhật ký request.
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public ResponseEntity<List<RequestLog>> latest(@RequestParam(defaultValue = "50") int size,
                                                   Authentication auth) {
        log.info("Admin '{}' truy vấn danh sách {} nhật ký request gần nhất", auth.getName(), size);
        List<RequestLog> logs = logService.latest(size);
        return ResponseEntity.status(HttpStatus.OK).body(logs);
    }
}