package com.example.network.controller;

import com.example.network.entity.RequestLog;
import com.example.network.repository.RequestLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final RequestLogRepository logRepository;

    public LogController(RequestLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    public List<RequestLog> latest(@RequestParam(defaultValue = "50") int size) {
        return logRepository.findAll(
                PageRequest.of(0, Math.min(size, 200), Sort.by("id").descending())
        ).getContent();
    }
}