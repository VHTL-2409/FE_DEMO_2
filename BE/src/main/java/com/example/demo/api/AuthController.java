package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.ChangePasswordRequest;
import com.example.demo.api.dto.auth.ForgotPasswordRequest;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RefreshResponse;
import com.example.demo.api.dto.auth.RefreshTokenRequest;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.api.dto.auth.RegisterResponse;
import com.example.demo.api.dto.auth.ResetPasswordRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @Autowired
    private Environment env;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${server.port:8082}")
    private int serverPort;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    
    
    

    

    

    @GetMapping("/oauth2/google/status")
    public Map<String, Object> googleOAuthStatus() {
        String id = clientId();
        boolean ok = id != null && !id.isBlank();
        return Map.of(
                "googleClientConfigured", ok,
                "oauthRedirectUri", redirectUri(),
                "userDir", System.getProperty("user.dir", "")
        );
    }

    @GetMapping("/oauth2/google/init")
    public void initGoogleOAuth2(HttpServletResponse response) throws IOException {
        String clientId = clientId();
        if (clientId == null || clientId.isBlank()) {
            log.warn("Google OAuth: GOOGLE_CLIENT_ID trống. user.dir={} — kiểm tra BE/.env, chạy mvn clean package, hoặc đặt biến BE_DOTENV_FILE.",
                    System.getProperty("user.dir"));
            response.sendRedirect(frontendBaseUrl + "/login?error=google_not_configured");
            return;
        }
        String redirectUri = redirectUri();
        String state = UUID.randomUUID().toString();
        String googleAuthUrl = String.format(
            "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=%s" +
            "&redirect_uri=%s" +
            "&response_type=code" +
            "&scope=openid%%20email%%20profile" +
            "&access_type=offline" +
            "&prompt=select_account" +
            "&state=%s",
            clientId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            state
        );
        response.sendRedirect(googleAuthUrl);
    }

    

    @GetMapping("/oauth2/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        
        if (error != null || code == null) {
            response.sendRedirect(frontendBaseUrl + "/login?error=google_denied");
            return;
        }

        try {
            
            String clientId = clientId();
            String clientSecret = clientSecret();
            String redirectUri = redirectUri();

            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String tokenReqBody = String.format(
                "{\"code\":\"%s\",\"client_id\":\"%s\",\"client_secret\":\"%s\",\"redirect_uri\":\"%s\",\"grant_type\":\"authorization_code\"}",
                code, clientId, clientSecret, redirectUri
            );
            HttpEntity<String> req = new HttpEntity<>(tokenReqBody, headers);
            ResponseEntity<Map> tokenResp = rt.postForEntity(
                "https://oauth2.googleapis.com/token", req, Map.class);
            String accessToken = (String) tokenResp.getBody().get("access_token");

            
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userReq = new HttpEntity<>(userHeaders);
            ResponseEntity<Map> userResp = rt.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET, userReq, Map.class);
            Map userInfo = userResp.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String googleUid = String.valueOf(userInfo.get("id"));

            
            AuthResponse authResponse = authService.handleGoogleOAuth2(email, name, googleUid);

            
            String roles = String.join(",", authResponse.getRoles());
            String successUrl = frontendBaseUrl + "/auth/google/callback" +
                "?token=" + authResponse.getToken() +
                "&refreshToken=" + authResponse.getRefreshToken() +
                "&username=" + URLEncoder.encode(authResponse.getUsername(), StandardCharsets.UTF_8) +
                "&name=" + URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8) +
                "&roles=" + URLEncoder.encode(roles, StandardCharsets.UTF_8);
            response.sendRedirect(successUrl);

        } catch (Exception e) {
            log.error("Google OAuth2 callback failed: {}", e.getMessage(), e);
            response.sendRedirect(frontendBaseUrl + "/login?error=google_oauth_failed");
        }
    }

    
    private String clientId() {
        try {
            String v = firstNonBlank(
                    env.getProperty("GOOGLE_CLIENT_ID"),
                    System.getenv("GOOGLE_CLIENT_ID"));
            if (v != null) {
                return v.trim();
            }
            v = env.getProperty("app.oauth2.google.client-id");
            return v != null && !v.isBlank() ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String clientSecret() {
        try {
            String v = firstNonBlank(
                    env.getProperty("GOOGLE_CLIENT_SECRET"),
                    System.getenv("GOOGLE_CLIENT_SECRET"));
            if (v != null) {
                return v.trim();
            }
            v = env.getProperty("app.oauth2.google.client-secret");
            return v != null && !v.isBlank() ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String redirectUri() {
        try {
            String explicit = firstNonBlank(
                    env.getProperty("GOOGLE_OAUTH_REDIRECT_BASE"),
                    System.getenv("GOOGLE_OAUTH_REDIRECT_BASE"));
            String base = explicit != null
                    ? explicit.trim()
                    : env.getProperty("APP_URL", "http://localhost:8082").trim();
            base = base.replaceAll("/$", "");
            
            if (base.contains(":5173") || base.contains(":4173")) {
                String was = base;
                base = "http://127.0.0.1:" + serverPort;
                log.warn("APP_URL trỏ tới cổng Vite ({}); dùng backend {} cho Google redirect_uri.", was, base);
            }
            return base + "/api/auth/oauth2/google/callback";
        } catch (Exception e) {
            return "http://127.0.0.1:" + serverPort + "/api/auth/oauth2/google/callback";
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.registerStudent(request), "Vui lòng xác minh email để hoàn tất đăng ký.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
            request.setIpAddress(resolveClientIp(httpRequest));
        }
        if (request.getDeviceInfo() == null || request.getDeviceInfo().isBlank()) {
            request.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        }
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshAccessToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> logout(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            authService.revokeRefreshToken(request.getRefreshToken());
        }
        return ApiResponse.success(Map.of("status", "logged_out"));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> logoutAll() {
        User user = currentUserService.requireCurrentUser();
        authService.revokeAllUserTokens(user.getUsername());
        return ApiResponse.success(Map.of("status", "all_tokens_revoked"));
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

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
