package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu vết thông tin lỗi ngoại lệ phục vụ việc truy vấn, kiểm toán của quản trị viên.
 */
@Entity
@Table(name = "error_logs", indexes = {
        @Index(name = "idx_error_logs_time", columnList = "time"),
        @Index(name = "idx_error_logs_status", columnList = "status"),
        @Index(name = "idx_error_logs_user", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false)
    private int status;

    private String exceptionType;

    @Column(length = 1000)
    private String message;

    private String path;

    private String method;

    private String userId;

    @Column(length = 2000)
    private String stackTrace;

    @PrePersist
    public void onCreate() {
        if (this.time == null) {
            this.time = LocalDateTime.now();
        }
    }
}
