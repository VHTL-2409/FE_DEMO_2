package com.example.demo.service;

import com.example.demo.api.dto.auth.AuthResponse;
import com.example.demo.api.dto.auth.LoginRequest;
import com.example.demo.api.dto.auth.RegisterRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ProfileService profileService;

    @Transactional
    public AuthResponse registerStudent(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email '" + request.getEmail() + "' is already registered.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        profileService.createProfilesForUser(user);
        log.info("Successfully registered new user: {}", user.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), List.of());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("User {} successfully logged in.", user.getUsername());
        return new AuthResponse(token, user.getUsername(), List.of());
    }

    public void changePassword(User user, com.example.demo.api.dto.auth.ChangePasswordRequest request) {
        User stored = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), stored.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải ít nhất 6 ký tự");
        }

        stored.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(stored);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }
}
