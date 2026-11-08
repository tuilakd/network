package com.example.network.service.impl;

import com.example.network.entity.ExceptionLog;
import com.example.network.entity.RequestLog;
import com.example.network.repository.ExceptionLogRepository;
import com.example.network.repository.RequestLogRepository;
import com.example.network.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final int MAX_SIZE = 200;

    private final RequestLogRepository logRepository;
    private final ExceptionLogRepository exceptionLogRepository;

    @Override
    public List<RequestLog> latest(int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return logRepository
                .findAll(PageRequest.of(0, pageSize, Sort.by("id").descending()))
                .getContent();
    }

    @Override
    public List<ExceptionLog> latestExceptions(int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return exceptionLogRepository.findByOrderByTimestampDesc(PageRequest.of(0, pageSize));
    }
}