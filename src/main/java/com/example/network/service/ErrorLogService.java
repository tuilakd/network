package com.example.network.service;

import com.example.network.entity.ErrorLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface ErrorLogService {

    void logError(HttpServletRequest request, Exception ex, int status, String customMessage);

    Page<ErrorLog> getLogs(Integer status, String userId, LocalDateTime fromTime, LocalDateTime toTime, int page, int size);
}
