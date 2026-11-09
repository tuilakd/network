package com.example.network.repository;

import com.example.network.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    @Query("SELECT e FROM ErrorLog e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:userId IS NULL OR e.userId = :userId) AND " +
            "(:fromTime IS NULL OR e.time >= :fromTime) AND " +
            "(:toTime IS NULL OR e.time <= :toTime) " +
            "ORDER BY e.time DESC")
    Page<ErrorLog> filterLogs(@Param("status") Integer status,
                              @Param("userId") String userId,
                              @Param("fromTime") LocalDateTime fromTime,
                              @Param("toTime") LocalDateTime toTime,
                              Pageable pageable);
}
