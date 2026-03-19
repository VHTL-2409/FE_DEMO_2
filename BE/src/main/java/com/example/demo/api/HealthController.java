package com.example.demo.api;

import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final EmailService emailService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/health/email")
    public Map<String, Object> emailStatus() {
        boolean configured = emailService.isMailConfigured();
        return Map.of(
                "configured", configured,
                "status", configured ? "READY" : "NOT_CONFIGURED",
                "message", configured
                        ? "Dịch vụ email đã sẵn sàng. Có thể gửi email xác minh, quên mật khẩu."
                        : "Email chưa cấu hình. Đặt SPRING_MAIL_USERNAME và SPRING_MAIL_PASSWORD trong .env rồi chạy run-with-mail.ps1"
        );
    }
}
