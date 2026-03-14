package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.ChangePasswordRequest;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
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
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.registerStudent(request), "User registered successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();
        authService.changePassword(user, request);
        return ApiResponse.success(Map.of("status", "ok"));
    }

    @GetMapping("/users")
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
