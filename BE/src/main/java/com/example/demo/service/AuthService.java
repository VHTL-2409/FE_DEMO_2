package com.example.demo.service;

import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RefreshResponse;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.api.dto.auth.RegisterResponse;
import com.example.demo.api.dto.auth.ResendVerificationResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.EmailVerificationToken;
import com.example.demo.domain.entity.PasswordResetToken;
import com.example.demo.domain.entity.RefreshToken;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ProfileService profileService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    @Value("${app.jwt.issuer:eduexam}")
    private String jwtIssuer;

    @Value("${app.jwt.audience:eduexam-client}")
    private String jwtAudience;

    @Transactional
    public RegisterResponse registerStudent(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email '" + request.getEmail() + "' is already registered.");
        }

        String rawPassword = request.getPassword();
        String username = request.getUsername();
        if (rawPassword == null || rawPassword.trim().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải ít nhất 8 ký tự.");
        }
        if (!rawPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.");
        }
        // Check password doesn't contain username
        if (username != null && rawPassword.toLowerCase().contains(username.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu không được chứa tên đăng nhập.");
        }
        // Check for common passwords
        String lowerPassword = rawPassword.toLowerCase();
        if (lowerPassword.contains("password") || lowerPassword.contains("123456") ||
            lowerPassword.contains("admin") || lowerPassword.equals("qwerty") ||
            lowerPassword.contains("letmein") || lowerPassword.contains("welcome")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu quá đơn giản. Vui lòng chọn mật khẩu mạnh hơn.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .build();
        userRepository.save(user);
        profileService.createProfilesForUser(user);
        log.info("Successfully registered new user: {}", user.getUsername());

        verificationTokenRepository.deleteByUser(user);
        String verificationToken = generateSecureToken();
        EmailVerificationToken evToken = EmailVerificationToken.builder()
                .user(user)
                .token(verificationToken)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
        verificationTokenRepository.save(evToken);

        boolean emailSent = emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        String verificationUrl = emailSent ? null : (frontendBaseUrl + "/verify-email?token=" + verificationToken);

        return RegisterResponse.builder()
                .token(null)
                .username(user.getUsername())
                .roles(List.of())
                .verificationPending(true)
                .verificationUrl(verificationUrl)
                .emailSent(emailSent)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByUsernameWithRoles(request.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!user.getEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED: Email chưa được xác minh. Vui lòng kiểm tra hộp thư.");
        }

        // Rotate refresh tokens: delete old ones and create new one
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = createRefreshToken(user, request.getDeviceInfo(), request.getIpAddress());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, jwtIssuer, jwtAudience);

        log.info("User {} successfully logged in.", user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getExpirationMs())
                .username(user.getUsername())
                .roles(extractRoleNames(user))
                .build();
    }

    @Transactional
    public RefreshResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findValidToken(refreshTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        User user = storedToken.getUser();
        if (!user.getEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        // Rotate: delete old token and create new one
        refreshTokenRepository.delete(storedToken);
        RefreshToken newRefreshToken = createRefreshToken(user, storedToken.getDeviceInfo(), storedToken.getIpAddress());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateToken(userDetails, jwtIssuer, jwtAudience);

        log.debug("Access token refreshed for user: {}", user.getUsername());
        return RefreshResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        refreshTokenRepository.findValidToken(refreshTokenValue)
                .ifPresent(token -> {
                    log.info("Refresh token revoked for user: {}", token.getUser().getUsername());
                    refreshTokenRepository.delete(token);
                });
    }

    @Transactional
    public void revokeAllUserTokens(String username) {
        userRepository.findByUsernameWithRoles(username).ifPresent(user -> {
            int count = refreshTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .toList().size();
            refreshTokenRepository.deleteAllByUser(user);
            log.info("Revoked {} refresh tokens for user: {}", count, username);
        });
    }

    private RefreshToken createRefreshToken(User user, String deviceInfo, String ipAddress) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(Instant.now().plusSeconds(refreshExpirationDays * 86400L))
                .deviceInfo(truncate(deviceInfo, 255))
                .ipAddress(truncate(ipAddress, 45))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Transactional
    public void changePassword(User user, com.example.demo.api.dto.auth.ChangePasswordRequest request) {
        User stored = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), stored.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng.");
        }
        String newPassword = request.getNewPassword();
        String username = stored.getUsername();
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải ít nhất 8 ký tự.");
        }
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.");
        }
        // Check password doesn't contain username
        if (username != null && newPassword.toLowerCase().contains(username.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được chứa tên đăng nhập.");
        }
        // Check for common passwords
        String lowerPassword = newPassword.toLowerCase();
        if (lowerPassword.contains("password") || lowerPassword.contains("123456") ||
            lowerPassword.contains("admin") || lowerPassword.equals("qwerty") ||
            lowerPassword.contains("letmein") || lowerPassword.contains("welcome")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới quá đơn giản. Vui lòng chọn mật khẩu mạnh hơn.");
        }
        // Check if new password is same as current
        if (passwordEncoder.matches(newPassword, stored.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        stored.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(stored);
        // Invalidate all refresh tokens — force re-login with new password
        refreshTokenRepository.deleteAllByUser(stored);
        log.info("Password changed and all sessions revoked for user: {}", stored.getUsername());
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElse(null);
        if (user == null) {
            log.warn("Password reset requested for unknown email: {}", email);
            return null;
        }
        resetTokenRepository.deleteByUser(user);
        String token = generateSecureToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        resetTokenRepository.save(resetToken);
        log.info("Password reset token created for user: {}", user.getUsername());
        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), token);
        return emailSent ? "SENT" : token;
    }

    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token không hợp lệ.");
        }
        EmailVerificationToken evToken = verificationTokenRepository
                .findByTokenAndExpiresAtAfter(token.trim(), Instant.now())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn."));
        User user = evToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(evToken);
        log.info("Email verified for user: {}", user.getUsername());
    }

    @Transactional
    public ResendVerificationResponse resendVerificationEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || user.getEmailVerified()) {
            return null;
        }
        verificationTokenRepository.deleteByUser(user);
        String verificationToken = generateSecureToken();
        EmailVerificationToken evToken = EmailVerificationToken.builder()
                .user(user)
                .token(verificationToken)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
        verificationTokenRepository.save(evToken);
        boolean emailSent = emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        String verificationUrl = emailSent ? null : (frontendBaseUrl + "/verify-email?token=" + verificationToken);
        return ResendVerificationResponse.builder()
                .emailSent(emailSent)
                .verificationUrl(verificationUrl)
                .build();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token không hợp lệ.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải ít nhất 8 ký tự.");
        }
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.");
        }
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndExpiresAtAfter(token.trim(), Instant.now())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn."));
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
        log.info("Password reset completed for user: {}", user.getUsername());
    }

    private static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private List<String> extractRoleNames(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .sorted(Comparator.comparing(role -> role.getName().name()))
                .map(role -> role.getName().name())
                .toList();
    }
}
