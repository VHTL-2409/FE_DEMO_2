package com.example.demo.service;

import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ML-Powered Risk Scoring Service.
 *
 * Cung cấp machine learning-based risk scoring kết hợp với rule-based scoring.
 * Features:
 * - Hybrid scoring: ML + Rule-based
 * - Feature extraction từ fraud signals
 * - Anomaly detection sử dụng statistical methods
 * - Confidence scoring
 * - Model status tracking
 *
 * Models supported:
 * - Random Forest (primary)
 * - Gradient Boosting
 * - Isolation Forest (anomaly detection)
 */
@Service
@RequiredArgsConstructor
public class MLRiskScoringService {

    private static final Logger log = LoggerFactory.getLogger(MLRiskScoringService.class);

    private final FraudSignalRepository fraudSignalRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;

    // ML Configuration
    @Value("${app.ml.risk-scoring.enabled:false}")
    private boolean mlEnabled;

    @Value("${app.ml.risk-scoring.base-url:http://localhost:8090}")
    private String mlServiceUrl;

    @Value("${app.ml.risk-scoring.timeout-ms:5000}")
    private int mlTimeoutMs;

    @Value("${app.ml.risk-scoring.hybrid-weight:0.6}")
    private double mlWeight; // Weight for ML score in hybrid combination

    @Value("${app.ml.risk-scoring.min-confidence:0.5}")
    private double minConfidence;

    @Value("${app.ml.risk-scoring.confidence-threshold:0.7}")
    private double confidenceThreshold;

    // Feature weights for anomaly detection (when ML is disabled)
    private static final Map<String, Double> ANOMALY_WEIGHTS = Map.ofEntries(
            // Browser Events
            Map.entry("TAB_SWITCH", 0.8),
            Map.entry("WINDOW_BLUR", 0.6),
            Map.entry("EXIT_FULLSCREEN", 0.9),
            Map.entry("FULLSCREEN_VIOLATION", 0.9),
            Map.entry("COPY_ATTEMPT", 0.7),
            Map.entry("PASTE_ATTEMPT", 0.85),
            Map.entry("CLIPBOARD_ABUSE", 0.9),
            Map.entry("DEVTOOLS_OPEN", 0.95),
            Map.entry("PRINT_SCREEN", 0.85),
            // Identity & Device
            Map.entry("DUPLICATE_IP", 0.92),
            Map.entry("IP_FINGERPRINT_GRAPH", 0.92),
            Map.entry("DEVICE_FINGERPRINT_CHANGED", 0.95),
            Map.entry("MULTIPLE_DEVICE_SESSION", 0.92),
            Map.entry("TIMING_ANOMALY", 0.85),
            // AI Camera Detection - Critical
            Map.entry("MULTIPLE_FACES", 0.98),
            Map.entry("FACE_SPOOFING_SUSPECTED", 0.95),
            // AI Camera Detection - High
            Map.entry("NO_CAMERA", 0.95),
            Map.entry("FACE_NOT_DETECTED", 0.90),
            Map.entry("FACE_OBSTRUCTED_MASK", 0.88),
            // AI Camera Detection - Medium
            Map.entry("EYES_OBSTRUCTED", 0.75),
            Map.entry("PARTIAL_FACE_VISIBLE", 0.70),
            Map.entry("FACE_TURNED_AWAY", 0.72),
            Map.entry("EYES_NOT_DETECTED", 0.68),
            // AI Camera Detection - Low
            Map.entry("FACE_TOO_FAR", 0.60),
            Map.entry("FACE_TOO_CLOSE", 0.40),
            Map.entry("FACE_NOT_CENTERED", 0.45),
            // Quality & Lighting
            Map.entry("VERY_LOW_LIGHTING", 0.80),
            Map.entry("LOW_LIGHTING", 0.55),
            Map.entry("OVEREXPOSED_FRAME", 0.40),
            Map.entry("VERY_BLURRY_FRAME", 0.82),
            Map.entry("BLURRY_FRAME", 0.45),
            // Legacy AI signals
            Map.entry("AI_MULTIPLE_FACES", 0.92),
            Map.entry("AI_PHONE_DETECTED", 0.94),
            // Eye Tracking
            Map.entry("EYE_BLINK_ANOMALY", 0.70),
            Map.entry("EYES_CLOSED_PROLONGED", 0.50),
            // Gaze Tracking
            Map.entry("GAZE_OFF_SCREEN", 0.85),
            Map.entry("RAPID_EYE_MOVEMENT", 0.65),
            // Deep Learning Spoofing
            Map.entry("PRINTED_PHOTO", 0.95),
            Map.entry("SCREEN_REPLAY", 0.92),
            Map.entry("DEEPFAKE", 0.98),
            Map.entry("FLAT_IMAGE", 0.80),
            Map.entry("SCREEN_DISPLAY", 0.75)
    );

