package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class ProductionSafetyConfig {

    private static final Set<String> WEAK_VALUES = Set.of(
            "",
            "123",
            "123456",
            "password",
            "changeme",
            "change_me",
            "CHANGE_ME_in_env",
            "CHANGE_ME_admin_password",
            "change_this_admin_password",
            "your_base64_jwt_secret"
    );

    private final Environment environment;

    public ProductionSafetyConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateProductionSettings() {
        if (!isProductionProfile()) {
            return;
        }

        requireStrong("SECURITY_JWT_SECRET", property("security.jwt.secret"), 32);
        requireStrong("DEMO_ADMIN_PASSWORD", property("demo.bootstrap.admin.password"), 12);

        boolean aiEnabled = booleanProperty("app.ai-service.enabled", true);
        if (aiEnabled) {
            requireStrong("AI_SERVICE_API_KEY", property("app.ai-service.api-key"), 24);
        }

        String appUrl = property("app.url", "APP_URL");
        String frontendUrl = property("app.frontend.base-url");
        String cors = property("app.cors.allowed-origins");
        rejectLocalhost("APP_URL", appUrl);
        rejectLocalhost("APP_FRONTEND_BASE_URL", frontendUrl);
        rejectUnsafeCors(cors);
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile == null ? "" : profile.trim().toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private String property(String key) {
        String value = environment.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private String property(String firstKey, String secondKey) {
        String value = property(firstKey);
        return value.isBlank() ? property(secondKey) : value;
    }

    private boolean booleanProperty(String key, boolean defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Set.of("1", "true", "yes", "on").contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private void requireStrong(String envName, String value, int minLength) {
        String normalized = value == null ? "" : value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (normalized.length() < minLength
                || WEAK_VALUES.contains(normalized)
                || lower.startsWith("required_")
                || lower.contains("replace_with")) {
            throw new IllegalStateException(envName + " must be set to a non-placeholder value for production");
        }
    }

    private void rejectLocalhost(String envName, String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException(envName + " must be set to the public production URL");
        }
    }

    private void rejectUnsafeCors(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.contains("*") || normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must contain production origins only");
        }
    }
}
