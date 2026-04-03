package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Set<String> KNOWN_WEAK_SECRETS = Set.of(
        "ZGVtby1zZWNyZXQta2V5LWRlbW8tc2VjcmV0LWtleS1kZW1vLXNlY3JldC1rZXk=",
        "c2VjcmV0LWtleS1kZW1v",
        "my-secret-key",
        "changeme",
        "secret",
        "password"
    );

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer:eduexam}")
    private String defaultIssuer;

    @Value("${app.jwt.audience:eduexam-client}")
    private String defaultAudience;

    @PostConstruct
    private void validateSecretStrength() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be set via SECURITY_JWT_SECRET environment variable");
        }
        if (KNOWN_WEAK_SECRETS.contains(secret)) {
            throw new IllegalStateException("JWT secret is a known weak placeholder. Please set a strong secret via SECURITY_JWT_SECRET env var");
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, defaultIssuer, defaultAudience);
    }

    public String generateToken(UserDetails userDetails, String issuer, String audience) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, defaultAudience);
    }

    public boolean isTokenValid(String token, UserDetails userDetails, String expectedAudience) {
        try {
            String username = extractUsername(token);
            if (!username.equals(userDetails.getUsername())) return false;
            if (isTokenExpired(token)) return false;
            if (!validateAudience(token, expectedAudience)) return false;
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean validateAudience(String token, String expectedAudience) {
        try {
            Claims claims = parseClaims(token);
            var audiences = claims.getAudience();
            return audiences != null && audiences.contains(expectedAudience);
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .clockSkewSeconds(60)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
