package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu vết lịch sử mọi ngoại lệ phát sinh trong hệ thống.
 */
@Entity
@Table(name = "exception_logs")
@Getter
@Setter
@NoArgsConstructor
public class ExceptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private String httpMethod;

    private String path;

    private String username;

    private String errorCode;

    @Column(length = 1000)
    private String errorMessage;

    private String exceptionType;

    @PrePersist
    public void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
