package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.ChangePasswordRequest;
import com.example.demo.api.dto.auth.ForgotPasswordRequest;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.api.dto.auth.RegisterResponse;
import com.example.demo.api.dto.auth.ResetPasswordRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.registerStudent(request), "Vui lòng xác minh email để hoàn tất đăng ký.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String result = authService.requestPasswordReset(request.getEmail());
        var body = new java.util.HashMap<String, Object>();
        body.put("message", "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.");
        if (result != null && !"SENT".equals(result)) {
            body.put("resetUrl", "/reset-password?token=" + result);
        }
        return ApiResponse.success(body);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success(Map.of("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập."));
    }

    @GetMapping("/verify-email")
    public ApiResponse<Map<String, Object>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.success(Map.of("message", "Email đã được xác minh. Bạn có thể đăng nhập."));
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Map<String, Object>> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        var result = authService.resendVerificationEmail(request.getEmail());
        if (result == null) {
            return ApiResponse.success(Map.of("message", "Nếu email chưa xác minh, bạn sẽ nhận được email hướng dẫn."));
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("message", result.isEmailSent() ? "Đã gửi lại email xác minh." : "Chế độ demo: sử dụng link bên dưới.");
        if (result.getVerificationUrl() != null) {
            body.put("verificationUrl", result.getVerificationUrl());
        }
        return ApiResponse.success(body);
    }

    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();
        authService.changePassword(user, request);
        return ApiResponse.success(Map.of("status", "ok"));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> users() {
        List<Map<String, Object>> users = authService.listUsers().stream()
                .map(this::toUserMap)
                .toList();
        return ApiResponse.success(users);
    }

    private Map<String, Object> toUserMap(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(r -> r.getName().name()).toList());
    }
}
