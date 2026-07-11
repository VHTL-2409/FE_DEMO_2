package com.example.demo.service;

import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MLRiskScoringService {

    private static final Logger log = LoggerFactory.getLogger(MLRiskScoringService.class);

    private final FraudSignalRepository fraudSignalRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;

    
    @Value("${app.ml.risk-scoring.enabled:false}")
    private boolean mlEnabled;

    @Value("${app.ml.risk-scoring.base-url:http://localhost:8090}")
    private String mlServiceUrl;

    @Value("${app.ml.risk-scoring.timeout-ms:5000}")
    private int mlTimeoutMs;

    @Value("${app.ml.risk-scoring.hybrid-weight:0.6}")
    private double mlWeight; 

    @Value("${app.ml.risk-scoring.min-confidence:0.5}")
    private double minConfidence;

    @Value("${app.ml.risk-scoring.confidence-threshold:0.7}")
    private double confidenceThreshold;

    @Value("${app.ai-service.api-key:${APP_AI_SERVICE_API_KEY:${AI_SERVICE_API_KEY:}}}")
    private String aiServiceApiKey;

    
    private static final Map<String, Double> ANOMALY_WEIGHTS = Map.ofEntries(
            
            Map.entry("TAB_SWITCH", 0.8),
            Map.entry("WINDOW_BLUR", 0.6),
            Map.entry("EXIT_FULLSCREEN", 0.9),
            Map.entry("COPY_PASTE", 0.8),
            Map.entry("DEVTOOLS_OPEN", 0.95),
            Map.entry("PRINT_SCREEN", 0.85),
            
            Map.entry("DUPLICATE_IP", 0.92),
            Map.entry("IP_FINGERPRINT_GRAPH", 0.92),
            Map.entry("DEVICE_FINGERPRINT_CHANGED", 0.95),
            Map.entry("MULTIPLE_DEVICE_SESSION", 0.92),
            Map.entry("TIMING_ANOMALY", 0.85),
            
            Map.entry("MULTIPLE_FACES", 0.98),
            Map.entry("FACE_SPOOFING_SUSPECTED", 0.95),
            
            Map.entry("NO_CAMERA", 0.95),
            Map.entry("NO_MIC", 0.95),
            Map.entry("FACE_NOT_DETECTED", 0.90),
            Map.entry("FACE_OBSTRUCTED_MASK", 0.88),
            
            Map.entry("EYES_OBSTRUCTED", 0.75),
            Map.entry("PARTIAL_FACE_VISIBLE", 0.70),
            Map.entry("FACE_TURNED_AWAY", 0.72),
            Map.entry("EYES_NOT_DETECTED", 0.68),
            
            Map.entry("FACE_TOO_FAR", 0.60),
            Map.entry("FACE_TOO_CLOSE", 0.40),
            Map.entry("FACE_NOT_CENTERED", 0.45),
            
            Map.entry("VERY_LOW_LIGHTING", 0.80),
            Map.entry("LOW_LIGHTING", 0.55),
            Map.entry("OVEREXPOSED_FRAME", 0.40),
            Map.entry("VERY_BLURRY_FRAME", 0.82),
            Map.entry("BLURRY_FRAME", 0.45),
            Map.entry("AI_PHONE_DETECTED", 0.94),
            Map.entry("AI_SPEAKING_DETECTED", 0.75),
            
            Map.entry("EYE_BLINK_ANOMALY", 0.70),
            Map.entry("EYES_CLOSED_PROLONGED", 0.50),
            
            Map.entry("GAZE_OFF_SCREEN", 0.85),
            Map.entry("RAPID_EYE_MOVEMENT", 0.65),
            
            Map.entry("PRINTED_PHOTO", 0.95),
            Map.entry("SCREEN_REPLAY", 0.92),
            Map.entry("DEEPFAKE", 0.98),
            Map.entry("LOW_LIVENESS", 0.88),
            Map.entry("FLAT_IMAGE", 0.80),
            Map.entry("SCREEN_DISPLAY", 0.75)
    );

    

    @Transactional(readOnly = true)
    public MLRiskScoreResponse analyzeRisk(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        return analyzeRiskInternal(attempt);
    }

    

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

    

    public MLModelStatusResponse getModelStatus() {
        MLModelStatusResponse.MLModelStatusResponseBuilder builder = MLModelStatusResponse.builder()
                .modelType("HybridRiskScoring")
                .modelVersion("1.0.0")
                .status(mlEnabled ? "CHECKING" : "DISABLED")
                .algorithm(mlEnabled ? "ExternalML + RuleBased" : "RuleBased");

        if (!mlEnabled) {
            builder.message("ML risk scoring is disabled. Rule-based risk scoring is the canonical source.")
                   .recommendations(List.of(
                           "Rule-based risk scoring is the canonical source for MCQ exams",
                           "Enable app.ml.risk-scoring.enabled=true only when an external ML model is available",
                           "Avoid enabling statistical fallback as a second risk source"
                   ));
        } else {
            Map<?, ?> status = queryMlServiceStatus();
            if (status.isEmpty()) {
                builder.status("UNAVAILABLE")
                       .algorithm("ExternalML + RuleBased")
                       .warnings(List.of("External ML risk service is enabled but unavailable. Attempts will use statistical fallback."))
                       .message("ML risk scoring is enabled, but the external service is not reachable.")
                       .recommendations(List.of(
                               "Start the AI service and verify /ml/risk/status",
                               "Keep app.ml.risk-scoring.enabled=false until the service is stable",
                               "Review fallback scoring before trusting high-risk decisions"
                       ));
            } else {
                boolean available = booleanFrom(status, "available").orElse(false);
                boolean trainedModelLoaded = booleanFrom(status, "trainedModelLoaded", "trained_model_loaded").orElse(false);
                String algorithm = stringFrom(status, "algorithm").orElse("ExternalMLRiskModel");
                String version = stringFrom(status, "version", "modelVersion", "model_version").orElse("1.0.0");
                builder.status(available ? (trainedModelLoaded ? "READY" : "HEURISTIC_READY") : "UNAVAILABLE")
                       .modelVersion(version)
                       .algorithm(algorithm)
                       .warnings(trainedModelLoaded ? List.of() : List.of("External risk service is available but no trained model is loaded."))
                       .message(stringFrom(status, "message").orElse("ML risk scoring service status received."))
                       .recommendations(List.of(
                               "Monitor model accuracy over time",
                               "Use HEURISTIC_READY as baseline only, not as a trained ML model",
                               "Collect labeled samples before switching to trained-model decisions"
                       ));
            }
        }

        return builder.build();
    }

    

    public MLRiskPredictionRequest extractFeatures(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        return extractFeaturesFromAttempt(attempt);
    }

    

    private MLRiskScoreResponse analyzeRiskInternal(ExamAttempt attempt) {
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        
        MLRiskPredictionRequest features = extractFeaturesFromAttempt(attempt);

        
        int ruleBasedScore = calculateRuleBasedScore(signals, attempt);
        String ruleBasedLevel = resolveLevel(ruleBasedScore);

        
        MLAnalysisResult mlResult = performMLAnalysis(signals, features, attempt);

        
        int combinedScore = combineScores(ruleBasedScore, mlResult.getMlScore(), mlResult.getConfidence());
        String combinedLevel = resolveLevel(combinedScore);
        boolean suspicious = combinedScore >= 40;

        
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
                .scoringStatus(mlResult.getStatus())
                .scoringSource(mlResult.getSource())
                .algorithm(mlResult.getAlgorithm())
                .topFeatures(topFeatures)
                .mlAnalysis(buildMLAnalysis(mlResult, signals, features))
                .analyzedAt(VietNamTime.now())
                .build();
    }

    private MLRiskPredictionRequest extractFeaturesFromAttempt(ExamAttempt attempt) {
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        
        Map<String, Long> signalCounts = signals.stream()
                .collect(Collectors.groupingBy(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()), Collectors.counting()));

        Map<SignalSeverity, Long> severityCounts = signals.stream()
                .collect(Collectors.groupingBy(FraudSignal::getSeverity, Collectors.counting()));

        MLRiskPredictionRequest.SignalFeatures signalFeatures = MLRiskPredictionRequest.SignalFeatures.builder()
                .tabSwitchCount(signalCounts.getOrDefault("TAB_SWITCH", 0L).intValue())
                .windowBlurCount(signalCounts.getOrDefault("WINDOW_BLUR", 0L).intValue())
                .fullscreenExitCount(signalCounts.getOrDefault("EXIT_FULLSCREEN", 0L).intValue())
                .clipboardAttempts(signalCounts.getOrDefault("COPY_PASTE", 0L).intValue())
                .devtoolsOpened(signalCounts.getOrDefault("DEVTOOLS_OPEN", 0L).intValue())
                .rightClickCount(signalCounts.getOrDefault("RIGHT_CLICK", 0L).intValue())
                .printScreenCount(signalCounts.getOrDefault("PRINT_SCREEN", 0L).intValue())
                .ipChanges(signalCounts.getOrDefault("IP_CHANGED", 0L).intValue())
                .deviceChanges(signalCounts.getOrDefault("DEVICE_FINGERPRINT_CHANGED", 0L).intValue())
                .duplicateIpEvents(signalCounts.getOrDefault("DUPLICATE_IP", 0L).intValue() +
                        signalCounts.getOrDefault("IP_FINGERPRINT_GRAPH", 0L).intValue())
                .suspiciousSignals(signalCounts.getOrDefault("MULTIPLE_FACES", 0L).intValue() +
                        signalCounts.getOrDefault("AI_PHONE_DETECTED", 0L).intValue())
                .totalSignalCount(signals.size())
                .criticalSignalCount(severityCounts.getOrDefault(SignalSeverity.CRITICAL, 0L).intValue())
                .highSignalCount(severityCounts.getOrDefault(SignalSeverity.HIGH, 0L).intValue())
                .mediumSignalCount(severityCounts.getOrDefault(SignalSeverity.MEDIUM, 0L).intValue())
                .lowSignalCount(severityCounts.getOrDefault(SignalSeverity.LOW, 0L).intValue())
                .signalsPerMinute(calculateSignalsPerMinute(signals, attempt))
                .build();

        
        MLRiskPredictionRequest.BehavioralFeatures behaviorFeatures = extractBehavioralFeatures(signals);

        
        MLRiskPredictionRequest.TemporalFeatures temporalFeatures = extractTemporalFeatures(signals, attempt);

        
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
        return MLRiskPredictionRequest.BehavioralFeatures.builder()
                .totalMouseMovements(0)
                .typingPatternMismatch(false)
                .build();
    }

    private MLRiskPredictionRequest.TemporalFeatures extractTemporalFeatures(List<FraudSignal> signals, ExamAttempt attempt) {
        LocalDateTime startTime = attempt.getStartedAt() != null
                ? attempt.getStartedAt()
                : attempt.getJoinedAt();
        if (startTime == null) {
            startTime = signals.stream()
                    .map(FraudSignal::getCreatedAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(VietNamTime.now());
        }
        LocalDateTime endTime = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : VietNamTime.now();
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();

        
        boolean impossiblyFast = signals.stream()
                .anyMatch(s -> {
                    String type = FraudSignalTypeNormalizer.canonical(s.getSignalType());
                    return "TIMING_ANOMALY".equals(type) || "IMPOSSIBLE_SPEED".equals(type);
                });

        return MLRiskPredictionRequest.TemporalFeatures.builder()
                .examDurationMinutes(durationMinutes)
                .timeSinceStartMinutes(Duration.between(startTime, VietNamTime.now()).toMinutes())
                .sessionStartTime(startTime)
                .lastActivityTime(signals.stream()
                        .map(FraudSignal::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(startTime))
                .impossiblyFastAnswers(impossiblyFast)
                .build();
    }

    private MLRiskPredictionRequest.ContextualFeatures extractContextualFeatures(ExamAttempt attempt) {
        
        boolean isSharedIp = fraudSignalRepository.findByAttempt(attempt).stream()
                .anyMatch(s -> {
                    String type = FraudSignalTypeNormalizer.canonical(s.getSignalType());
                    return "DUPLICATE_IP".equals(type) || "IP_FINGERPRINT_GRAPH".equals(type);
                });

        return MLRiskPredictionRequest.ContextualFeatures.builder()
                .isSharedIp(isSharedIp)
                .isSharedDevice(false) 
                .build();
    }

    private int calculateRuleBasedScore(List<FraudSignal> signals, ExamAttempt attempt) {
        
        Map<String, List<FraudSignal>> byCategory = new HashMap<>();
        for (FraudSignal signal : signals) {
            String category = signal.getCategory() != null ? signal.getCategory() : "OTHER";
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(signal);
        }

        int score = 0;

        
        List<FraudSignal> screenSignals = byCategory.getOrDefault("SCREEN_LEAVE", List.of());
        score += Math.min(clusteredScore(screenSignals, 60), 35);

        
        List<FraudSignal> clipboardSignals = byCategory.getOrDefault("CLIPBOARD", List.of());
        score += Math.min(clusteredScore(clipboardSignals, 30), 25);

        
        List<FraudSignal> techSignals = byCategory.getOrDefault("TECHNICAL", List.of());
        score += Math.min(clusteredScore(techSignals, 120), 25);

        
        List<FraudSignal> identitySignals = byCategory.getOrDefault("IDENTITY", List.of());
        int identityScore = identitySignals.stream()
                .mapToInt(s -> (int) Math.round(ANOMALY_WEIGHTS.getOrDefault(FraudSignalTypeNormalizer.canonical(s.getSignalType()), 0.5) * 10.0))
                .max()
                .orElse(0);
        score += Math.min(identityScore, 30);

        
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
            double weight = ANOMALY_WEIGHTS.getOrDefault(FraudSignalTypeNormalizer.canonical(signal.getSignalType()), 0.5);
            int signalScore = (int) (weight * 10);

            if (signalTime == null || clusterStart == null) {
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
        if (!mlEnabled) {
            return new MLAnalysisResult(
                    0.0,
                    0.0,
                    "DISABLED",
                    0.0,
                    "DISABLED",
                    "RULE_BASED",
                    "RuleBased"
            );
        }

        
        double anomalyScore = calculateAnomalyScore(signals, features);
        double confidence = calculateConfidence(signals, features);
        String riskLevel = resolveLevel((int) anomalyScore);
        double fraudProbability = anomalyScore / 100.0;

        try {
            return callMLService(features);
        } catch (Exception e) {
            log.warn("ML service call failed, using statistical fallback: {}", e.getMessage());
        }

        return new MLAnalysisResult(
                anomalyScore,
                confidence,
                riskLevel,
                fraudProbability,
                "FALLBACK_STATISTICAL",
                "STATISTICAL_FALLBACK",
                "WeightedSignalStatisticalFallback"
        );
    }

    private MLAnalysisResult callMLService(MLRiskPredictionRequest features) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(mlTimeoutMs, 1000));
        factory.setReadTimeout(Math.max(mlTimeoutMs, 1000));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
            headers.set("X-API-Key", aiServiceApiKey.trim());
        }

        RestTemplate restTemplate = new RestTemplate(factory);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                mlServiceUrl.replaceAll("/+$", "") + "/ml/risk/predict",
                new HttpEntity<>(features, headers),
                Map.class
        );

        Map<?, ?> body = response.getBody();
        if (body == null || body.isEmpty()) {
            throw new IllegalStateException("ML service returned empty response");
        }

        double mlScore = numberFrom(body, "mlScore", "ml_score", "score", "riskScore", "risk_score", "combinedScore", "combined_score").orElse(0.0);
        double confidence = numberFrom(body, "confidence", "mlConfidence", "ml_confidence").orElse(0.7);
        double fraudProbability = numberFrom(body, "fraudProbability", "fraud_probability", "probability")
                .orElse(Math.max(0.0, Math.min(1.0, mlScore / 100.0)));
        String riskLevel = stringFrom(body, "riskLevel", "risk_level", "level", "mlRiskLevel", "ml_risk_level")
                .orElse(resolveLevel((int) Math.round(mlScore)));
        Map<?, ?> diagnostics = body.get("diagnostics") instanceof Map<?, ?> map ? map : Map.of();
        String algorithm = stringFrom(diagnostics, "algorithm")
                .or(() -> stringFrom(body, "algorithm", "model", "modelVersion", "model_version"))
                .orElse("ExternalMLRiskModel");

        return new MLAnalysisResult(
                Math.max(0.0, Math.min(100.0, mlScore)),
                Math.max(0.0, Math.min(1.0, confidence)),
                riskLevel,
                Math.max(0.0, Math.min(1.0, fraudProbability)),
                "READY",
                "EXTERNAL_ML",
                algorithm
        );
    }

    private Map<?, ?> queryMlServiceStatus() {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Math.max(mlTimeoutMs, 1000));
            factory.setReadTimeout(Math.max(mlTimeoutMs, 1000));

            HttpHeaders headers = new HttpHeaders();
            if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
                headers.set("X-API-Key", aiServiceApiKey.trim());
            }

            RestTemplate restTemplate = new RestTemplate(factory);
            ResponseEntity<Map> response = restTemplate.exchange(
                    mlServiceUrl.replaceAll("/+$", "") + "/ml/risk/status",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            return body == null ? Map.of() : body;
        } catch (Exception e) {
            log.warn("ML service status probe failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private Optional<Double> numberFrom(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Number number) {
                return Optional.of(number.doubleValue());
            }
            if (value != null && !String.valueOf(value).isBlank()) {
                try {
                    return Optional.of(Double.parseDouble(String.valueOf(value)));
                } catch (NumberFormatException ignored) {
                    
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> stringFrom(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return Optional.of(String.valueOf(value).trim());
            }
        }
        return Optional.empty();
    }

    private Optional<Boolean> booleanFrom(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean bool) {
                return Optional.of(bool);
            }
            if (value != null && !String.valueOf(value).isBlank()) {
                return Optional.of(Boolean.parseBoolean(String.valueOf(value)));
            }
        }
        return Optional.empty();
    }

    private double calculateAnomalyScore(List<FraudSignal> signals, MLRiskPredictionRequest features) {
        if (features.getSignals() == null) return 0.0;

        MLRiskPredictionRequest.SignalFeatures sf = features.getSignals();
        double score = 0.0;

        
        score += sf.getTabSwitchCount() * 1.0;
        score += sf.getWindowBlurCount() * 0.8;
        score += sf.getFullscreenExitCount() * 1.5;
        score += sf.getClipboardAttempts() * 2.0;
        score += sf.getDevtoolsOpened() * 3.0;
        score += sf.getDuplicateIpEvents() * 4.0;
        score += sf.getDeviceChanges() * 4.0;
        
        score += sf.getSuspiciousSignals() * 5.5;

        
        score += sf.getCriticalSignalCount() * 5.0;
        score += sf.getHighSignalCount() * 3.0;
        score += sf.getMediumSignalCount() * 1.5;

        
        
        
        
        Map<String, Long> signalCounts = signals.stream()
                .collect(Collectors.groupingBy(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()), Collectors.counting()));
        
        
        score += signalCounts.getOrDefault("MULTIPLE_FACES", 0L) * 8.0;
        score += signalCounts.getOrDefault("FACE_SPOOFING_SUSPECTED", 0L) * 7.0;
        
        
        score += signalCounts.getOrDefault("NO_CAMERA", 0L) * 7.0;
        score += signalCounts.getOrDefault("NO_MIC", 0L) * 7.0;
        score += signalCounts.getOrDefault("FACE_NOT_DETECTED", 0L) * 6.0;
        score += signalCounts.getOrDefault("FACE_OBSTRUCTED_MASK", 0L) * 5.5;
        score += signalCounts.getOrDefault("VERY_LOW_LIGHTING", 0L) * 4.0;
        score += signalCounts.getOrDefault("VERY_BLURRY_FRAME", 0L) * 4.5;
        
        
        score += signalCounts.getOrDefault("EYES_OBSTRUCTED", 0L) * 3.0;
        score += signalCounts.getOrDefault("PARTIAL_FACE_VISIBLE", 0L) * 2.5;
        score += signalCounts.getOrDefault("FACE_TURNED_AWAY", 0L) * 2.5;
        score += signalCounts.getOrDefault("LOW_LIGHTING", 0L) * 2.0;
        score += signalCounts.getOrDefault("BLURRY_FRAME", 0L) * 2.0;
        
        
        score += signalCounts.getOrDefault("FACE_TOO_FAR", 0L) * 1.5;
        score += signalCounts.getOrDefault("FACE_TOO_CLOSE", 0L) * 1.0;
        score += signalCounts.getOrDefault("FACE_NOT_CENTERED", 0L) * 1.0;

        
        if (features.getTemporal() != null && Boolean.TRUE.equals(features.getTemporal().getImpossiblyFastAnswers())) {
            score += 15.0;
        }

        
        if (features.getContext() != null) {
            if (Boolean.TRUE.equals(features.getContext().getIsSharedIp())) {
                score += 12.0;
            }
            if (Boolean.TRUE.equals(features.getContext().getIsSharedDevice())) {
                score += 10.0;
            }
        }

        
        return Math.min(score * 2, 100.0);
    }

    private double calculateConfidence(List<FraudSignal> signals, MLRiskPredictionRequest features) {
        if (features.getSignals() == null) return 0.5;

        int totalSignals = features.getSignals().getTotalSignalCount();
        double confidence = 0.5;

        
        if (totalSignals >= 10) confidence += 0.2;
        else if (totalSignals >= 5) confidence += 0.1;

        
        if (features.getBehavior() != null) {
            if (features.getBehavior().getAverageTypingSpeed() != null) confidence += 0.1;
            if (features.getBehavior().getTypingConsistency() != null) confidence += 0.1;
        }

        return Math.min(confidence, 1.0);
    }

    private int combineScores(int ruleScore, double mlScore, double mlConfidence) {
        if (!mlEnabled) {
            return ruleScore;
        }
        
        if (mlConfidence >= confidenceThreshold && mlEnabled) {
            return (int) (ruleScore * (1 - mlWeight) + mlScore * mlWeight);
        }
        
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
            addFeatureIfNotEmpty(topFeatures, "COPY_PASTE", "BEHAVIOR", sf.getClipboardAttempts() * 2.0,
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
        analysis.put("status", mlResult.getStatus());
        analysis.put("source", mlResult.getSource());
        analysis.put("algorithm", mlResult.getAlgorithm());
        if (!mlEnabled) {
            analysis.put("mode", "RULE_BASED_ONLY");
            analysis.put("message", "External ML risk scoring is disabled; combined score uses rule-based scoring only.");
            analysis.put("detectedPatterns", List.of());
            analysis.put("warnings", List.of());
            return analysis;
        }

        analysis.put("mode", "EXTERNAL_ML".equals(mlResult.getSource()) ? "ML_HYBRID" : "STATISTICAL_FALLBACK");

        
        Map<String, Long> byCategory = signals.stream()
                .collect(Collectors.groupingBy(s ->
                        s.getCategory() != null ? s.getCategory() : "OTHER", Collectors.counting()));

        analysis.put("behavioralAnomalyScore", byCategory.getOrDefault("SCREEN_LEAVE", 0L) * 1.5 +
                byCategory.getOrDefault("CLIPBOARD", 0L) * 2.0);
        analysis.put("identityAnomalyScore", byCategory.getOrDefault("IDENTITY", 0L) * 3.0);
        analysis.put("temporalAnomalyScore", byCategory.getOrDefault("HEARTBEAT", 0L) * 1.0);

        
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

    @lombok.Value
    private static class MLAnalysisResult {
        double mlScore;
        double confidence;
        String riskLevel;
        double fraudProbability;
        String status;
        String source;
        String algorithm;
    }
}
