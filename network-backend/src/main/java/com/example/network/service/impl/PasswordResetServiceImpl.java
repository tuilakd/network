package com.example.network.service.impl;

import com.example.network.entity.PasswordResetOtp;
import com.example.network.entity.User;
import com.example.network.entity.enums.UserRole;
import com.example.network.entity.request.ForgotPasswordRequest;
import com.example.network.entity.request.ResetPasswordRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.PasswordResetOtpRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.EmailService;
import com.example.network.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cài đặt nghiệp vụ quên mật khẩu và đặt lại mật khẩu với mã OTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String DEFAULT_RESPONSE = "Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.";

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    @Override
    public ApiMessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Chống dò tài khoản: Nếu email không tồn tại hoặc tài khoản có role ADMIN thì im lặng
        if (userOpt.isEmpty() || userOpt.get().getRole() == UserRole.ADMIN) {
            log.info("Yêu cầu quên mật khẩu không đủ điều kiện xử lý (không tồn tại hoặc là ADMIN)");
            return new ApiMessageResponse(DEFAULT_RESPONSE);
        }

        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository.findByEmail(email);
        LocalDateTime now = LocalDateTime.now();

        // Chống spam: nếu OTP vừa được tạo trong 60 giây gần nhất thì bỏ qua
        if (otpOpt.isPresent()) {
            PasswordResetOtp existing = otpOpt.get();
            if (existing.getCreatedAt().isAfter(now.minusSeconds(60))) {
                log.info("Bỏ qua yêu cầu gửi lại OTP cho email {} vì trong vòng 60 giây", email);
                return new ApiMessageResponse(DEFAULT_RESPONSE);
            }
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên
        int codeNumber = secureRandom.nextInt(1000000);
        String code = String.format("%06d", codeNumber);
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = now.plusMinutes(10);

        // Cập nhật bản ghi cũ hoặc tạo mới nếu chưa có
        PasswordResetOtp otpRecord;
        if (otpOpt.isPresent()) {
            otpRecord = otpOpt.get();
        } else {
            otpRecord = new PasswordResetOtp();
            otpRecord.setEmail(email);
        }

        otpRecord.setCodeHash(codeHash);
        otpRecord.setExpiresAt(expiresAt);
        otpRecord.setCreatedAt(now);
        otpRecord.setAttempts(0);
        passwordResetOtpRepository.save(otpRecord);

        // Gửi email bất đồng bộ
        emailService.sendPasswordResetOtp(email, code);

        return new ApiMessageResponse(DEFAULT_RESPONSE);
    }

    @Transactional(noRollbackFor = AppException.class)
    @Override
    public ApiMessageResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository.findByEmail(email);

        // Không tìm thấy OTP tương ứng
        if (otpOpt.isEmpty()) {
            log.warn("Không tìm thấy OTP cho email {}", email);
            throw new AppException(DomainCode.INVALID_OTP);
        }

        PasswordResetOtp otp = otpOpt.get();

        // Kiểm tra hiệu lực (10 phút)
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Mã OTP của email {} đã hết hạn", email);
            passwordResetOtpRepository.delete(otp);
            throw new AppException(DomainCode.INVALID_OTP);
        }

        // Kiểm tra số lần nhập sai đã vượt quá ngưỡng 5 lần trước đó
        if (otp.getAttempts() >= 5) {
            log.warn("Mã OTP của email {} đã nhập sai quá 5 lần, hủy mã", email);
            passwordResetOtpRepository.delete(otp);
            throw new AppException(DomainCode.INVALID_OTP);
        }

        // Kiểm tra mã OTP người dùng nhập
        boolean isMatch = passwordEncoder.matches(request.getCode(), otp.getCodeHash());
        if (!isMatch) {
            int newAttempts = otp.getAttempts() + 1;
            otp.setAttempts(newAttempts);
            if (newAttempts >= 5) {
                log.warn("Nhập sai mã OTP lần thứ 5 cho email {}, hủy mã", email);
                passwordResetOtpRepository.delete(otp);
            } else {
                log.warn("Nhập sai mã OTP lần thứ {} cho email {}", newAttempts, email);
                passwordResetOtpRepository.save(otp);
            }
            throw new AppException(DomainCode.INVALID_OTP);
        }

        // Mã đúng: Tìm tài khoản và đổi mật khẩu
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            passwordResetOtpRepository.delete(otp);
            throw new AppException(DomainCode.INVALID_OTP);
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa mã OTP sau khi sử dụng thành công
        passwordResetOtpRepository.delete(otp);
        log.info("Đặt lại mật khẩu thành công cho tài khoản có email {}", email);

        return new ApiMessageResponse("Đặt lại mật khẩu thành công");
    }
}