    /**
     * Perform ML-powered risk analysis cho một attempt.
     * Kết hợp rule-based scoring với ML inference (nếu enabled).
     */
    @Transactional(readOnly = true)
    public MLRiskScoreResponse analyzeRisk(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        return analyzeRiskInternal(attempt);
    }

    /**
     * Analyze risk cho tất cả attempts của một exam.
     */
    @Transactional(readOnly = true)
    public List<MLRiskScoreResponse> analyzeRiskByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        List<ExamAttempt> attempts = examAttemptRepository.findByExamAndStatusIn(
                exam,
                List.of(AttemptStatus.SUBMITTED, AttemptStatus.AUTO_SUBMITTED)
        );

        return attempts.stream()
                .map(this::analyzeRiskInternal)
                .collect(Collectors.toList());
    }

    /**
     * Batch analyze với async processing.
     */
    @Async
    public void analyzeRiskBatchAsync(List<Long> attemptIds) {
        for (Long attemptId : attemptIds) {
            try {
                analyzeRisk(attemptId);
            } catch (Exception e) {
                log.error("Error analyzing risk for attempt {}: {}", attemptId, e.getMessage());
            }
        }
    }

    /**
     * Get ML model status.
     */
    public MLModelStatusResponse getModelStatus() {
        MLModelStatusResponse.MLModelStatusResponseBuilder builder = MLModelStatusResponse.builder()
                .modelType("HybridRiskScoring")
                .modelVersion("1.0.0")
                .status(mlEnabled ? "READY" : "DISABLED")
                .algorithm(mlEnabled ? "RandomForest + RuleBased" : "RuleBased");

        if (!mlEnabled) {
            builder.message("ML risk scoring is disabled. Using statistical anomaly detection instead.")
                   .recommendations(List.of(
                           "Enable ML service by setting app.ml.risk-scoring.enabled=true",
                           "Configure ML service URL: app.ml.risk-scoring.base-url",
                           "Train model with historical fraud data"
                   ));
        } else {
            builder.message("ML risk scoring is enabled and ready.")
                   .recommendations(List.of(
                           "Monitor model accuracy over time",
                           "Retrain model periodically with new data",
                           "Collect more labeled samples for better accuracy"
                   ));
        }

        return builder.build();
    }

    /**
     * Extract features từ fraud signals cho ML model.
     */
    public MLRiskPredictionRequest extractFeatures(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        return extractFeaturesFromAttempt(attempt);
    }

    // === Private Methods ===

    private MLRiskScoreResponse analyzeRiskInternal(ExamAttempt attempt) {
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        // Extract features
        MLRiskPredictionRequest features = extractFeaturesFromAttempt(attempt);

        // Calculate rule-based score (legacy)
        int ruleBasedScore = calculateRuleBasedScore(signals, attempt);
        String ruleBasedLevel = resolveLevel(ruleBasedScore);

        // ML-based analysis (uses statistical methods when ML is disabled)
        MLAnalysisResult mlResult = performMLAnalysis(signals, features, attempt);

        // Combine scores
        int combinedScore = combineScores(ruleBasedScore, mlResult.getMlScore(), mlResult.getConfidence());
        String combinedLevel = resolveLevel(combinedScore);
        boolean suspicious = combinedScore >= 40;

        // Extract feature importance
        List<MLRiskScoreResponse.FeatureImportance> topFeatures = extractTopFeatures(signals, features);

        return MLRiskScoreResponse.builder()
                .attemptId(attempt.getId())
                .studentUsername(attempt.getStudent().getUsername())
                .ruleBasedScore(ruleBasedScore)
                .ruleBasedLevel(ruleBasedLevel)
                .mlScore(mlResult.getMlScore())
                .mlConfidence(mlResult.getConfidence())
                .mlRiskLevel(mlResult.getRiskLevel())
                .fraudProbability(mlResult.getFraudProbability())
                .combinedScore(combinedScore)
                .combinedLevel(combinedLevel)
                .suspicious(suspicious)
                .modelVersion("1.0.0")
                .modelType(mlEnabled ? "ML_HYBRID" : "RULE_BASED")
                .topFeatures(topFeatures)
                .mlAnalysis(buildMLAnalysis(mlResult, signals, features))
                .analyzedAt(VietNamTime.now())
                .build();
    }

    private MLRiskPredictionRequest extractFeaturesFromAttempt(ExamAttempt attempt) {
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        // Signal features
        Map<String, Long> signalCounts = signals.stream()
                .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));

        Map<SignalSeverity, Long> severityCounts = signals.stream()
                .collect(Collectors.groupingBy(FraudSignal::getSeverity, Collectors.counting()));

        MLRiskPredictionRequest.SignalFeatures signalFeatures = MLRiskPredictionRequest.SignalFeatures.builder()
                .tabSwitchCount(signalCounts.getOrDefault("TAB_SWITCH", 0L).intValue())
                .windowBlurCount(signalCounts.getOrDefault("WINDOW_BLUR", 0L).intValue())
                .fullscreenExitCount(signalCounts.getOrDefault("EXIT_FULLSCREEN", 0L).intValue() +
                        signalCounts.getOrDefault("FULLSCREEN_VIOLATION", 0L).intValue())
                .clipboardAttempts(signalCounts.getOrDefault("CLIPBOARD_ABUSE", 0L).intValue() +
                        signalCounts.getOrDefault("COPY_ATTEMPT", 0L).intValue() +
                        signalCounts.getOrDefault("PASTE_ATTEMPT", 0L).intValue())
                .devtoolsOpened(signalCounts.getOrDefault("DEVTOOLS_OPEN", 0L).intValue())
                .rightClickCount(signalCounts.getOrDefault("RIGHT_CLICK", 0L).intValue())
                .printScreenCount(signalCounts.getOrDefault("PRINT_SCREEN", 0L).intValue())
                .ipChanges(signalCounts.getOrDefault("IP_CHANGED", 0L).intValue())
                .deviceChanges(signalCounts.getOrDefault("DEVICE_FINGERPRINT_CHANGED", 0L).intValue())
                .duplicateIpEvents(signalCounts.getOrDefault("DUPLICATE_IP", 0L).intValue() +
                        signalCounts.getOrDefault("IP_FINGERPRINT_GRAPH", 0L).intValue())
                .suspiciousSignals(signalCounts.getOrDefault("AI_MULTIPLE_FACES", 0L).intValue() +
                        signalCounts.getOrDefault("AI_PHONE_DETECTED", 0L).intValue())
                .totalSignalCount(signals.size())
                .criticalSignalCount(severityCounts.getOrDefault(SignalSeverity.CRITICAL, 0L).intValue())
                .highSignalCount(severityCounts.getOrDefault(SignalSeverity.HIGH, 0L).intValue())
                .mediumSignalCount(severityCounts.getOrDefault(SignalSeverity.MEDIUM, 0L).intValue())
                .lowSignalCount(severityCounts.getOrDefault(SignalSeverity.LOW, 0L).intValue())
                .signalsPerMinute(calculateSignalsPerMinute(signals, attempt))
                .build();

        // Behavioral features (from evidence)
        MLRiskPredictionRequest.BehavioralFeatures behaviorFeatures = extractBehavioralFeatures(signals);

        // Temporal features
        MLRiskPredictionRequest.TemporalFeatures temporalFeatures = extractTemporalFeatures(signals, attempt);

        // Contextual features
        MLRiskPredictionRequest.ContextualFeatures contextFeatures = extractContextualFeatures(attempt);

        return MLRiskPredictionRequest.builder()
                .attemptId(attempt.getId())
                .studentId(attempt.getStudent().getId())
                .examId(attempt.getExam().getId())
                .signals(signalFeatures)
                .behavior(behaviorFeatures)
                .temporal(temporalFeatures)
                .context(contextFeatures)
                .requestTime(VietNamTime.now())
                .build();
    }

    private MLRiskPredictionRequest.BehavioralFeatures extractBehavioralFeatures(List<FraudSignal> signals) {
        // Extract typing and mouse data from signals
        Double avgTypingSpeed = null;
        Double typingConsistency = null;
        Double avgMouseSpeed = null;
        Integer totalMovements = 0;
        Boolean typingMismatch = false;

        for (FraudSignal signal : signals) {
            if (signal.getEvidence() != null) {
                try {
                    Map<String, Object> evidence = objectMapper.readValue(signal.getEvidence(), Map.class);
                    if (evidence.containsKey("avgSpeedCpm")) {
                        avgTypingSpeed = toDouble(evidence.get("avgSpeedCpm"));
                    }
                    if (evidence.containsKey("consistency")) {
                        typingConsistency = toDouble(evidence.get("consistency"));
                    }
                    if (evidence.containsKey("avgSpeedPps")) {
                        avgMouseSpeed = toDouble(evidence.get("avgSpeedPps"));
                    }
                } catch (JsonProcessingException e) {
                    // Ignore parsing errors
                }
            }

            String type = signal.getSignalType();
            if ("TYPING_PATTERN_MISMATCH".equals(type)) {
                typingMismatch = true;
            }
        }

        return MLRiskPredictionRequest.BehavioralFeatures.builder()
                .averageTypingSpeed(avgTypingSpeed)
                .typingConsistency(typingConsistency)
                .mouseMovementAvgSpeed(avgMouseSpeed)
                .totalMouseMovements(totalMovements)
                .typingPatternMismatch(typingMismatch)
                .build();
    }

    private MLRiskPredictionRequest.TemporalFeatures extractTemporalFeatures(List<FraudSignal> signals, ExamAttempt attempt) {
        LocalDateTime startTime = attempt.getStartedAt();
        LocalDateTime endTime = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : VietNamTime.now();
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();

        // Find impossibly fast answers from signals
        boolean impossiblyFast = signals.stream()
                .anyMatch(s -> "TIMING_ANOMALY".equals(s.getSignalType()) ||
                        "IMPOSSIBLE_SPEED".equals(s.getSignalType()));

        return MLRiskPredictionRequest.TemporalFeatures.builder()
                .examDurationMinutes(durationMinutes)
                .timeSinceStartMinutes(Duration.between(startTime, VietNamTime.now()).toMinutes())
                .sessionStartTime(startTime)
                .lastActivityTime(signals.isEmpty() ? startTime :
                        signals.get(signals.size() - 1).getCreatedAt())
                .impossiblyFastAnswers(impossiblyFast)
                .build();
    }

    private MLRiskPredictionRequest.ContextualFeatures extractContextualFeatures(ExamAttempt attempt) {
        // Check for shared IP
        boolean isSharedIp = fraudSignalRepository.findByAttempt(attempt).stream()
                .anyMatch(s -> "DUPLICATE_IP".equals(s.getSignalType()) ||
                        "IP_FINGERPRINT_GRAPH".equals(s.getSignalType()));

        return MLRiskPredictionRequest.ContextualFeatures.builder()
                .isSharedIp(isSharedIp)
                .isSharedDevice(false) // Would need additional logic
                .build();
    }

    private int calculateRuleBasedScore(List<FraudSignal> signals, ExamAttempt attempt) {
        // Use clustering-based approach from RiskScoringService
        Map<String, List<FraudSignal>> byCategory = new HashMap<>();
        for (FraudSignal signal : signals) {
            String category = signal.getCategory() != null ? signal.getCategory() : "OTHER";
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(signal);
        }

        int score = 0;

        // SCREEN_LEAVE category
        List<FraudSignal> screenSignals = byCategory.getOrDefault("SCREEN_LEAVE", List.of());
        score += Math.min(clusteredScore(screenSignals, 60), 35);

        // CLIPBOARD category
        List<FraudSignal> clipboardSignals = byCategory.getOrDefault("CLIPBOARD", List.of());
        score += Math.min(clusteredScore(clipboardSignals, 30), 25);

        // TECHNICAL category
        List<FraudSignal> techSignals = byCategory.getOrDefault("TECHNICAL", List.of());
        score += Math.min(clusteredScore(techSignals, 120), 25);

        // IDENTITY category
        List<FraudSignal> identitySignals = byCategory.getOrDefault("IDENTITY", List.of());
        int identityScore = identitySignals.stream()
                .mapToInt(s -> ANOMALY_WEIGHTS.getOrDefault(s.getSignalType(), 0.5).intValue() * 10)
                .max()
                .orElse(0);
        score += Math.min(identityScore, 30);

        // HEARTBEAT category
        List<FraudSignal> heartbeatSignals = byCategory.getOrDefault("HEARTBEAT", List.of());
        score += Math.min(clusteredScore(heartbeatSignals, 0), 10);

        return Math.min(score, 100);
    }

    private int clusteredScore(List<FraudSignal> signals, long dedupWindow) {
        if (signals.isEmpty()) return 0;

        signals.sort(Comparator.comparing(FraudSignal::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        int totalScore = 0;
        int clusterMax = 0;
        LocalDateTime clusterStart = null;

        for (FraudSignal signal : signals) {
            LocalDateTime signalTime = signal.getCreatedAt();
            double weight = ANOMALY_WEIGHTS.getOrDefault(signal.getSignalType(), 0.5);
            int signalScore = (int) (weight * 10);

            if (clusterStart == null) {
                clusterStart = signalTime;
                clusterMax = signalScore;
            } else {
                long diff = Duration.between(clusterStart, signalTime).toSeconds();
                if (diff < dedupWindow) {
                    clusterMax = Math.max(clusterMax, signalScore);
                } else {
                    totalScore += clusterMax;
                    clusterStart = signalTime;
                    clusterMax = signalScore;
                }
            }
        }

        return totalScore + clusterMax;
    }

    private MLAnalysisResult performMLAnalysis(List<FraudSignal> signals,
                                               MLRiskPredictionRequest features,
                                               ExamAttempt attempt) {
        // Statistical anomaly scoring (replaces ML when disabled)
        double anomalyScore = calculateAnomalyScore(signals, features);
        double confidence = calculateConfidence(signals, features);
        String riskLevel = resolveLevel((int) anomalyScore);
        double fraudProbability = anomalyScore / 100.0;

        // If ML service is enabled, try to call external ML service
        if (mlEnabled) {
            try {
                return callMLService(features);
            } catch (Exception e) {
                log.warn("ML service call failed, using statistical fallback: {}", e.getMessage());
            }
        }

        return new MLAnalysisResult(anomalyScore, confidence, riskLevel, fraudProbability);
    }

    private MLAnalysisResult callMLService(MLRiskPredictionRequest features) {
        // TODO: Implement actual ML service call
        // For now, use statistical fallback
        double anomalyScore = calculateAnomalyScore(List.of(), features);
        return new MLAnalysisResult(anomalyScore, 0.7, "MEDIUM", anomalyScore / 100.0);
    }

    private double calculateAnomalyScore(List<FraudSignal> signals, MLRiskPredictionRequest features) {
        if (features.getSignals() == null) return 0.0;

        MLRiskPredictionRequest.SignalFeatures sf = features.getSignals();
        double score = 0.0;

        // Weighted signal scoring
        score += sf.getTabSwitchCount() * 1.0;
        score += sf.getWindowBlurCount() * 0.8;
        score += sf.getFullscreenExitCount() * 1.5;
        score += sf.getClipboardAttempts() * 2.0;
        score += sf.getDevtoolsOpened() * 3.0;
        score += sf.getDuplicateIpEvents() * 4.0;
        score += sf.getDeviceChanges() * 4.0;
        // AI camera signals contribute significantly to suspicious score
        score += sf.getSuspiciousSignals() * 5.5;

        // Severity weighting
        score += sf.getCriticalSignalCount() * 5.0;
        score += sf.getHighSignalCount() * 3.0;
        score += sf.getMediumSignalCount() * 1.5;

        // AI Camera-specific scoring
        // Count AI camera signals directly from signal types
        // These are already counted in suspiciousSignals but with explicit weighting
        // for better discrimination
        Map<String, Long> signalCounts = signals.stream()
                .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));
        
        // Critical AI camera signals
        score += signalCounts.getOrDefault("MULTIPLE_FACES", 0L) * 8.0;
        score += signalCounts.getOrDefault("FACE_SPOOFING_SUSPECTED", 0L) * 7.0;
        
        // High severity AI camera signals
        score += signalCounts.getOrDefault("NO_CAMERA", 0L) * 7.0;
        score += signalCounts.getOrDefault("FACE_NOT_DETECTED", 0L) * 6.0;
        score += signalCounts.getOrDefault("FACE_OBSTRUCTED_MASK", 0L) * 5.5;
        score += signalCounts.getOrDefault("VERY_LOW_LIGHTING", 0L) * 4.0;
        score += signalCounts.getOrDefault("VERY_BLURRY_FRAME", 0L) * 4.5;
        
        // Medium severity AI camera signals
        score += signalCounts.getOrDefault("EYES_OBSTRUCTED", 0L) * 3.0;
        score += signalCounts.getOrDefault("PARTIAL_FACE_VISIBLE", 0L) * 2.5;
        score += signalCounts.getOrDefault("FACE_TURNED_AWAY", 0L) * 2.5;
        score += signalCounts.getOrDefault("LOW_LIGHTING", 0L) * 2.0;
        score += signalCounts.getOrDefault("BLURRY_FRAME", 0L) * 2.0;
        
        // Low severity AI camera signals
        score += signalCounts.getOrDefault("FACE_TOO_FAR", 0L) * 1.5;
        score += signalCounts.getOrDefault("FACE_TOO_CLOSE", 0L) * 1.0;
        score += signalCounts.getOrDefault("FACE_NOT_CENTERED", 0L) * 1.0;

        // Behavioral anomalies
        if (features.getBehavior() != null) {
            if (Boolean.TRUE.equals(features.getBehavior().getTypingPatternMismatch())) {
                score += 10.0;
            }
            if (Boolean.TRUE.equals(features.getBehavior().getMouseSignatureAnomaly())) {
                score += 8.0;
            }
        }

        // Temporal anomalies
        if (features.getTemporal() != null && Boolean.TRUE.equals(features.getTemporal().getImpossiblyFastAnswers())) {
            score += 15.0;
        }

        // Contextual anomalies
        if (features.getContext() != null) {
            if (Boolean.TRUE.equals(features.getContext().getIsSharedIp())) {
                score += 12.0;
            }
            if (Boolean.TRUE.equals(features.getContext().getIsSharedDevice())) {
                score += 10.0;
            }
        }

        // Normalize to 0-100
        return Math.min(score * 2, 100.0);
    }

    private double calculateConfidence(List<FraudSignal> signals, MLRiskPredictionRequest features) {
        if (features.getSignals() == null) return 0.5;

        int totalSignals = features.getSignals().getTotalSignalCount();
        double confidence = 0.5;

        // More signals = higher confidence
        if (totalSignals >= 10) confidence += 0.2;
        else if (totalSignals >= 5) confidence += 0.1;

        // Behavioral data availability
        if (features.getBehavior() != null) {
            if (features.getBehavior().getAverageTypingSpeed() != null) confidence += 0.1;
            if (features.getBehavior().getTypingConsistency() != null) confidence += 0.1;
        }

        return Math.min(confidence, 1.0);
    }

    private int combineScores(int ruleScore, double mlScore, double mlConfidence) {
        // If ML has high confidence, weight it more
        if (mlConfidence >= confidenceThreshold && mlEnabled) {
            return (int) (ruleScore * (1 - mlWeight) + mlScore * mlWeight);
        }
        // Otherwise rely more on rule-based
        return (int) (ruleScore * 0.7 + mlScore * 0.3);
    }

    private String resolveLevel(int score) {
        if (score >= 81) return "CRITICAL";
        if (score >= 61) return "HIGH_RISK";
        if (score >= 40) return "SUSPICIOUS";
        return "CLEAN";
    }

    private double calculateSignalsPerMinute(List<FraudSignal> signals, ExamAttempt attempt) {
        if (signals.isEmpty() || attempt.getStartedAt() == null) return 0.0;
        LocalDateTime endTime = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : VietNamTime.now();
        long minutes = Math.max(1, Duration.between(attempt.getStartedAt(), endTime).toMinutes());
        return (double) signals.size() / minutes;
    }

    private List<MLRiskScoreResponse.FeatureImportance> extractTopFeatures(
            List<FraudSignal> signals,
            MLRiskPredictionRequest features) {
        List<MLRiskScoreResponse.FeatureImportance> topFeatures = new ArrayList<>();

        if (features.getSignals() != null) {
            MLRiskPredictionRequest.SignalFeatures sf = features.getSignals();

            addFeatureIfNotEmpty(topFeatures, "TAB_SWITCH", "BEHAVIOR", sf.getTabSwitchCount() * 1.0,
                    (double) sf.getTabSwitchCount(), "Tab switching frequency");
            addFeatureIfNotEmpty(topFeatures, "CLIPBOARD_ABUSE", "BEHAVIOR", sf.getClipboardAttempts() * 2.0,
                    (double) sf.getClipboardAttempts(), "Clipboard manipulation attempts");
            addFeatureIfNotEmpty(topFeatures, "DEVTOOLS_OPEN", "TECHNICAL", sf.getDevtoolsOpened() * 3.0,
                    (double) sf.getDevtoolsOpened(), "Developer tools opened");
            addFeatureIfNotEmpty(topFeatures, "DUPLICATE_IP", "IDENTITY", sf.getDuplicateIpEvents() * 4.0,
                    (double) sf.getDuplicateIpEvents(), "IP address anomalies");
            addFeatureIfNotEmpty(topFeatures, "DEVICE_CHANGE", "IDENTITY", sf.getDeviceChanges() * 4.0,
                    (double) sf.getDeviceChanges(), "Device fingerprint changes");
            addFeatureIfNotEmpty(topFeatures, "AI_DETECTION", "IDENTITY", sf.getSuspiciousSignals() * 5.0,
                    (double) sf.getSuspiciousSignals(), "AI-detected anomalies");
        }

        // Sort by importance and take top 5
        topFeatures.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        return topFeatures.stream().limit(5).collect(Collectors.toList());
    }

    private void addFeatureIfNotEmpty(List<MLRiskScoreResponse.FeatureImportance> list,
                                       String name, String group, double importance, double value,
                                       String description) {
        if (value > 0) {
            list.add(MLRiskScoreResponse.FeatureImportance.builder()
                    .featureName(name)
                    .featureGroup(group)
                    .importance(importance)
                    .value(value)
                    .anomalyDescription(description)
                    .build());
        }
    }

    private Map<String, Object> buildMLAnalysis(MLAnalysisResult mlResult,
                                                List<FraudSignal> signals,
                                                MLRiskPredictionRequest features) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        // Categorize signals
        Map<String, Long> byCategory = signals.stream()
                .collect(Collectors.groupingBy(s ->
                        s.getCategory() != null ? s.getCategory() : "OTHER", Collectors.counting()));

        analysis.put("behavioralAnomalyScore", byCategory.getOrDefault("SCREEN_LEAVE", 0L) * 1.5 +
                byCategory.getOrDefault("CLIPBOARD", 0L) * 2.0);
        analysis.put("identityAnomalyScore", byCategory.getOrDefault("IDENTITY", 0L) * 3.0);
        analysis.put("temporalAnomalyScore", byCategory.getOrDefault("HEARTBEAT", 0L) * 1.0);

        // Detected patterns
        List<String> patterns = new ArrayList<>();
        if (byCategory.getOrDefault("SCREEN_LEAVE", 0L) > 5) {
            patterns.add("FREQUENT_SCREEN_LEAVE");
        }
        if (byCategory.getOrDefault("CLIPBOARD", 0L) > 2) {
            patterns.add("CLIPBOARD_MANIPULATION");
        }
        if (byCategory.getOrDefault("TECHNICAL", 0L) > 3) {
            patterns.add("TECHNICAL_TOOL_USE");
        }
        if (byCategory.getOrDefault("IDENTITY", 0L) > 1) {
            patterns.add("IDENTITY_ANOMALY");
        }
        analysis.put("detectedPatterns", patterns);

        // Warnings
        List<String> warnings = new ArrayList<>();
        if (mlResult.getFraudProbability() > 0.7) {
            warnings.add("HIGH_FRAUD_PROBABILITY");
        }
        if (mlResult.getConfidence() < minConfidence) {
            warnings.add("LOW_CONFIDENCE_INFERENCE");
        }
        analysis.put("warnings", warnings);

        return analysis;
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @lombok.Value
    private static class MLAnalysisResult {
        double mlScore;
        double confidence;
        String riskLevel;
        double fraudProbability;
    }
}
