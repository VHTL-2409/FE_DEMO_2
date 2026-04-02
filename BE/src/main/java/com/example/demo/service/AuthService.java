package com.example.demo.service;

import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.api.dto.auth.RegisterResponse;
import com.example.demo.api.dto.auth.ResendVerificationResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.EmailVerificationToken;
import com.example.demo.domain.entity.PasswordResetToken;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Value;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ProfileService profileService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public RegisterResponse registerStudent(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email '" + request.getEmail() + "' is already registered.");
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.trim().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải ít nhất 8 ký tự.");
        }
        if (!rawPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.");
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

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("User {} successfully logged in.", user.getUsername());
        return new AuthResponse(token, user.getUsername(), extractRoleNames(user));
    }

    public void changePassword(User user, com.example.demo.api.dto.auth.ChangePasswordRequest request) {
        User stored = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), stored.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sai MK");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải ít nhất 8 ký tự và chứa cả chữ hoa, chữ thường và số.");
        }
        if (!request.getNewPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.");
        }

        stored.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(stored);
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
