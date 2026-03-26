package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class DeviceFingerprintService {

    public String normalizeFingerprint(String rawFingerprint, String userAgent, Long studentId) {
        String raw = normalizeInput(rawFingerprint);
        String agent = normalizeInput(userAgent);
        String student = studentId == null ? "" : studentId.toString();
        if (raw.isBlank() && agent.isBlank()) {
            return "";
        }

        String canonical = raw + "|" + agent + "|" + student;
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
