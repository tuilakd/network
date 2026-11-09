package com.example.network.repository;

import com.example.network.entity.ExceptionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {
    List<ExceptionLog> findByOrderByTimestampDesc(Pageable pageable);
}
