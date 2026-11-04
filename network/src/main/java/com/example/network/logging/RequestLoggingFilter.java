package com.example.network.logging;

import com.example.network.entity.RequestLog;
import com.example.network.repository.RequestLogRepository;
import com.example.network.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLogRepository logRepository;
    private final JwtUtil jwtUtil;

    public RequestLoggingFilter(RequestLogRepository logRepository, JwtUtil jwtUtil) {
        this.logRepository = logRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                RequestLog log = new RequestLog();
                log.setMethod(request.getMethod());
                String url = request.getRequestURI();
                if (request.getQueryString() != null) {
                    url += "?" + request.getQueryString();
                }
                log.setUrl(url);
                log.setIp(request.getRemoteAddr());
                log.setUsername(extractUsername(request));
                log.setStatus(response.getStatus());
                log.setDurationMs(System.currentTimeMillis() - start);
                logRepository.save(log);
            } catch (Exception ignored) {
                // lỗi ghi log không được làm hỏng request
            }
        }
    }

    private String extractUsername(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                return jwtUtil.extractUsername(token);
            }
        }
        return "anonymous";
    }
}