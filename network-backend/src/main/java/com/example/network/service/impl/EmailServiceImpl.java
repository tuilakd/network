package com.example.network.service.impl;

import com.example.network.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Cài đặt service gửi email bằng JavaMailSender.
 * Hỗ trợ chế độ dev (in mã ra console khi chưa cấu hình tài khoản mail).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.mail.from-name:Mini Messenger}")
    private String mailFromName;

    @Async
    @Override
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            log.info("[DEV MODE] Chưa cấu hình spring.mail.username. Mã OTP đặt lại mật khẩu cho {}: {}", toEmail, otpCode);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            String fromAddress = (mailFrom != null && !mailFrom.trim().isEmpty()) ? mailFrom.trim() : mailUsername.trim();
            helper.setFrom(fromAddress, mailFromName);
            helper.setTo(toEmail);
            helper.setSubject("[Mini Messenger] Mã xác nhận đặt lại mật khẩu");

            String content = "Xin chào,\n\n"
                    + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Mini Messenger.\n"
                    + "Mã xác nhận (OTP) của bạn là: " + otpCode + "\n\n"
                    + "Mã này có hiệu lực trong 10 phút. Tuyệt đối không chia sẻ mã này với bất kỳ ai để đảm bảo an toàn tài khoản.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                    + "Trân trọng,\nĐội ngũ Mini Messenger";

            helper.setText(content, false);

            mailSender.send(message);
            log.info("Đã gửi email xác nhận OTP đặt lại mật khẩu tới {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận OTP tới {}: {}", toEmail, e.getMessage());
        }
    }
}
