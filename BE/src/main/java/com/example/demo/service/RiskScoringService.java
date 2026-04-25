package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.AutoPausedBy;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RiskScoreLog;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
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
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${demo.risk.level.suspicious-min:31}")
    private int suspiciousMin;

    @Value("${demo.risk.level.high-risk-min:61}")
    private int highRiskMin;

    @Value("${demo.risk.level.critical-min:81}")
    private int criticalMin;

    @Value("${demo.risk.snapshots.interval-seconds:60}")
    private long snapshotIntervalSeconds;

    @Value("${demo.risk.snapshots.min-delta:5}")
    private int snapshotMinDelta;

    @Value("${demo.risk.weight.tab-switch:0.5}")
    private double tabSwitchWeight;

    @Value("${demo.risk.cap.tab-switch:30}")
    private int tabSwitchCap;

    @Value("${demo.risk.weight.window-blur:0.4}")
    private double blurWeight;

    @Value("${demo.risk.cap.window-blur:25}")
    private int blurCap;

    @Value("${demo.risk.weight.exit-fullscreen:0.75}")
    private double exitFullscreenWeight;

    @Value("${demo.risk.cap.exit-fullscreen:40}")
    private int exitFullscreenCap;

    @Value("${demo.risk.weight.copy-paste:0.8}")
    private double copyPasteWeight;

    @Value("${demo.risk.cap.copy-paste:40}")
    private int copyPasteCap;

    @Value("${demo.risk.weight.idle-time:0.3}")
    private double idleTimeWeight;

    @Value("${demo.risk.cap.idle-time:20}")
    private int idleTimeCap;

    @Value("${demo.risk.weight.devtools-open:0.9}")
    private double devToolsWeight;

    @Value("${demo.risk.cap.devtools-open:60}")
    private int devToolsCap;

    @Value("${demo.risk.weight.right-click:0.35}")
    private double rightClickWeight;

    @Value("${demo.risk.cap.right-click:15}")
    private int rightClickCap;

    @Value("${demo.risk.weight.print-screen:0.85}")
    private double printScreenWeight;

    @Value("${demo.risk.cap.print-screen:45}")
    private int printScreenCap;

    @Value("${demo.risk.weight.rapid-question-switch:0.6}")
    private double rapidQuestionSwitchWeight;

    @Value("${demo.risk.cap.rapid-question-switch:30}")
    private int rapidQuestionSwitchCap;

    @Value("${demo.risk.weight.multi-monitor:0.85}")
    private double multiMonitorWeight;

    @Value("${demo.risk.cap.multi-monitor:45}")
    private int multiMonitorCap;

    @Value("${demo.risk.weight.duplicate-ip:0.7}")
    private double duplicateIpWeight;

    @Value("${demo.risk.cap.duplicate-ip:45}")
    private int duplicateIpCap;

    @Value("${demo.risk.weight.time-anomaly:0.65}")
    private double timeAnomalyWeight;

    @Value("${demo.risk.cap.time-anomaly:40}")
    private int timeAnomalyCap;

    @Value("${demo.risk.weight.heartbeat-stale:0.7}")
    private double heartbeatStaleWeight;

    @Value("${demo.risk.cap.heartbeat-stale:35}")
    private int heartbeatStaleCap;

    @Value("${demo.risk.weight.device-fingerprint-changed:0.95}")
    private double deviceFingerprintWeight;

    @Value("${demo.risk.cap.device-fingerprint-changed:70}")
    private int deviceFingerprintCap;

    @Value("${demo.risk.weight.default:0.5}")
    private double defaultWeight;

    @Value("${demo.risk.cap.default:25}")
    private int defaultCap;

    @Value("${demo.risk.recency.half-life-minutes:15}")
    private long recencyHalfLifeMinutes;

    /** Opt-in auto-resume when risk drops from CRITICAL. Default false (teacher must manually resume). */
    @Value("${demo.risk.auto-resume.enabled:false}")
    private boolean autoResumeEnabled;

    @Transactional
    public RiskScoreResponse recomputeRisk(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (FraudSignal signal : signals) {
            SignalScoreConfig config = signalConfig(signal.getSignalType(), signal.getSeverity());
            int existing = breakdown.getOrDefault(config.key(), 0);
            int contribution = weightedContribution(signal, config, now);
            int next = Math.min(config.cap(), existing + Math.max(contribution, 0));
            breakdown.put(config.key(), next);
        }

        int score = Math.min(100, breakdown.values().stream().mapToInt(Integer::intValue).sum());
        RiskLevel level = resolveLevel(score);
        DecisionSummary decision = buildDecisionSummary(attempt, level, breakdown);
        RiskScoreLog previousLog = riskScoreLogRepository.findTop1ByAttemptOrderByCreatedAtDesc(attempt);
        RiskLevel previousLevel = previousLog != null ? previousLog.getLevel() : RiskLevel.CLEAN;
        RiskActionType actionTaken = applyAutomatedAction(attempt, level, previousLevel);
        RiskActionType resumeAction = applyAutomatedResume(attempt, level, previousLevel);
        if (resumeAction != RiskActionType.NONE) {
            actionTaken = resumeAction;
        }

        attempt.setRiskScore(score);
        attempt.setRiskLevel(level);
        attempt.setSuspicious(level.isSuspicious());
        examAttemptRepository.save(attempt);

        if (shouldPersistSnapshot(previousLog, score, level, actionTaken, now)) {
            riskScoreLogRepository.save(RiskScoreLog.builder()
                    .attempt(attempt)
                    .student(attempt.getStudent())
                    .score(score)
                    .level(level)
                    .breakdown(writeJson(breakdown))
                    .actionTaken(actionTaken)
                    .createdAt(now)
                    .build());
        }

        if (level.isSuspicious() || actionTaken != RiskActionType.NONE) {
            realtimeNotificationService.notifyRiskUpdated(attempt, level, breakdown, actionTaken, decision);
        }

        return toResponse(attempt, score, level, breakdown, actionTaken, decision, now);
    }

    public RiskLevel resolveLevel(int score) {
        if (score >= criticalMin) {
            return RiskLevel.CRITICAL;
        }
        if (score >= highRiskMin) {
            return RiskLevel.HIGH_RISK;
        }
        if (score >= suspiciousMin) {
            return RiskLevel.SUSPICIOUS;
        }
        return RiskLevel.CLEAN;
    }

    public boolean isSuspicious(int riskScore) {
        return resolveLevel(riskScore).isSuspicious();
    }

    /**
     * Apply automated action based on risk level.
     * Only auto-pauses if the attempt is currently IN_PROGRESS.
     * Sets autoPausedBy = SYSTEM so auto-resume can distinguish from manual pause.
     */
    private RiskActionType applyAutomatedAction(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (level == RiskLevel.CRITICAL && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.PAUSED);
            attempt.setAutoPausedBy(AutoPausedBy.SYSTEM);
            attempt.setPausedAt(VietNamTime.now());
            auditLogService.logSystemAttemptPaused(attempt, "Risk score reached critical threshold");
            realtimeNotificationService.notifyAttemptPaused(attempt,
                    "Phiên thi đang được tạm dừng để giám thị kiểm tra.");
            return RiskActionType.ATTEMPT_PAUSED;
        }

        if (level == RiskLevel.HIGH_RISK && previousLevel.ordinal() < RiskLevel.HIGH_RISK.ordinal()) {
            auditLogService.logSystemRiskWarning(attempt, "Risk score reached high-risk threshold");
            realtimeNotificationService.notifySystemWarning(attempt,
                    "Hệ thống phát hiện rủi ro cao. Vui lòng tiếp tục làm bài đúng quy định.");
            return RiskActionType.WARNING_SENT;
        }

        if (level == RiskLevel.SUSPICIOUS && previousLevel == RiskLevel.CLEAN) {
            return RiskActionType.REVIEW_REQUIRED;
        }

        return RiskActionType.NONE;
    }

    /**
     * Auto-resume only when:
     * - Attempt is PAUSED
     * - It was auto-paused by SYSTEM (not manually paused by teacher)
     * - Risk has dropped below CRITICAL
     * - Auto-resume is enabled via config (default: false)
     */
    private RiskActionType applyAutomatedResume(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (!autoResumeEnabled) {
            return RiskActionType.NONE;
        }
        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            return RiskActionType.NONE;
        }
        if (attempt.getAutoPausedBy() != AutoPausedBy.SYSTEM) {
            return RiskActionType.NONE;
        }
        if (previousLevel != RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        if (level == RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setAutoPausedBy(AutoPausedBy.NONE);
        auditLogService.logSystemAttemptResumed(attempt, "Risk score dropped from CRITICAL to " + level.name());
        realtimeNotificationService.notifyAttemptResumed(attempt,
                "Rủi ro đã giảm. Phiên thi đã được khôi phục. Vui lòng tiếp tục làm bài.");
        return RiskActionType.ATTEMPT_RESUMED;
    }

    private boolean shouldPersistSnapshot(
            RiskScoreLog previousLog,
            int score,
            RiskLevel level,
            RiskActionType actionTaken,
            LocalDateTime now
    ) {
        if (previousLog == null) {
            return true;
        }
        if (actionTaken != RiskActionType.NONE) {
            return true;
        }
        if (previousLog.getLevel() != level) {
            return true;
        }
        if (Math.abs(previousLog.getScore() - score) >= snapshotMinDelta) {
            return true;
        }
        return Duration.between(previousLog.getCreatedAt(), now).toSeconds() >= Math.max(snapshotIntervalSeconds, 1);
    }

    private RiskScoreResponse toResponse(
            ExamAttempt attempt,
            int score,
            RiskLevel level,
            Map<String, Integer> breakdown,
            RiskActionType actionTaken,
            DecisionSummary decision,
            LocalDateTime now
    ) {
        List<RiskScoreResponse.LatestSignalItem> latestSignals = fraudSignalRepository
                .findTop20ByAttemptOrderByCreatedAtDesc(attempt)
                .stream()
                .limit(5)
                .map(signal -> RiskScoreResponse.LatestSignalItem.builder()
                        .signalType(signal.getSignalType())
                        .confidence(signal.getConfidence())
                        .severity(signal.getSeverity().name())
                        .evidence(signal.getEvidence())
                        .createdAt(toOffset(signal.getCreatedAt()))
                        .build())
                .toList();
        return RiskScoreResponse.builder()
                .attemptId(attempt.getId())
                .score(score)
                .level(level.name())
                .suspicious(level.isSuspicious())
                .reviewRequired(decision.reviewRequired())
                .recommendedAction(decision.recommendedAction())
                .actionTaken(actionTaken.name())
                .status(attempt.getStatus().name())
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

    private SignalScoreConfig signalConfig(String signalType, SignalSeverity severity) {
        String normalized = signalType == null ? "UNKNOWN_SIGNAL" : signalType.trim().toUpperCase();
        return switch (normalized) {
            case "TAB_SWITCH" -> new SignalScoreConfig("TAB_SWITCH", tabSwitchWeight, tabSwitchCap);
            case "WINDOW_BLUR", "BLUR" -> new SignalScoreConfig("WINDOW_BLUR", blurWeight, blurCap);
            case "EXIT_FULLSCREEN" -> new SignalScoreConfig("EXIT_FULLSCREEN", exitFullscreenWeight, exitFullscreenCap);
            case "FULLSCREEN_EVASION" -> new SignalScoreConfig("FULLSCREEN_EVASION", 0.9, 45);
            case "COPY_PASTE" -> new SignalScoreConfig("COPY_PASTE", copyPasteWeight, copyPasteCap);
            case "CLIPBOARD_BURST" -> new SignalScoreConfig("CLIPBOARD_BURST", 0.95, 50);
            case "IDLE_TIME" -> new SignalScoreConfig("IDLE_TIME", idleTimeWeight, idleTimeCap);
            case "DEVTOOLS_OPEN" -> new SignalScoreConfig("DEVTOOLS_OPEN", devToolsWeight, devToolsCap);
            case "RIGHT_CLICK" -> new SignalScoreConfig("RIGHT_CLICK", rightClickWeight, rightClickCap);
            case "PRINT_SCREEN" -> new SignalScoreConfig("PRINT_SCREEN", printScreenWeight, printScreenCap);
            case "RAPID_QUESTION_SWITCH" -> new SignalScoreConfig("RAPID_QUESTION_SWITCH", rapidQuestionSwitchWeight, rapidQuestionSwitchCap);
            case "QUESTION_TIMING_ANOMALY" -> new SignalScoreConfig("QUESTION_TIMING_ANOMALY", 0.72, 35);
            case "ANSWER_CHANGE_BURST" -> new SignalScoreConfig("ANSWER_CHANGE_BURST", 0.92, 50);
            case "SYNC_BEHAVIOR" -> new SignalScoreConfig("SYNC_BEHAVIOR", 0.9, 55);
            case "MULTI_MONITOR" -> new SignalScoreConfig("MULTI_MONITOR", multiMonitorWeight, multiMonitorCap);
            case "DUPLICATE_IP" -> new SignalScoreConfig("DUPLICATE_IP", duplicateIpWeight, duplicateIpCap);
            case "IP_FINGERPRINT_GRAPH" -> new SignalScoreConfig("IP_FINGERPRINT_GRAPH", 0.96, 60);
            case "VPN_DETECTED" -> new SignalScoreConfig("VPN_DETECTED", 0.95, 70);
            case "PROXY_DETECTED" -> new SignalScoreConfig("PROXY_DETECTED", 0.9, 65);
            case "TOR_EXIT_NODE" -> new SignalScoreConfig("TOR_EXIT_NODE", 1.0, 80);
            case "GEO_ANOMALY" -> new SignalScoreConfig("GEO_ANOMALY", 0.75, 50);
            case "SUBNET_SUSPICION" -> new SignalScoreConfig("SUBNET_SUSPICION", 0.7, 40);
            case "TIME_ANOMALY", "FAST_SUBMIT" -> new SignalScoreConfig("TIME_ANOMALY", timeAnomalyWeight, timeAnomalyCap);
            case "IMPOSSIBLE_SPEED", "IMPOSSIBLE_EXAM_SPEED" -> new SignalScoreConfig("IMPOSSIBLE_SPEED", 0.92, 60);
            case "FAST_ANSWER" -> new SignalScoreConfig("FAST_ANSWER", 0.65, 35);
            case "SUSPICIOUS_BURST" -> new SignalScoreConfig("SUSPICIOUS_BURST", 0.7, 40);
            case "PERFECT_TIMING_PATTERN" -> new SignalScoreConfig("PERFECT_TIMING_PATTERN", 0.85, 50);
            case "PACING_INCONSISTENCY" -> new SignalScoreConfig("PACING_INCONSISTENCY", 0.3, 20);
            case "FAST_OUTLIER", "SLOW_OUTLIER" -> new SignalScoreConfig("TIMING_OUTLIER", 0.5, 25);
            case "HEARTBEAT_STALE" -> new SignalScoreConfig("HEARTBEAT_STALE", heartbeatStaleWeight, heartbeatStaleCap);
            case "NETWORK_INSTABILITY" -> new SignalScoreConfig("NETWORK_INSTABILITY", 0.7, 35);
            case "SESSION_RECOVERY" -> new SignalScoreConfig("SESSION_RECOVERY", 0.78, 35);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalScoreConfig("DEVICE_FINGERPRINT_CHANGED", deviceFingerprintWeight, deviceFingerprintCap);
            case "EXACT_ANSWER_MATCH" -> new SignalScoreConfig("EXACT_ANSWER_MATCH", 1.0, 85);
            case "COLLABORATION_SIGNAL" -> new SignalScoreConfig("COLLABORATION_SIGNAL", 0.9, 70);
            case "ANSWER_TEMPLATE" -> new SignalScoreConfig("ANSWER_TEMPLATE", 0.75, 55);
            case "PERFORMANCE_OUTLIER", "HIGH_PERFORMANCE_OUTLIER", "LOW_PERFORMANCE_OUTLIER" -> new SignalScoreConfig("PERFORMANCE_OUTLIER", 0.6, 40);
            case "SCORE_CLUSTER_ANOMALY", "SCORE_DISTRIBUTION_ANOMALY" -> new SignalScoreConfig("SCORE_CLUSTER_ANOMALY", 0.7, 45);
            case "SUDDEN_IMPROVEMENT", "SUDDEN_DECLINE" -> new SignalScoreConfig("HISTORICAL_ANOMALY", 0.55, 35);
            case "TYPING_PATTERN_MISMATCH" -> new SignalScoreConfig("TYPING_PATTERN_MISMATCH", 0.85, 55);
            case "MOUSE_SIGNATURE_ANOMALY" -> new SignalScoreConfig("MOUSE_SIGNATURE_ANOMALY", 0.7, 40);
            case "AUTOMATED_INPUT_DETECTED", "AUTOMATED_FLIGHT_TIME", "AUTOMATED_DWELL_TIME" -> new SignalScoreConfig("AUTOMATED_INPUT_DETECTED", 0.98, 75);
            case "INTERRUPTION_PATTERN", "EXCESSIVE_INTERRUPTIONS", "INTERRUPTION_BURST" -> new SignalScoreConfig("INTERRUPTION_PATTERN", 0.4, 25);
            case "RHYTHM_ANOMALY" -> new SignalScoreConfig("RHYTHM_ANOMALY", 0.6, 35);
            case "ANSWER_SIMILARITY" -> new SignalScoreConfig("ANSWER_SIMILARITY", 0.98, 70);
            case "AI_MULTIPLE_FACES" -> new SignalScoreConfig("AI_MULTIPLE_FACES", 0.98, 70);
            case "AI_FACE_MISSING" -> new SignalScoreConfig("AI_FACE_MISSING", 0.78, 35);
            case "AI_PHONE_DETECTED" -> new SignalScoreConfig("AI_PHONE_DETECTED", 1.0, 75);
            case "AI_LOOKING_AWAY" -> new SignalScoreConfig("AI_LOOKING_AWAY", 0.74, 30);
            case "AI_SPEAKING_DETECTED" -> new SignalScoreConfig("AI_SPEAKING_DETECTED", 0.7, 30);
            default -> new SignalScoreConfig(normalized, defaultWeight, defaultCap + severity.baseScore());
        };
    }

    private int weightedContribution(FraudSignal signal, SignalScoreConfig config, LocalDateTime now) {
        double recency = recencyMultiplier(signal.getCreatedAt(), now);
        double contribution = signal.getSeverity().baseScore() * signal.getConfidence() * config.weight() * recency;
        return (int) Math.round(contribution);
    }

    private double recencyMultiplier(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) {
            return 1.0;
        }
        long safeHalfLife = Math.max(recencyHalfLifeMinutes, 1);
        long ageMinutes = Math.max(Duration.between(createdAt, now).toMinutes(), 0);
        double decay = Math.pow(0.5d, (double) ageMinutes / safeHalfLife);
        return Math.max(0.35d, Math.min(1.0d, decay));
    }

    private DecisionSummary buildDecisionSummary(
            ExamAttempt attempt,
            RiskLevel level,
            Map<String, Integer> breakdown
    ) {
        List<String> reasons = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        breakdown.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(3)
                .forEach(entry -> {
                    reasons.add(reasonLabel(entry.getKey()));
                    evidence.add(reasonLabel(entry.getKey()) + ": " + entry.getValue() + " diem");
                });

        String recommendedAction = switch (level) {
            case CRITICAL -> "PAUSE_AND_REVIEW";
            case HIGH_RISK -> "WARN_AND_ESCALATE";
            case SUSPICIOUS -> "REVIEW_ATTEMPT";
            case CLEAN -> "CONTINUE_MONITORING";
        };
        boolean reviewRequired = level.isSuspicious() || attempt.getStatus() == AttemptStatus.PAUSED;
        return new DecisionSummary(reviewRequired, recommendedAction, reasons, evidence);
    }

    private String reasonLabel(String key) {
        return switch (key) {
            case "TAB_SWITCH" -> "Chuyen tab bat thuong";
            case "WINDOW_BLUR" -> "Mat tieu diem cua so";
            case "EXIT_FULLSCREEN", "FULLSCREEN_EVASION" -> "Thoat hoac ne tran toan man hinh";
            case "COPY_PASTE", "CLIPBOARD_BURST" -> "Hoat dong clipboard bat thuong";
            case "DEVTOOLS_OPEN" -> "Mo DevTools";
            case "QUESTION_TIMING_ANOMALY" -> "Thoi gian lam cau hoi bat thuong";
            case "ANSWER_CHANGE_BURST" -> "Thay doi dap an don dap";
            case "SYNC_BEHAVIOR" -> "Nhieu thi sinh co hanh vi dong bo";
            case "DUPLICATE_IP", "IP_FINGERPRINT_GRAPH" -> "Lien he IP hoac fingerprint dang ngo";
            case "NETWORK_INSTABILITY", "SESSION_RECOVERY" -> "Ket noi hoac khoi phuc phien bat thuong";
            case "DEVICE_FINGERPRINT_CHANGED" -> "Thiet bi thay doi trong luc thi";
            case "VPN_DETECTED" -> "Phat hien su dung VPN";
            case "PROXY_DETECTED" -> "Phat hien su dung Proxy";
            case "TOR_EXIT_NODE" -> "Phat hien su dung Tor";
            case "GEO_ANOMALY" -> "Vi tri dia ly bat thuong";
            case "SUBNET_SUSPICION" -> "Nhieu thi sinh cung subnet";
            case "IMPOSSIBLE_SPEED", "IMPOSSIBLE_EXAM_SPEED" -> "Toc do lam bai khong the";
            case "FAST_ANSWER" -> "Lam bai qua nhanh";
            case "SUSPICIOUS_BURST" -> "Lam nhieu cau trong thoi gian ngan";
            case "PERFECT_TIMING_PATTERN" -> "Thoi gian lam bai qua chinh xac";
            case "PACING_INCONSISTENCY" -> "Toc do lam bai khong deu";
            case "TIMING_OUTLIER" -> "Thoi gian bat thuong ve mat thong ke";
            case "EXACT_ANSWER_MATCH" -> "Trung lap dap an chinh xac";
            case "COLLABORATION_SIGNAL" -> "Dau hieu hop tac gian lan";
            case "ANSWER_TEMPLATE" -> "Chung mot bieu mau dap an";
            case "PERFORMANCE_OUTLIER" -> "Diem bat thuong so voi lop";
            case "SCORE_CLUSTER_ANOMALY" -> "Phan bo diem tap trung bat thuong";
            case "HISTORICAL_ANOMALY" -> "Thay doi diem bat thuong so voi lich su";
            case "TYPING_PATTERN_MISMATCH" -> "Mau go ban phim khac voi ho so";
            case "MOUSE_SIGNATURE_ANOMALY" -> "Huong dan chuot bat thuong";
            case "AUTOMATED_INPUT_DETECTED" -> "Phat hien input tu dong hoa";
            case "INTERRUPTION_PATTERN" -> "Mau ngat quang bat thuong";
            case "RHYTHM_ANOMALY" -> "Chu ky go bat thuong";
            case "ANSWER_SIMILARITY" -> "Do tuong dong dap an cao";
            case "AI_MULTIPLE_FACES" -> "AI phat hien nhieu khuon mat";
            case "AI_FACE_MISSING" -> "AI phat hien mat mat";
            case "AI_PHONE_DETECTED" -> "AI phat hien dien thoai";
            case "AI_LOOKING_AWAY" -> "AI phat hien nhin lech huong";
            case "AI_SPEAKING_DETECTED" -> "AI phat hien noi chuyen";
            case "TIME_ANOMALY" -> "Thoi gian nop bat thuong";
            case "HEARTBEAT_STALE" -> "Heartbeat gian doan";
            default -> key;
        };
    }

    private String writeJson(Map<String, Integer> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize risk breakdown", ex);
        }
    }

    private record SignalScoreConfig(String key, double weight, int cap) {
    }

    public record DecisionSummary(
            boolean reviewRequired,
            String recommendedAction,
            List<String> reasons,
            List<String> evidenceSummary
    ) {
    }
}
