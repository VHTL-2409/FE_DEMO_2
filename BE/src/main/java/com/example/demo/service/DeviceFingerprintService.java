package com.example.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
public class DeviceFingerprintService {

    private final ObjectMapper objectMapper;

    public DeviceFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    

    public String normalizeFingerprint(String rawFingerprint, String userAgent, Long studentId) {
        String canonical = buildCanonical(rawFingerprint, userAgent, studentId);
        if (canonical.isBlank()) {
            return "";
        }
        return sha256(canonical);
    }

    

    public CanonicalFingerprint parseCanonical(String rawFingerprint, String userAgent, Long studentId) {
        String canonical = buildCanonical(rawFingerprint, userAgent, studentId);
        String hash = canonical.isBlank() ? "" : sha256(canonical);
        return new CanonicalFingerprint(hash, canonical);
    }

    private String buildCanonical(String rawFingerprint, String userAgent, Long studentId) {
        String raw = normalizeInput(rawFingerprint);
        String agent = normalizeInput(userAgent);
        String student = studentId == null ? "" : studentId.toString();

        if (!raw.isBlank()) {
            Map<String, String> parsed = parseFingerprintJson(raw);
            if (parsed != null) {
                return buildCanonicalFromMap(parsed) + "|" + student;
            }
            return raw + "|" + student;
        }

        if (!agent.isBlank()) {
            return agent + "|" + student;
        }

        return "";
    }

    private Map<String, String> parseFingerprintJson(String raw) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (String key : FINGERPRINT_KEYS) {
                Object value = parsed.get(key);
                if (value != null) {
                    result.put(key, normalizeInput(String.valueOf(value)));
                }
            }
            return result.isEmpty() ? null : result;
        } catch (JsonProcessingException | ClassCastException | NullPointerException ex) {
            return null;
        }
    }

    private String buildCanonicalFromMap(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!entry.getValue().isBlank()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        }
        return sb.toString();
    }

    private static final String[] FINGERPRINT_KEYS = {
            "screen", "timezone", "language", "platform",
            "userAgent", "hardwareConcurrency", "deviceMemory",
            "colorDepth", "touchPoints"
    };

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

    public record CanonicalFingerprint(String hash, String canonical) {}
}
