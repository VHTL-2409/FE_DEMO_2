package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
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

    private RiskActionType applyAutomatedAction(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (level == RiskLevel.CRITICAL && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.PAUSED);
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
     * Tự động khôi phục attempt từ PAUSED → IN_PROGRESS khi risk giảm từ CRITICAL.
     * Chỉ resume nếu attempt đang ở trạng thái PAUSED và risk level đã giảm khỏi CRITICAL.
     */
    private RiskActionType applyAutomatedResume(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            return RiskActionType.NONE;
        }
        if (previousLevel != RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        if (level == RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        examAttemptRepository.save(attempt);
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
            case "TIME_ANOMALY", "FAST_SUBMIT" -> new SignalScoreConfig("TIME_ANOMALY", timeAnomalyWeight, timeAnomalyCap);
            case "HEARTBEAT_STALE" -> new SignalScoreConfig("HEARTBEAT_STALE", heartbeatStaleWeight, heartbeatStaleCap);
            case "NETWORK_INSTABILITY" -> new SignalScoreConfig("NETWORK_INSTABILITY", 0.7, 35);
            case "SESSION_RECOVERY" -> new SignalScoreConfig("SESSION_RECOVERY", 0.78, 35);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalScoreConfig("DEVICE_FINGERPRINT_CHANGED", deviceFingerprintWeight, deviceFingerprintCap);
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
                    evidence.add(reasonLabel(entry.getKey()) + ": " + entry.getValue() + " điểm");
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
            case "TAB_SWITCH" -> "Chuyển tab bất thường";
            case "WINDOW_BLUR" -> "Mất tiêu điểm cửa sổ";
            case "EXIT_FULLSCREEN", "FULLSCREEN_EVASION" -> "Thoát hoặc né tránh toàn màn hình";
            case "COPY_PASTE", "CLIPBOARD_BURST" -> "Hoạt động clipboard bất thường";
            case "DEVTOOLS_OPEN" -> "Mở DevTools";
            case "QUESTION_TIMING_ANOMALY" -> "Thời gian làm câu hỏi bất thường";
            case "ANSWER_CHANGE_BURST" -> "Thay đổi đáp án dồn dập";
            case "SYNC_BEHAVIOR" -> "Nhiều thí sinh có hành vi đồng bộ";
            case "DUPLICATE_IP", "IP_FINGERPRINT_GRAPH" -> "Liên hệ IP hoặc fingerprint đáng ngờ";
            case "NETWORK_INSTABILITY", "SESSION_RECOVERY" -> "Kết nối hoặc khôi phục phiên bất thường";
            case "DEVICE_FINGERPRINT_CHANGED" -> "Thiết bị thay đổi trong lúc thi";
            case "ANSWER_SIMILARITY" -> "Độ tương đồng đáp án cao";
            case "AI_MULTIPLE_FACES" -> "AI phát hiện nhiều khuôn mặt";
            case "AI_FACE_MISSING" -> "AI phát hiện mất mặt";
            case "AI_PHONE_DETECTED" -> "AI phát hiện điện thoại";
            case "AI_LOOKING_AWAY" -> "AI phát hiện nhìn lệch hướng";
            case "AI_SPEAKING_DETECTED" -> "AI phát hiện nói chuyện";
            case "TIME_ANOMALY" -> "Thời gian nộp bất thường";
            case "HEARTBEAT_STALE" -> "Heartbeat gián đoạn";
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
