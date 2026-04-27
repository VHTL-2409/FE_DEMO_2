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

/**
 * Chuẩn hóa và băm device fingerprint.
 *
 * Chấp nhận hai định dạng từ frontend:
 * - JSON string: {"screen":"1920x1080","timezone":"Asia/Ho_Chi_Minh","language":"vi-VN",...}
 * - Plain string: fallback legacy
 *
 * Fingerprint chuẩn hóa sử dụng rawFingerprint khi được frontend cung cấp,
 * nếu không sẽ fallback sang userAgent + platform + studentId.
 * Kết quả được SHA-256 băm nên thông tin thiết bị thô không bao giờ được lưu.
 * Điều này đảm bảo cùng một thiết bị luôn tạo ra cùng một fingerprint hash
 * bất kể API call nào gửi nó (start, signal, heartbeat).
 *
 * <p>Các trường chuẩn (ổn định, ít nhiễu): screen, timezone, language, platform,
 * userAgent, hardwareConcurrency, deviceMemory, colorDepth, touchPoints.
 * Viewport/viewportWidth/viewportHeight bị loại trừ vì chúng thay đổi
 * khi resize window và gây false positives.</p>
 */
@Service
public class DeviceFingerprintService {

    private final ObjectMapper objectMapper;

    public DeviceFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Chuẩn hóa và băm device fingerprint.
     *
     * Canonical = parsed JSON canonical fields + studentId  (nếu rawFingerprint là JSON hợp lệ)
     *          = rawFingerprint + studentId               (nếu rawFingerprint là plain string không rỗng)
     *          = userAgent + "|" + platform + "|" + studentId  (nếu không)
     *
     * Chuỗi canonical được SHA-256 băm nên fingerprint thô
     * không bao giờ được lưu dạng plain text.
     */
    public String normalizeFingerprint(String rawFingerprint, String userAgent, Long studentId) {
        String canonical = buildCanonical(rawFingerprint, userAgent, studentId);
        if (canonical.isBlank()) {
            return "";
        }
        return sha256(canonical);
    }

    /**
     * Trả về biểu diễn canonical có cấu trúc của fingerprint,
     * hoặc raw fingerprint string nếu không thể parse thành JSON.
     */
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
