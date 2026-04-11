package com.example.demo.repository;

import com.example.demo.domain.entity.PasswordResetToken;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndExpiresAtAfter(String token, java.time.Instant cutoff);

    void deleteByUser(User user);
}
