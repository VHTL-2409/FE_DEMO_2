package com.example.demo.repository;

import com.example.demo.domain.entity.EmailVerificationToken;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenAndExpiresAtAfter(String token, java.time.Instant cutoff);

    void deleteByUser(User user);
}
