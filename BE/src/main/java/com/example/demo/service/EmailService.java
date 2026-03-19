package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class EmailService {

    private final Optional<JavaMailSender> mailSender;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}") String mailUser) {
        this.mailSender = Optional.ofNullable(mailSender);
        boolean hasCreds = mailUser != null && !mailUser.isBlank();
        if (mailSender == null || !hasCreds) {
            log.info("Email: chưa cấu hình. Đặt SPRING_MAIL_USERNAME và SPRING_MAIL_PASSWORD trong .env (Gmail App Password) rồi chạy run-with-mail.ps1");
        } else {
            log.info("Email: đã cấu hình ({}), sẵn sàng gửi thư.", mailUser);
        }
    }

    @Value("${app.mail.from:noreply@examportal.edu}")
    private String fromEmail;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * Kiểm tra mail đã được cấu hình chưa (host không rỗng).
     */
    public boolean isMailConfigured() {
        return mailSender.isPresent();
    }

    /**
     * Gửi email đặt lại mật khẩu.
     * @return true nếu gửi thành công, false nếu không gửi được (mail chưa cấu hình hoặc lỗi).
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        if (mailSender.isEmpty()) {
            log.debug("Mail not configured, skip sending password reset email");
            return false;
        }
        try {
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("[EduPlatform] Đặt lại mật khẩu");
            msg.setText("Bạn đã yêu cầu đặt lại mật khẩu.\n\n" +
                    "Vui lòng truy cập link sau trong vòng 1 giờ:\n" + resetUrl + "\n\n" +
                    "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
            mailSender.get().send(msg);
            log.info("Password reset email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email xác minh đăng ký.
     */
    public boolean sendVerificationEmail(String toEmail, String verificationToken) {
        if (mailSender.isEmpty()) {
            log.debug("Mail not configured, skip sending verification email");
            return false;
        }
        try {
            String verifyUrl = frontendBaseUrl + "/verify-email?token=" + verificationToken;
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("[EduPlatform] Xác minh email đăng ký");
            msg.setText("Chào mừng bạn đến với EduPlatform!\n\n" +
                    "Vui lòng xác minh email bằng cách truy cập link sau trong vòng 24 giờ:\n" + verifyUrl + "\n\n" +
                    "Nếu bạn không đăng ký, hãy bỏ qua email này.");
            mailSender.get().send(msg);
            log.info("Verification email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {} - Kiểm tra SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD (Gmail App Password) trong .env", toEmail, e.getMessage());
            return false;
        }
    }
}
