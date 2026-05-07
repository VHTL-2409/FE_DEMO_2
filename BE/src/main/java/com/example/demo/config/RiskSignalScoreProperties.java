package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "demo.risk")
public class RiskSignalScoreProperties {

    private Map<String, Integer> signalScores = new LinkedHashMap<>();
    private Map<String, Double> weight = new LinkedHashMap<>();
    private Map<String, Integer> cap = new LinkedHashMap<>();

    public Map<String, Integer> getSignalScores() {
        return signalScores;
    }

    public void setSignalScores(Map<String, Integer> signalScores) {
        this.signalScores = signalScores != null ? new LinkedHashMap<>(signalScores) : new LinkedHashMap<>();
    }

    public Map<String, Double> getWeight() {
        return weight;
    }

    public void setWeight(Map<String, Double> weight) {
        this.weight = weight != null ? new LinkedHashMap<>(weight) : new LinkedHashMap<>();
    }

    public Map<String, Integer> getCap() {
        return cap;
    }

    public void setCap(Map<String, Integer> cap) {
        this.cap = cap != null ? new LinkedHashMap<>(cap) : new LinkedHashMap<>();
    }

    // Ưu tiên điểm cấu hình trực tiếp; nếu không có thì tính theo weight * cap.
    public int resolve(String signalType, int fallback) {
        Integer configured = lookup(signalScores, signalType);
        if (configured == null) {
            configured = scoreFromWeightAndCap(signalType, fallback);
        }
        return configured == null ? Math.max(0, fallback) : Math.max(0, configured);
    }

    private Integer scoreFromWeightAndCap(String signalType, int fallback) {
        Double configuredWeight = lookup(weight, signalType);
        Integer configuredCap = lookup(cap, signalType);
        if (configuredWeight == null && configuredCap == null) {
            return null;
        }

        double effectiveWeight = configuredWeight != null
                ? configuredWeight
                : defaultWeight();
        int effectiveCap = configuredCap != null
                ? configuredCap
                : defaultCap(fallback);

        int safeCap = Math.max(0, effectiveCap);
        double safeWeight = Math.max(0.0d, Math.min(1.0d, effectiveWeight));
        return Math.min(safeCap, (int) Math.round(safeWeight * safeCap));
    }

    private double defaultWeight() {
        Double configured = lookup(weight, "default");
        return configured != null ? configured : 1.0d;
    }

    private int defaultCap(int fallback) {
        Integer configured = lookup(cap, "default");
        return configured != null ? configured : Math.max(0, fallback);
    }

    private <T> T lookup(Map<String, T> values, String signalType) {
        String target = normalize(signalType);
        if (target.isBlank() || values == null || values.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, T> entry : values.entrySet()) {
            if (normalize(entry.getKey()).equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.toUpperCase(Locale.ROOT);
    }
}
