package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.ProctorFlagStatus;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RiskScoreLog;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.ProctorFlagRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase-1 Risk Scoring Service.
 *
 * Simple, transparent scoring based only on signals received from the frontend.
 * No anomaly detection, no recency decay, no auto-generated signals.
 *
 * Category scoring with dedup windows:
 * - SCREEN_LEAVE (TAB_SWITCH=10, WINDOW_BLUR=8, EXIT_FULLSCREEN=15, LONG_SCREEN_LEAVE=25): max 35 pts, 60s window
 * - CLIPBOARD (COPY=8, CUT=8, PASTE=10, LONG_PASTE=22): max 25 pts, 30s window
 * - TECHNICAL (RIGHT_CLICK=5, INSPECT=12, PRINT_SCREEN=15, DEVTOOLS=22): max 25 pts, 120s window
 * - IDENTITY (DEVICE_CHANGE=20, DUPLICATE_IP=25, IP_GRAPH=30): max 30 pts, session-level
 * - HEARTBEAT (STALE=5, NETWORK=5, RECOVERY=3): max 10 pts, session-level
 *
 * Risk levels: 0-19=CLEAN, 20-49=SUSPICIOUS, 50-74=HIGH_RISK, 75-100=CRITICAL
 *
 * Flags: <50=no flag, 50-74=OPEN HIGH_RISK, >=75=OPEN CRITICAL
 * No auto-pause on CRITICAL in Phase-1.
 */
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(VN).toOffsetDateTime();
    }

    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoreLogRepository riskScoreLogRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ProctorFlagRepository proctorFlagRepository;
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${demo.risk.level.suspicious-min:20}")
    private int suspiciousMin;

    @Value("${demo.risk.level.high-risk-min:50}")
    private int highRiskMin;

    @Value("${demo.risk.level.critical-min:75}")
    private int criticalMin;

    @Value("${demo.risk.dedup.screen-leave-seconds:60}")
    private long screenLeaveDedupSeconds;

    @Value("${demo.risk.dedup.clipboard-seconds:30}")
    private long clipboardDedupSeconds;

    @Value("${demo.risk.dedup.technical-seconds:120}")
    private long technicalDedupSeconds;

    @Value("${demo.risk.snapshots.interval-seconds:60}")
    private long snapshotIntervalSeconds;

    // Fixed point scores per signal type (no weight/confidence math)
    private static final Map<String, Integer> SIGNAL_SCORES = Map.ofEntries(
            // SCREEN_LEAVE category
            Map.entry("TAB_SWITCH", 10),
            Map.entry("WINDOW_BLUR", 8),
            Map.entry("EXIT_FULLSCREEN", 15),
            Map.entry("FULLSCREEN_VIOLATION", 15),
            Map.entry("LONG_SCREEN_LEAVE", 25),
            // CLIPBOARD category
            Map.entry("COPY_ATTEMPT", 8),
            Map.entry("CUT_ATTEMPT", 8),
            Map.entry("PASTE_ATTEMPT", 10),
            Map.entry("LONG_PASTE", 22),
            Map.entry("CLIPBOARD_ABUSE", 10),
            Map.entry("CLIPBOARD_BURST", 10),
            // TECHNICAL category
            Map.entry("RIGHT_CLICK", 5),
            Map.entry("INSPECT_ATTEMPT", 12),
            Map.entry("PRINT_SCREEN", 15),
            Map.entry("DEVTOOLS_OPEN", 22),
            Map.entry("DEVTOOLS_DETECTED", 22),
            // IDENTITY category
            Map.entry("DEVICE_FINGERPRINT_CHANGED", 20),
            Map.entry("DUPLICATE_IP", 25),
            Map.entry("IP_FINGERPRINT_GRAPH", 30),
            Map.entry("IP_ANOMALY", 25),
            // HEARTBEAT category
            Map.entry("HEARTBEAT_STALE", 5),
            Map.entry("NETWORK_INSTABILITY", 5),
            Map.entry("SESSION_RECOVERY", 3)
    );

    // Category grouping
    private static final Map<String, String> SIGNAL_CATEGORY = Map.ofEntries(
            Map.entry("TAB_SWITCH", "SCREEN_LEAVE"),
            Map.entry("WINDOW_BLUR", "SCREEN_LEAVE"),
            Map.entry("EXIT_FULLSCREEN", "SCREEN_LEAVE"),
            Map.entry("FULLSCREEN_VIOLATION", "SCREEN_LEAVE"),
            Map.entry("LONG_SCREEN_LEAVE", "SCREEN_LEAVE"),
            Map.entry("COPY_ATTEMPT", "CLIPBOARD"),
            Map.entry("CUT_ATTEMPT", "CLIPBOARD"),
            Map.entry("PASTE_ATTEMPT", "CLIPBOARD"),
            Map.entry("LONG_PASTE", "CLIPBOARD"),
            Map.entry("CLIPBOARD_ABUSE", "CLIPBOARD"),
            Map.entry("CLIPBOARD_BURST", "CLIPBOARD"),
            Map.entry("RIGHT_CLICK", "TECHNICAL"),
            Map.entry("INSPECT_ATTEMPT", "TECHNICAL"),
            Map.entry("PRINT_SCREEN", "TECHNICAL"),
            Map.entry("DEVTOOLS_OPEN", "TECHNICAL"),
            Map.entry("DEVTOOLS_DETECTED", "TECHNICAL"),
            Map.entry("DEVICE_FINGERPRINT_CHANGED", "IDENTITY"),
            Map.entry("DUPLICATE_IP", "IDENTITY"),
            Map.entry("IP_FINGERPRINT_GRAPH", "IDENTITY"),
            Map.entry("IP_ANOMALY", "IDENTITY"),
            Map.entry("HEARTBEAT_STALE", "HEARTBEAT"),
            Map.entry("NETWORK_INSTABILITY", "HEARTBEAT"),
            Map.entry("SESSION_RECOVERY", "HEARTBEAT")
    );

    // Dedup windows per category (seconds)
    private static final Map<String, Long> CATEGORY_DEDUP_SECONDS = Map.of(
            "SCREEN_LEAVE", 60L,
            "CLIPBOARD", 30L,
            "TECHNICAL", 120L,
            "IDENTITY", 0L,    // session-level, no dedup window
            "HEARTBEAT", 0L     // session-level, no dedup window
    );

    // Category caps
    private static final Map<String, Integer> CATEGORY_CAPS = Map.of(
            "SCREEN_LEAVE", 35,
            "CLIPBOARD", 25,
            "TECHNICAL", 25,
            "IDENTITY", 30,
            "HEARTBEAT", 10
    );

    @Transactional
    public RiskScoreResponse recomputeRisk(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        // Step 1: Apply dedup windows per category — take the highest score in each window
        Map<String, Integer> categoryScores = computeCategoryScores(signals, now);

        // Step 2: Cap each category
        int screenLeaveScore = Math.min(categoryScores.getOrDefault("SCREEN_LEAVE", 0), CATEGORY_CAPS.getOrDefault("SCREEN_LEAVE", 35));
        int clipboardScore = Math.min(categoryScores.getOrDefault("CLIPBOARD", 0), CATEGORY_CAPS.getOrDefault("CLIPBOARD", 25));
        int technicalScore = Math.min(categoryScores.getOrDefault("TECHNICAL", 0), CATEGORY_CAPS.getOrDefault("TECHNICAL", 25));
        int identityScore = Math.min(categoryScores.getOrDefault("IDENTITY", 0), CATEGORY_CAPS.getOrDefault("IDENTITY", 30));
        int heartbeatScore = Math.min(categoryScores.getOrDefault("HEARTBEAT", 0), CATEGORY_CAPS.getOrDefault("HEARTBEAT", 10));

        // Step 3: Compute totals
        int behaviorScore = Math.min(70, screenLeaveScore + clipboardScore + technicalScore + heartbeatScore);
        int totalRisk = Math.min(100, behaviorScore + identityScore);
        RiskLevel level = resolveLevel(totalRisk);

        // Step 4: Build breakdown
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("screenLeaveScore", screenLeaveScore);
        breakdown.put("clipboardScore", clipboardScore);
        breakdown.put("technicalScore", technicalScore);
        breakdown.put("identityScore", identityScore);
        breakdown.put("heartbeatScore", heartbeatScore);
        breakdown.put("behaviorScore", behaviorScore);
        breakdown.put("totalScore", totalRisk);

        // Step 5: Update attempt
        attempt.setRiskScore(totalRisk);
        attempt.setRiskLevel(level);
        attempt.setSuspicious(level.isSuspicious());
        examAttemptRepository.save(attempt);

        // Step 6: Sync flag
        ProctorFlag flag = syncRiskFlag(attempt, totalRisk, level, now);

        // Step 7: Persist snapshot
        RiskScoreLog previousLog = riskScoreLogRepository.findTop1ByAttemptOrderByCreatedAtDesc(attempt);
        if (shouldPersistSnapshot(previousLog, totalRisk, level, now)) {
            riskScoreLogRepository.save(RiskScoreLog.builder()
                    .attempt(attempt)
                    .student(attempt.getStudent())
                    .score(totalRisk)
                    .level(level)
                    .breakdown(writeJson(breakdown))
                    .actionTaken(RiskActionType.NONE)
                    .createdAt(now)
                    .build());
        }

        // Step 8: Send realtime notification if suspicious+
        if (level.isSuspicious()) {
            realtimeNotificationService.notifyRiskUpdated(attempt, level, breakdown,
                    RiskActionType.NONE, buildDecisionSummary(level, breakdown));
        }

        return toResponse(attempt, totalRisk, level, breakdown, signals, flag, now);
    }

    /**
     * Recompute risk WITHOUT triggering automated actions (no flag sync).
     * Used by read endpoints to avoid side effects.
     */
    @Transactional
    public RiskScoreResponse recomputeRiskSkipAutoActions(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        Map<String, Integer> categoryScores = computeCategoryScores(signals, now);
        int screenLeaveScore = Math.min(categoryScores.getOrDefault("SCREEN_LEAVE", 0), CATEGORY_CAPS.getOrDefault("SCREEN_LEAVE", 35));
        int clipboardScore = Math.min(categoryScores.getOrDefault("CLIPBOARD", 0), CATEGORY_CAPS.getOrDefault("CLIPBOARD", 25));
        int technicalScore = Math.min(categoryScores.getOrDefault("TECHNICAL", 0), CATEGORY_CAPS.getOrDefault("TECHNICAL", 25));
        int identityScore = Math.min(categoryScores.getOrDefault("IDENTITY", 0), CATEGORY_CAPS.getOrDefault("IDENTITY", 30));
        int heartbeatScore = Math.min(categoryScores.getOrDefault("HEARTBEAT", 0), CATEGORY_CAPS.getOrDefault("HEARTBEAT", 10));

        int behaviorScore = Math.min(70, screenLeaveScore + clipboardScore + technicalScore + heartbeatScore);
        int totalRisk = Math.min(100, behaviorScore + identityScore);
        RiskLevel level = resolveLevel(totalRisk);

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("screenLeaveScore", screenLeaveScore);
        breakdown.put("clipboardScore", clipboardScore);
        breakdown.put("technicalScore", technicalScore);
        breakdown.put("identityScore", identityScore);
        breakdown.put("heartbeatScore", heartbeatScore);
        breakdown.put("behaviorScore", behaviorScore);
        breakdown.put("totalScore", totalRisk);

        return toResponse(attempt, totalRisk, level, breakdown, signals, null, now);
    }

    /**
     * Compute the highest signal score per category, deduped by time window.
     * Signals are sorted by creation time; within each dedup window, only the highest-scoring signal counts.
     */
    private Map<String, Integer> computeCategoryScores(List<FraudSignal> signals, LocalDateTime now) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("SCREEN_LEAVE", 0);
        result.put("CLIPBOARD", 0);
        result.put("TECHNICAL", 0);
        result.put("IDENTITY", 0);
        result.put("HEARTBEAT", 0);

        // For categories with dedup windows: track last counted time
        Map<String, LocalDateTime> lastCountedAt = new LinkedHashMap<>();
        lastCountedAt.put("SCREEN_LEAVE", LocalDateTime.MIN);
        lastCountedAt.put("CLIPBOARD", LocalDateTime.MIN);
        lastCountedAt.put("TECHNICAL", LocalDateTime.MIN);
        lastCountedAt.put("IDENTITY", LocalDateTime.MAX);
        lastCountedAt.put("HEARTBEAT", LocalDateTime.MAX);

        for (FraudSignal signal : signals) {
            String signalType = normalizeSignalType(signal.getSignalType());
            String category = SIGNAL_CATEGORY.getOrDefault(signalType, "OTHER");
            if (category.equals("OTHER")) continue;

            Integer score = SIGNAL_SCORES.get(signalType);
            if (score == null) continue;

            Long dedupWindow = CATEGORY_DEDUP_SECONDS.getOrDefault(category, 0L);
            LocalDateTime signalTime = signal.getCreatedAt();
            if (signalTime == null) signalTime = now;

            if (dedupWindow > 0) {
                // Time-window dedup: only count if outside the dedup window from last counted
                LocalDateTime windowStart = lastCountedAt.get(category).plusSeconds(dedupWindow);
                if (!signalTime.isAfter(windowStart)) {
                    continue; // within dedup window, skip
                }
            }
            // Dedup passed or no dedup window (identity/heartbeat = session level)
            int current = result.getOrDefault(category, 0);
            int next = Math.max(current, score);
            result.put(category, next);
            lastCountedAt.put(category, signalTime);
        }

        return result;
    }

    private String normalizeSignalType(String signalType) {
        if (signalType == null) return "";
        String normalized = signalType.trim().toUpperCase();
        // Aliases
        return switch (normalized) {
            case "BLUR" -> "WINDOW_BLUR";
            case "COPY_PASTE" -> "CLIPBOARD_ABUSE";
            case "INSPECT_ATTEMPT" -> "INSPECT_ATTEMPT";
            default -> normalized;
        };
    }

    /**
     * Sync flag based on risk level.
     * < 50: no flag
     * 50-74: OPEN HIGH_RISK flag
     * >= 75: OPEN CRITICAL flag
     */
    @Transactional
    public ProctorFlag syncRiskFlag(ExamAttempt attempt, int score, RiskLevel level, LocalDateTime now) {
        if (score < highRiskMin || level == RiskLevel.CLEAN) {
            return null;
        }

        ProctorFlagStatus targetStatus = level == RiskLevel.CRITICAL
                ? ProctorFlagStatus.OPEN
                : ProctorFlagStatus.OPEN;
        String flagType = level == RiskLevel.CRITICAL ? "CRITICAL_RISK" : "HIGH_RISK";

        // Update existing OPEN flag or create new
        ProctorFlag flag = proctorFlagRepository
                .findFirstByAttemptAndStatusOrderByCreatedAtDesc(attempt, ProctorFlagStatus.OPEN)
                .orElseGet(() -> ProctorFlag.builder()
                        .attempt(attempt)
                        .exam(attempt.getExam())
                        .status(ProctorFlagStatus.OPEN)
                        .createdAt(now != null ? now : VietNamTime.now())
                        .build());

        flag.setFlagType(flagType);
        flag.setRiskScore(score);
        flag.setRiskLevel(level);
        flag.setTitle(level == RiskLevel.CRITICAL
                ? "Can review gian lan nghiem trong"
                : "Can review nguy co gian lan");
        flag.setDescription(buildDescription(score, level));
        flag.setStatus(ProctorFlagStatus.OPEN);

        return proctorFlagRepository.save(flag);
    }

    private String buildDescription(int score, RiskLevel level) {
        return String.format("Diem rui ro: %d — Muc do: %s. Can giao vien review them.", score, level.name());
    }

    public RiskLevel resolveLevel(int score) {
        if (score >= criticalMin) return RiskLevel.CRITICAL;
        if (score >= highRiskMin) return RiskLevel.HIGH_RISK;
        if (score >= suspiciousMin) return RiskLevel.SUSPICIOUS;
        return RiskLevel.CLEAN;
    }

    public boolean isSuspicious(int riskScore) {
        return resolveLevel(riskScore).isSuspicious();
    }

    private boolean shouldPersistSnapshot(RiskScoreLog previousLog, int score, RiskLevel level, LocalDateTime now) {
        if (previousLog == null) return true;
        if (previousLog.getLevel() != level) return true;
        if (Math.abs(previousLog.getScore() - score) >= 5) return true;
        return Duration.between(previousLog.getCreatedAt(), now).toSeconds() >= Math.max(snapshotIntervalSeconds, 1);
    }

    private RiskScoreResponse toResponse(
            ExamAttempt attempt,
            int score,
            RiskLevel level,
            Map<String, Integer> breakdown,
            List<FraudSignal> signals,
            ProctorFlag flag,
            LocalDateTime now
    ) {
        List<RiskScoreResponse.LatestSignalItem> latestSignals = fraudSignalRepository
                .findTop20ByAttemptOrderByCreatedAtDesc(attempt)
                .stream()
                .limit(5)
                .map(signal -> RiskScoreResponse.LatestSignalItem.builder()
                        .signalType(signal.getSignalType())
                        .confidence(signal.getConfidence())
                        .severity(signal.getSeverity() != null ? signal.getSeverity().name() : "LOW")
                        .evidence(signal.getEvidence())
                        .createdAt(toOffset(signal.getCreatedAt()))
                        .build())
                .toList();

        DecisionSummary decision = buildDecisionSummary(level, breakdown);

        return RiskScoreResponse.builder()
                .attemptId(attempt.getId())
                .score(score)
                .level(level.name())
                .suspicious(level.isSuspicious())
                .reviewRequired(level.isSuspicious())
                .recommendedAction(decision.recommendedAction())
                .actionTaken(RiskActionType.NONE.name())
                .status(attempt.getStatus().name())
                .autoPausedBy("NONE")
                .breakdown(breakdown)
                .reasons(decision.reasons())
                .evidenceSummary(decision.evidenceSummary())
                .latestSignals(latestSignals)
                .attempt(RiskScoreResponse.AttemptSnapshot.builder()
                        .id(attempt.getId())
                        .examId(attempt.getExam().getId())
                        .examTitle(attempt.getExam().getTitle())
                        .status(attempt.getStatus().name())
                        .riskScore(score)
                        .riskLevel(level.name())
                        .suspicious(level.isSuspicious())
                        .startedAt(toOffset(attempt.getStartedAt()))
                        .submittedAt(toOffset(attempt.getSubmittedAt()))
                        .build())
                .student(RiskScoreResponse.StudentSnapshot.builder()
                        .id(attempt.getStudent().getId())
                        .username(attempt.getStudent().getUsername())
                        .name(attempt.getStudent().getFullName())
                        .email(attempt.getStudent().getEmail())
                        .studentCode(attempt.getStudent().getStudentCode())
                        .build())
                .updatedAt(toOffset(now))
                .build();
    }

    private DecisionSummary buildDecisionSummary(RiskLevel level, Map<String, Integer> breakdown) {
        List<String> reasons = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        // Top contributing categories
        if (breakdown.getOrDefault("screenLeaveScore", 0) > 0) {
            reasons.add("Han hanh vi roi man hinh");
            evidence.add("SCREEN_LEAVE: " + breakdown.get("screenLeaveScore") + " diem");
        }
        if (breakdown.getOrDefault("clipboardScore", 0) > 0) {
            reasons.add("Han clipboard bat thuong");
            evidence.add("CLIPBOARD: " + breakdown.get("clipboardScore") + " diem");
        }
        if (breakdown.getOrDefault("technicalScore", 0) > 0) {
            reasons.add("Ky thuat canh cao");
            evidence.add("TECHNICAL: " + breakdown.get("technicalScore") + " diem");
        }
        if (breakdown.getOrDefault("identityScore", 0) > 0) {
            reasons.add("Phat hien bat thuong ve dinh danh");
            evidence.add("IDENTITY: " + breakdown.get("identityScore") + " diem");
        }

        String recommendedAction = switch (level) {
            case CRITICAL -> "FLAG_AND_REVIEW";
            case HIGH_RISK -> "FLAG_FOR_REVIEW";
            case SUSPICIOUS -> "REVIEW_ATTEMPT";
            case CLEAN -> "CONTINUE_MONITORING";
        };

        boolean reviewRequired = level.isSuspicious();
        return new DecisionSummary(reviewRequired, recommendedAction, reasons, evidence);
    }

    private String writeJson(Map<String, Integer> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize risk breakdown", ex);
        }
    }

    public record DecisionSummary(
            boolean reviewRequired,
            String recommendedAction,
            List<String> reasons,
            List<String> evidenceSummary
    ) {}
}
