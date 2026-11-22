package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu mã OTP đặt lại mật khẩu cho từng tài khoản email.
 * Mỗi email chỉ lưu tối đa một bản ghi (unique).
 */
@Entity
@Table(name = "password_reset_otps")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int attempts;
}
