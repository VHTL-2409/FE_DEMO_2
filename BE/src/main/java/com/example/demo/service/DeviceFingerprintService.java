package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Device fingerprint normalization and hashing.
 *
 * Canonical fingerprint uses rawFingerprint when provided by the frontend,
 * otherwise falls back to userAgent + platform + studentId.
 * The result is SHA-256 hashed so the raw device info is never stored.
 * This ensures the same device always produces the same fingerprint hash
 * regardless of which API call sends it (start, signal, heartbeat).
 */
@Service
public class DeviceFingerprintService {

    /**
     * Normalizes and hashes a device fingerprint.
     *
     * Canonical = rawFingerprint|"+studentId  (if rawFingerprint is non-blank)
     *          = userAgent|"+platform|"+studentId  (otherwise)
     *
     * The canonical string is SHA-256 hashed so the raw fingerprint
     * is never persisted in plain text.
     */
    public String normalizeFingerprint(String rawFingerprint, String userAgent, Long studentId) {
        String raw = normalizeInput(rawFingerprint);
        String agent = normalizeInput(userAgent);
        String student = studentId == null ? "" : studentId.toString();

        String canonical;
        if (!raw.isBlank()) {
            // Use the frontend-provided raw fingerprint (stable, no UA mismatch)
            canonical = raw + "|" + student;
        } else if (!agent.isBlank()) {
            // Fallback: use userAgent + platform as device identifier
            canonical = agent + "|" + student;
        } else {
            return "";
        }

        return sha256(canonical);
    }

    private String normalizeInput(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
