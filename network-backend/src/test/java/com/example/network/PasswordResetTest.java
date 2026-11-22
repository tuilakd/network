package com.example.network;

import com.example.network.entity.PasswordResetOtp;
import com.example.network.entity.User;
import com.example.network.entity.enums.UserRole;
import com.example.network.entity.request.ForgotPasswordRequest;
import com.example.network.entity.request.ResetPasswordRequest;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.PasswordResetOtpRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PasswordResetTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test_user_forgot@example.com";
    private static final String ADMIN_EMAIL = "admin_forgot@example.com";

    @BeforeEach
    void setUp() {
        passwordResetOtpRepository.deleteAll();

        // Tạo hoặc reset user test thông thường
        Optional<User> existingUser = userRepository.findByEmail(TEST_EMAIL);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setUsername("testuser_forgot");
            user.setEmail(TEST_EMAIL);
        }
        user.setRole(UserRole.USER);
        user.setPassword(passwordEncoder.encode("oldPassword123"));
        userRepository.save(user);

        // Tạo hoặc reset admin test
        Optional<User> existingAdmin = userRepository.findByEmail(ADMIN_EMAIL);
        User admin;
        if (existingAdmin.isPresent()) {
            admin = existingAdmin.get();
        } else {
            admin = new User();
            admin.setUsername("admin_forgot");
            admin.setEmail(ADMIN_EMAIL);
        }
        admin.setRole(UserRole.ADMIN);
        admin.setPassword(passwordEncoder.encode("adminPass123"));
        userRepository.save(admin);
    }

    @Test
    @DisplayName("Email không tồn tại vẫn trả thông báo thành công và không tạo OTP")
    void forgotPassword_nonExistentEmail_returnsSuccessMessageWithoutCreatingOtp() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent_email_12345@example.com");

        var response = passwordResetService.forgotPassword(request);
        assertEquals("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.", response.getMessage());

        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository.findByEmail("nonexistent_email_12345@example.com");
        assertTrue(otpOpt.isEmpty());
    }

    @Test
    @DisplayName("Email thuộc tài khoản ADMIN không được gửi OTP")
    void forgotPassword_adminRole_silentlyIgnores() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(ADMIN_EMAIL);

        var response = passwordResetService.forgotPassword(request);
        assertEquals("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.", response.getMessage());

        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository.findByEmail(ADMIN_EMAIL);
        assertTrue(otpOpt.isEmpty());
    }

    @Test
    @DisplayName("Gửi OTP thành công cho user thường và chống spam trong 60 giây")
    void forgotPassword_validUser_createsOtpAndRateLimits() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(TEST_EMAIL);

        var response = passwordResetService.forgotPassword(request);
        assertEquals("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.", response.getMessage());

        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository.findByEmail(TEST_EMAIL);
        assertTrue(otpOpt.isPresent());
        PasswordResetOtp firstOtp = otpOpt.get();
        assertEquals(0, firstOtp.getAttempts());
        assertNotNull(firstOtp.getCodeHash());

        // Gửi lại ngay lập tức (trong 60 giây) -> chống spam, OTP không thay đổi
        passwordResetService.forgotPassword(request);
        PasswordResetOtp secondOtp = passwordResetOtpRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertEquals(firstOtp.getCodeHash(), secondOtp.getCodeHash());
        assertEquals(firstOtp.getCreatedAt(), secondOtp.getCreatedAt());
    }

    @Test
    @DisplayName("Nhập sai mã OTP tăng attempts và đến lần 5 thì xóa OTP")
    void resetPassword_wrongCodeIncrementsAttemptsUntilDeletion() {
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(TEST_EMAIL);
        otp.setCodeHash(passwordEncoder.encode("123456"));
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(8));
        otp.setAttempts(0);
        passwordResetOtpRepository.save(otp);

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(TEST_EMAIL);
        req.setCode("000000"); // sai mã
        req.setNewPassword("newPassword456");

        // Nhập sai lần 1-4
        for (int i = 1; i <= 4; i++) {
            AppException ex = assertThrows(AppException.class, () -> passwordResetService.resetPassword(req));
            assertEquals(DomainCode.INVALID_OTP, ex.getErrorCode());
            PasswordResetOtp currentOtp = passwordResetOtpRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertEquals(i, currentOtp.getAttempts());
        }

        // Nhập sai lần thứ 5 -> ném lỗi và xóa OTP
        AppException ex = assertThrows(AppException.class, () -> passwordResetService.resetPassword(req));
        assertEquals(DomainCode.INVALID_OTP, ex.getErrorCode());

        Optional<PasswordResetOtp> deletedOtp = passwordResetOtpRepository.findByEmail(TEST_EMAIL);
        assertTrue(deletedOtp.isEmpty());
    }

    @Test
    @DisplayName("Mã OTP hết hạn thì ném lỗi INVALID_OTP và xóa OTP")
    void resetPassword_expiredOtp_throwsExceptionAndDeletesOtp() {
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(TEST_EMAIL);
        otp.setCodeHash(passwordEncoder.encode("123456"));
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // đã hết hạn
        otp.setAttempts(0);
        passwordResetOtpRepository.save(otp);

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(TEST_EMAIL);
        req.setCode("123456");
        req.setNewPassword("newPassword456");

        AppException ex = assertThrows(AppException.class, () -> passwordResetService.resetPassword(req));
        assertEquals(DomainCode.INVALID_OTP, ex.getErrorCode());

        Optional<PasswordResetOtp> deletedOtp = passwordResetOtpRepository.findByEmail(TEST_EMAIL);
        assertTrue(deletedOtp.isEmpty());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thành công với mã đúng, cập nhật mật khẩu và xóa OTP")
    void resetPassword_success() {
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(TEST_EMAIL);
        otp.setCodeHash(passwordEncoder.encode("654321"));
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(9));
        otp.setAttempts(0);
        passwordResetOtpRepository.save(otp);

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(TEST_EMAIL);
        req.setCode("654321");
        req.setNewPassword("brandNewPassword123");

        var response = passwordResetService.resetPassword(req);
        assertEquals("Đặt lại mật khẩu thành công", response.getMessage());

        // Kiểm tra OTP đã bị xóa
        assertTrue(passwordResetOtpRepository.findByEmail(TEST_EMAIL).isEmpty());

        // Kiểm tra mật khẩu user đã được cập nhật
        User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches("brandNewPassword123", updatedUser.getPassword()));
        assertFalse(passwordEncoder.matches("oldPassword123", updatedUser.getPassword()));
    }
}
