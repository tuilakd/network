package com.example.network.controller;

import com.example.network.entity.RequestLog;
import com.example.network.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public ResponseEntity<List<RequestLog>> latest(@RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(logService.latest(size));
    }
}