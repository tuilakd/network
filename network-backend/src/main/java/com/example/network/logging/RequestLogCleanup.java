package com.example.network.logging;

import com.example.network.repository.RequestLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RequestLogCleanup {

    private final RequestLogRepository logRepository;
    private final int retentionDays;

    public RequestLogCleanup(RequestLogRepository logRepository,
                             @Value("${app.log.retention-days:30}") int retentionDays) {
        this.logRepository = logRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldLogs() {
        int deleted = logRepository.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
        log.info("Đã xóa {} bản ghi request_logs cũ hơn {} ngày", deleted, retentionDays);
    }
}