package com.example.network.service;

/**
 * Service gửi email thông báo và mã xác thực trong hệ thống.
 */
public interface EmailService {

    /**
     * Gửi mã OTP đặt lại mật khẩu tới email người nhận.
     *
     * @param toEmail địa chỉ email nhận
     * @param otpCode mã OTP 6 số
     */
    void sendPasswordResetOtp(String toEmail, String otpCode);
}
