package com.example.network.repository;

import com.example.network.entity.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    @Modifying
    @Query("DELETE FROM RequestLog r WHERE r.createdAt < :time")
    int deleteOlderThan(@Param("time") LocalDateTime time);
}