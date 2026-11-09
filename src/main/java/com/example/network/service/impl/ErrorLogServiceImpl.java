package com.example.network.service.impl;

import com.example.network.entity.ErrorLog;
import com.example.network.repository.ErrorLogRepository;
import com.example.network.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogServiceImpl implements ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(HttpServletRequest request, Exception ex, int status, String customMessage) {
        try {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setTime(LocalDateTime.now());
            errorLog.setStatus(status);
            errorLog.setExceptionType(ex != null ? ex.getClass().getSimpleName() : "Unknown");
            errorLog.setPath(request != null ? request.getRequestURI() : "N/A");
            errorLog.setMethod(request != null ? request.getMethod() : "N/A");

            // Lấy username người dùng hiện tại, nếu chưa đăng nhập thì là 'anonymous'
            String username = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
            errorLog.setUserId(username);

            // Message: ưu tiên customMessage, cắt ngắn tối đa 1000 ký tự
            String msg = (customMessage != null && !customMessage.isBlank())
                    ? customMessage
                    : (ex != null ? ex.getMessage() : "No message");
            if (msg != null && msg.length() > 990) {
                msg = msg.substring(0, 990) + "...";
            }
            errorLog.setMessage(msg);

            // StackTrace: CHỈ lưu cho lỗi máy chủ (status >= 500), cắt ngắn tối đa 2000 ký tự
            // Lỗi 4xx KHÔNG lưu stack trace để tránh làm phình rác database
            if (status >= 500 && ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String fullTrace = sw.toString();
                if (fullTrace.length() > 1990) {
                    fullTrace = fullTrace.substring(0, 1990) + "...";
                }
                errorLog.setStackTrace(fullTrace);
            } else {
                errorLog.setStackTrace(null);
            }

            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            // Bọc try-catch tuyệt đối không để việc lưu log làm crash hoặc gián đoạn luồng xử lý
            log.error("Không thể lưu ErrorLog vào database: {}", e.getMessage());
        }
    }

    @Override
    public Page<ErrorLog> getLogs(Integer status, String userId, LocalDateTime fromTime, LocalDateTime toTime, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String trimmedUser = (userId != null && !userId.isBlank()) ? userId.trim() : null;
        return errorLogRepository.filterLogs(status, trimmedUser, fromTime, toTime, pageable);
    }
}
