package com.example.network.repository;

import com.example.network.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository thao tác với bảng password_reset_otps.
 */
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmail(String email);

    void deleteByEmail(String email);
}
