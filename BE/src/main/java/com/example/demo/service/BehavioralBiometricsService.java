package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Phan tich sinh trắc học hanh vi (behavioral biometrics).
 * Phat hien gian lan dua tren:
 * - Mau go ban phim (typing patterns)
 * - Huong dan chuot (mouse movement patterns)
 * - Toc do go (typing speed)
 * - Rhythm patterns (inter-key timing)
 *
 * Ky thuat:
 * - Keystroke Dynamics (dwell time, flight time)
 * - Mouse movement signature
 * - Profile comparison
 * - Consistency scoring
 *
 * Gian lan phat hien:
 * - TYPING_PATTERN_MISMATCH: Mau go khac voi profile
 * - MOUSE_SIGNATURE_ANOMALY: Huong dan chuot bat thuong
 * - AUTOMATED_INPUT_DETECTED: Co dau hieu input tu dong
 * - INTERRUPTION_PATTERN: Nhieu lan ngat qua ngan
 * - RHYTHM_ANOMALY: Chu ky go bat thuong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BehavioralBiometricsService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamEventRepository examEventRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;
    private final ObjectMapper objectMapper;

    @Value("${demo.biometrics.min-keystrokes:100}")
    private int minKeystrokes;

    @Value("${demo.biometrics.typing-speed-max:400}")
    private int maxTypingSpeedCpm; // characters per minute

    @Value("${demo.biometrics.typing-speed-min:20}")
    private int minTypingSpeedCpm;

    @Value("${demo.biometrics.interruption-threshold-ms:500}")
    private long interruptionThresholdMs;

    @Value("${demo.biometrics.mismatch-threshold:0.3}")
    private double mismatchThreshold;

    /**
     * Phan tich sinh truc hanh vi cho mot attempt.
     */
    @Transactional
    public BehavioralBiometricResult analyzeAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<ExamEvent> events = examEventRepository.findByAttemptIdOrderByCreatedAtAsc(attemptId);

        TypingProfile typingProfile = extractTypingProfile(events);
        MouseProfile mouseProfile = extractMouseProfile(events);
        InterruptionPattern interruptions = analyzeInterruptions(events);

        List<String> anomalies = new ArrayList<>();
        double overallRiskScore = 0.0;

        // 1. Keystroke dynamics analysis
        if (typingProfile.totalKeystrokes() >= minKeystrokes) {
            if (typingProfile.avgSpeedCpm() > maxTypingSpeedCpm) {
                anomalies.add("EXCESSIVE_TYPING_SPEED:" + typingProfile.avgSpeedCpm() + "cpm");
                overallRiskScore += 25;
            }
            if (typingProfile.avgSpeedCpm() < minTypingSpeedCpm) {
                anomalies.add("SLOW_TYPING_SPEED:" + typingProfile.avgSpeedCpm() + "cpm");
                overallRiskScore += 10;
            }
            if (typingProfile.consistencyScore() < 0.5) {
                anomalies.add("LOW_CONSISTENCY:" + typingProfile.consistencyScore());
                overallRiskScore += 15;
            }
            if (typingProfile.flightTimeVariance() > 0.8) {
                anomalies.add("HIGH_FLIGHT_VARIANCE:" + typingProfile.flightTimeVariance());
                overallRiskScore += 20;
            }
        }

        // 2. Automated input detection
        if (typingProfile.avgFlightTimeMs() > 0 && typingProfile.avgFlightTimeMs() < 30) {
            anomalies.add("AUTOMATED_FLIGHT_TIME:" + typingProfile.avgFlightTimeMs() + "ms");
            overallRiskScore += 30;
        }
        if (typingProfile.dwellTimeVariance() > 0.9) {
            anomalies.add("AUTOMATED_DWELL_TIME:" + typingProfile.dwellTimeVariance());
            overallRiskScore += 25;
        }

        // 3. Interruption pattern
        if (interruptions.totalInterruptions() > 10) {
            anomalies.add("EXCESSIVE_INTERRUPTIONS:" + interruptions.totalInterruptions());
            overallRiskScore += interruptions.totalInterruptions() * 2;
        }
        if (interruptions.shortBurstCount() > 5) {
            anomalies.add("INTERRUPTION_BURST:" + interruptions.shortBurstCount());
            overallRiskScore += 15;
        }

        // 4. Mouse movement anomaly
        if (mouseProfile.totalMovements() > 0) {
            if (mouseProfile.avgSpeedPps() > 2000) { // pixels per second
                anomalies.add("FAST_MOUSE:" + mouseProfile.avgSpeedPps() + "pps");
                overallRiskScore += 15;
            }
            if (mouseProfile.straightnessRatio() < 0.3) {
                anomalies.add("RANDOM_MOUSE_MOVEMENT:" + mouseProfile.straightnessRatio());
                overallRiskScore += 10;
            }
        }

        // 5. Record signals if anomalies found
        if (!anomalies.isEmpty()) {
            recordBiometricSignals(attempt, anomalies, typingProfile, interruptions);
        }

        return new BehavioralBiometricResult(
                attempt.getId(),
                typingProfile,
                mouseProfile,
                interruptions,
                anomalies,
                Math.min(100, overallRiskScore)
        );
    }

    /**
     * Trich xuat typing profile tu event log.
     * Dwell time: thoi gian giu phim
     * Flight time: thoi gian giua 2 phim
     */
    private TypingProfile extractTypingProfile(List<ExamEvent> events) {
        List<Long> keyDownTimes = new ArrayList<>();
        List<Long> keyUpTimes = new ArrayList<>();
        List<Long> interKeyTimes = new ArrayList<>();
        int keystrokeCount = 0;
        int errorCount = 0;

        long lastKeyUp = 0;
        long sessionStart = 0;
        long sessionEnd = 0;

        for (ExamEvent event : events) {
            if (sessionStart == 0) sessionStart = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L;
            sessionEnd = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L;

            Map<String, Object> data = parseEventData(event.getEventData());
            String eventType = event.getEventType();

            if ("KEY_DOWN".equals(eventType)) {
                keyDownTimes.add((Long) data.getOrDefault("timestamp", System.currentTimeMillis()));
                keystrokeCount++;
            } else if ("KEY_UP".equals(eventType)) {
                long time = (Long) data.getOrDefault("timestamp", System.currentTimeMillis());
                keyUpTimes.add(time);

                if (lastKeyUp > 0) {
                    interKeyTimes.add(time - lastKeyUp);
                }
                lastKeyUp = time;
            } else if ("BACKSPACE".equals(eventType) || "DELETE_KEY".equals(eventType)) {
                errorCount++;
            }
        }

        // Tinh statistics
        double avgSpeedCpm = 0;
        if (!keyDownTimes.isEmpty() && sessionStart > 0 && sessionEnd > sessionStart) {
            long durationMinutes = Math.max(1, (sessionEnd - sessionStart) / 60000);
            avgSpeedCpm = (double) keystrokeCount / durationMinutes;
        }

        double avgFlightTimeMs = interKeyTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double flightTimeVariance = calculateVariance(interKeyTimes, avgFlightTimeMs);

        double avgDwellTime = keyUpTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double dwellTimeVariance = calculateVariance(keyUpTimes, avgDwellTime);

        double consistencyScore = calculateConsistencyScore(interKeyTimes);

        return new TypingProfile(
                keystrokeCount,
                errorCount,
                avgSpeedCpm,
                avgFlightTimeMs,
                flightTimeVariance,
                avgDwellTime,
                dwellTimeVariance,
                consistencyScore,
                interKeyTimes.size()
        );
    }

    /**
     * Trich xuat mouse profile.
     */
    private MouseProfile extractMouseProfile(List<ExamEvent> events) {
        List<Double> speeds = new ArrayList<>();
        List<Double> straightness = new ArrayList<>();
        int clickCount = 0;
        int movementCount = 0;

        double lastX = 0, lastY = 0, lastTime = 0;

        for (ExamEvent event : events) {
            Map<String, Object> data = parseEventData(event.getEventData());
            String eventType = event.getEventType();

            if ("MOUSE_MOVE".equals(eventType) || "MOUSE_DRAG".equals(eventType)) {
                Double x = parseDouble(data.get("x"));
                Double y = parseDouble(data.get("y"));
                Double time = parseDouble(data.get("timestamp"));

                if (x != null && y != null && time != null && lastTime > 0) {
                    double dx = x - lastX;
                    double dy = y - lastY;
                    double dt = time - lastTime;

                    if (dt > 0) {
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        double speed = distance / (dt / 1000.0); // pixels per second
                        speeds.add(speed);
                    }

                    double directDistance = Math.sqrt(dx * dx + dy * dy);
                    double pathLength = directDistance > 0 ? distance / directDistance : 1.0;
                    straightness.add(Math.min(1.0, pathLength));
                    movementCount++;
                }

                lastX = x;
                lastY = y;
                lastTime = time;

            } else if ("MOUSE_CLICK".equals(eventType) || "RIGHT_CLICK".equals(eventType)) {
                clickCount++;
            }
        }

        double avgSpeedPps = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgStraightness = straightness.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return new MouseProfile(
                movementCount,
                clickCount,
                avgSpeedPps,
                avgStraightness
        );
    }

    /**
     * Phan tich pattern ngat.
     */
    private InterruptionPattern analyzeInterruptions(List<ExamEvent> events) {
        int totalInterruptions = 0;
        int shortBurstCount = 0;
        int blurCount = 0;
        int tabSwitchCount = 0;
        int fullscreenExitCount = 0;
        List<Long> blurDurations = new ArrayList<>();

        long lastBlurStart = 0;
        boolean inBlur = false;

        for (ExamEvent event : events) {
            String eventType = event.getEventType();

            if ("WINDOW_BLUR".equals(eventType) || "BLUR".equals(eventType)) {
                blurCount++;
                lastBlurStart = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L;
                inBlur = true;
            } else if ("WINDOW_FOCUS".equals(eventType) || "FOCUS".equals(eventType)) {
                if (inBlur && lastBlurStart > 0) {
                    long duration = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L - lastBlurStart;
                    blurDurations.add(duration);
                    if (duration < interruptionThresholdMs) {
                        shortBurstCount++;
                    }
                    totalInterruptions++;
                }
                inBlur = false;
            } else if ("TAB_SWITCH".equals(eventType)) {
                tabSwitchCount++;
                totalInterruptions++;
            } else if ("EXIT_FULLSCREEN".equals(eventType)) {
                fullscreenExitCount++;
                totalInterruptions++;
            }
        }

        long totalBlurDuration = blurDurations.stream().mapToLong(Long::longValue).sum();
        long avgBlurDuration = blurDurations.isEmpty() ? 0 : totalBlurDuration / blurDurations.size();

        return new InterruptionPattern(
                totalInterruptions,
                blurCount,
                tabSwitchCount,
                fullscreenExitCount,
                shortBurstCount,
                totalBlurDuration,
                avgBlurDuration
        );
    }

    private double calculateVariance(List<Long> values, double mean) {
        if (values.size() < 2) return 0.0;
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        return variance / (mean > 0 ? mean * mean : 1.0);
    }

    private double calculateConsistencyScore(List<Long> interKeyTimes) {
        if (interKeyTimes.size() < 10) return 1.0;

        // Giai thuat: tinh Coefficient of Variation (CV)
        double mean = interKeyTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 1.0;

        double variance = interKeyTimes.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double cv = stdDev / mean;

        // CV thap -> nhieu consistency -> score cao
        // CV = 0 -> perfect consistency -> score = 1.0
        // CV = 1 -> CV lon -> score thap
        return Math.max(0.0, 1.0 - Math.min(1.0, cv));
    }

    private Map<String, Object> parseEventData(String eventData) {
        if (eventData == null || eventData.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(eventData, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse event data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void recordBiometricSignals(ExamAttempt attempt, List<String> anomalies,
                                        TypingProfile profile, InterruptionPattern interruptions) {
        for (String anomaly : anomalies) {
            String[] parts = anomaly.split(":", 2);
            String signalType = parts[0];
            String details = parts.length > 1 ? parts[1] : "";

            SignalSeverity severity = signalType.contains("AUTOMATED")
                    ? SignalSeverity.HIGH
                    : signalType.contains("EXCESSIVE") || signalType.contains("HIGH")
                            ? SignalSeverity.MEDIUM
                            : SignalSeverity.LOW;

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "behavioral_biometrics");
            evidence.put("signalType", signalType);
            evidence.put("details", details);
            evidence.put("typingProfile", Map.of(
                    "keystrokeCount", profile.totalKeystrokes(),
                    "avgSpeedCpm", profile.avgSpeedCpm(),
                    "avgFlightTimeMs", profile.avgFlightTimeMs(),
                    "consistencyScore", profile.consistencyScore()
            ));
            evidence.put("interruptionPattern", Map.of(
                    "total", interruptions.totalInterruptions(),
                    "blurCount", interruptions.blurCount(),
                    "tabSwitchCount", interruptions.tabSwitchCount()
            ));

            fraudSignalService.recordServerSignal(
                    attempt, signalType, severity, 0.85, evidence
            );
        }
    }

    // ========== Record Classes ==========

    public record TypingProfile(
            int totalKeystrokes,
            int errorCount,
            double avgSpeedCpm,
            double avgFlightTimeMs,
            double flightTimeVariance,
            double avgDwellTime,
            double dwellTimeVariance,
            double consistencyScore,
            int interKeyCount
    ) {}

    public record MouseProfile(
            int totalMovements,
            int clickCount,
            double avgSpeedPps,
            double straightnessRatio
    ) {}

    public record InterruptionPattern(
            int totalInterruptions,
            int blurCount,
            int tabSwitchCount,
            int fullscreenExitCount,
            int shortBurstCount,
            long totalBlurDurationMs,
            long avgBlurDurationMs
    ) {}

    public record BehavioralBiometricResult(
            Long attemptId,
            TypingProfile typingProfile,
            MouseProfile mouseProfile,
            InterruptionPattern interruptionPattern,
            List<String> anomalies,
            double riskScore
    ) {}
}
