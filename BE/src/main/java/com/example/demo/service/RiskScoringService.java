package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.config.RiskSignalScoreProperties;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.ProctorFlagStatus;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RiskScoreLog;
import com.example.demo.realtime.TeacherAlertGateway;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Risk Scoring Service - Giai đoạn 1.
 *
 * Chấm điểm đơn giản, minh bạch dựa trên các signals nhận được từ frontend.
 * Không có phát hiện bất thường, không có recency decay, không có signals tự động sinh.
 *
 * Chấm điểm theo category với cửa sổ dedup:
 * - SCREEN_LEAVE (TAB_SWITCH=10, WINDOW_BLUR=8, EXIT_FULLSCREEN=15, LONG_SCREEN_LEAVE=25): tối đa 35 điểm, cửa sổ 60s
 * - CLIPBOARD (COPY_PASTE=10): tối đa 25 điểm, cửa sổ 30s
 * - TECHNICAL (RIGHT_CLICK=5, INSPECT=12, PRINT_SCREEN=15, DEVTOOLS=22): tối đa 25 điểm, cửa sổ 120s
 * - IDENTITY (DEVICE_CHANGE=20, DUPLICATE_IP=25, IP_GRAPH=30): tối đa 30 điểm, mức session
 * - HEARTBEAT (STALE=5, NETWORK=5, RECOVERY=3): tối đa 10 điểm, mức session
 *
 * Các mức rủi ro: 0-19=CLEAN, 20-49=SUSPICIOUS, 50-74=HIGH_RISK, 75-100=CRITICAL
 *
 * Flags: <50=không flag, 50-74=OPEN HIGH_RISK, >=75=OPEN CRITICAL
 * Không tự động pause trên CRITICAL trong Giai đoạn 1.
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
    private final RiskSignalScoreProperties riskSignalScoreProperties;

    @Value("${demo.risk.level.suspicious-min:20}")
    private int suspiciousMin;

    @Value("${demo.risk.level.high-risk-min:50}")
    private int highRiskMin;

    @Value("${demo.risk.level.critical-min:75}")
    private int criticalMin;

    @Value("${demo.risk.dedup.screen-leave-seconds:60}")
    private long screenLeaveDedupSeconds = 60L;

    @Value("${demo.risk.dedup.clipboard-seconds:30}")
    private long clipboardDedupSeconds = 30L;

    @Value("${demo.risk.dedup.technical-seconds:120}")
    private long technicalDedupSeconds = 120L;

    @Value("${demo.risk.snapshots.interval-seconds:60}")
    private long snapshotIntervalSeconds = 60L;

    @Value("${demo.risk.dedup.visual-identity-seconds:30}")
    private long visualIdentityDedupSeconds = 30L;

    @Value("${demo.risk.cap.screen-leave:35}")
    private int screenLeaveCap = 35;

    @Value("${demo.risk.cap.clipboard:25}")
    private int clipboardCap = 25;

    @Value("${demo.risk.cap.technical:25}")
    private int technicalCap = 25;

    @Value("${demo.risk.cap.identity:30}")
    private int identityCap = 30;

    @Value("${demo.risk.cap.heartbeat:10}")
    private int heartbeatCap = 10;

    @Value("${demo.risk.cap.visual-identity:40}")
    private int visualIdentityCap = 40;

    // Điểm cố định cho mỗi loại signal (không có phép tính weight/confidence)
    private static final Map<String, Integer> SIGNAL_SCORES = Map.ofEntries(
            // Category SCREEN_LEAVE
            Map.entry("TAB_SWITCH", 10),
            Map.entry("WINDOW_BLUR", 8),
            Map.entry("EXIT_FULLSCREEN", 15),
            Map.entry("LONG_SCREEN_LEAVE", 25),
            // Category CLIPBOARD
            Map.entry("COPY_PASTE", 10),
            // Category TECHNICAL
            Map.entry("RIGHT_CLICK", 5),
            Map.entry("PRINT_SCREEN", 15),
            Map.entry("DEVTOOLS_OPEN", 22),
            // Category IDENTITY
            Map.entry("DEVICE_FINGERPRINT_CHANGED", 20),
            Map.entry("MULTIPLE_DEVICE_SESSION", 25),
            Map.entry("IP_CHANGED", 15),
            Map.entry("DUPLICATE_IP", 25),
            Map.entry("IP_FINGERPRINT_GRAPH", 30),
            // Category HEARTBEAT
            Map.entry("HEARTBEAT_STALE", 5),
            Map.entry("NETWORK_INSTABILITY", 5),
            Map.entry("SESSION_RECOVERY", 3),
            // Nhóm VISUAL_IDENTITY
            Map.entry("FACE_NOT_DETECTED", 20),
            Map.entry("NO_CAMERA", 20),
            Map.entry("NO_MIC", 20),
            Map.entry("MULTIPLE_FACES", 25),
            Map.entry("FACE_SPOOFING_SUSPECTED", 25),
            Map.entry("FACE_OBSTRUCTED_MASK", 0),
            Map.entry("EYES_OBSTRUCTED", 10),
            Map.entry("PARTIAL_FACE_VISIBLE", 10),
            Map.entry("FACE_TOO_FAR", 8),
            Map.entry("FACE_TOO_CLOSE", 5),
            Map.entry("FACE_TURNED_AWAY", 10),
            Map.entry("FACE_NOT_CENTERED", 5),
            Map.entry("EYES_NOT_DETECTED", 8),
            Map.entry("VERY_LOW_LIGHTING", 15),
            Map.entry("LOW_LIGHTING", 8),
            Map.entry("OVEREXPOSED_FRAME", 5),
            Map.entry("VERY_BLURRY_FRAME", 15),
            Map.entry("BLURRY_FRAME", 5),
            Map.entry("EYE_BLINK_ANOMALY", 10),
            Map.entry("EYES_CLOSED_PROLONGED", 5),
            Map.entry("GAZE_OFF_SCREEN", 12),
            Map.entry("RAPID_EYE_MOVEMENT", 8),
            Map.entry("AI_PHONE_DETECTED", 15),
            Map.entry("AI_SPEAKING_DETECTED", 10),
            Map.entry("PRINTED_PHOTO", 25),
            Map.entry("SCREEN_REPLAY", 25),
            Map.entry("DEEPFAKE", 30),
            Map.entry("FLAT_IMAGE", 20),
            Map.entry("SCREEN_DISPLAY", 18)
    );

    // Nhóm category
    private static final Map<String, String> SIGNAL_CATEGORY = Map.ofEntries(
            Map.entry("TAB_SWITCH", "SCREEN_LEAVE"),
            Map.entry("WINDOW_BLUR", "SCREEN_LEAVE"),
            Map.entry("EXIT_FULLSCREEN", "SCREEN_LEAVE"),
            Map.entry("LONG_SCREEN_LEAVE", "SCREEN_LEAVE"),
            Map.entry("COPY_PASTE", "CLIPBOARD"),
            Map.entry("RIGHT_CLICK", "TECHNICAL"),
            Map.entry("PRINT_SCREEN", "TECHNICAL"),
            Map.entry("DEVTOOLS_OPEN", "TECHNICAL"),
            Map.entry("DEVICE_FINGERPRINT_CHANGED", "IDENTITY"),
            Map.entry("MULTIPLE_DEVICE_SESSION", "IDENTITY"),
            Map.entry("IP_CHANGED", "IDENTITY"),
            Map.entry("DUPLICATE_IP", "IDENTITY"),
            Map.entry("IP_FINGERPRINT_GRAPH", "IDENTITY"),
            Map.entry("HEARTBEAT_STALE", "HEARTBEAT"),
            Map.entry("NETWORK_INSTABILITY", "HEARTBEAT"),
            Map.entry("SESSION_RECOVERY", "HEARTBEAT"),
            Map.entry("FACE_NOT_DETECTED", "VISUAL_IDENTITY"),
            Map.entry("NO_CAMERA", "VISUAL_IDENTITY"),
            Map.entry("NO_MIC", "VISUAL_IDENTITY"),
            Map.entry("MULTIPLE_FACES", "VISUAL_IDENTITY"),
            Map.entry("FACE_SPOOFING_SUSPECTED", "VISUAL_IDENTITY"),
            Map.entry("FACE_OBSTRUCTED_MASK", "VISUAL_IDENTITY"),
            Map.entry("EYES_OBSTRUCTED", "VISUAL_IDENTITY"),
            Map.entry("PARTIAL_FACE_VISIBLE", "VISUAL_IDENTITY"),
            Map.entry("FACE_TOO_FAR", "VISUAL_IDENTITY"),
            Map.entry("FACE_TOO_CLOSE", "VISUAL_IDENTITY"),
            Map.entry("FACE_TURNED_AWAY", "VISUAL_IDENTITY"),
            Map.entry("FACE_NOT_CENTERED", "VISUAL_IDENTITY"),
            Map.entry("EYES_NOT_DETECTED", "VISUAL_IDENTITY"),
            Map.entry("VERY_LOW_LIGHTING", "VISUAL_IDENTITY"),
            Map.entry("LOW_LIGHTING", "VISUAL_IDENTITY"),
            Map.entry("OVEREXPOSED_FRAME", "VISUAL_IDENTITY"),
            Map.entry("VERY_BLURRY_FRAME", "VISUAL_IDENTITY"),
            Map.entry("BLURRY_FRAME", "VISUAL_IDENTITY"),
            Map.entry("EYE_BLINK_ANOMALY", "VISUAL_IDENTITY"),
            Map.entry("EYES_CLOSED_PROLONGED", "VISUAL_IDENTITY"),
            Map.entry("GAZE_OFF_SCREEN", "VISUAL_IDENTITY"),
            Map.entry("RAPID_EYE_MOVEMENT", "VISUAL_IDENTITY"),
            Map.entry("AI_PHONE_DETECTED", "VISUAL_IDENTITY"),
            Map.entry("AI_SPEAKING_DETECTED", "VISUAL_IDENTITY"),
            Map.entry("PRINTED_PHOTO", "VISUAL_IDENTITY"),
            Map.entry("SCREEN_REPLAY", "VISUAL_IDENTITY"),
            Map.entry("DEEPFAKE", "VISUAL_IDENTITY"),
            Map.entry("FLAT_IMAGE", "VISUAL_IDENTITY"),
            Map.entry("SCREEN_DISPLAY", "VISUAL_IDENTITY")
    );

    @Transactional
    public RiskScoreResponse recomputeRisk(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        // Bước 1: Áp dụng cửa sổ dedup theo category — lấy điểm cao nhất trong mỗi cửa sổ
        Map<String, Integer> categoryScores = computeCategoryScores(signals, now);

        // Bước 2: Giới hạn tối đa mỗi category
        int screenLeaveScore = Math.min(categoryScores.getOrDefault("SCREEN_LEAVE", 0), categoryCap("SCREEN_LEAVE"));
        int clipboardScore = Math.min(categoryScores.getOrDefault("CLIPBOARD", 0), categoryCap("CLIPBOARD"));
        int technicalScore = Math.min(categoryScores.getOrDefault("TECHNICAL", 0), categoryCap("TECHNICAL"));
        int identityScore = Math.min(categoryScores.getOrDefault("IDENTITY", 0), categoryCap("IDENTITY"));
        int heartbeatScore = Math.min(categoryScores.getOrDefault("HEARTBEAT", 0), categoryCap("HEARTBEAT"));
        int visualIdentityScore = Math.min(categoryScores.getOrDefault("VISUAL_IDENTITY", 0), categoryCap("VISUAL_IDENTITY"));

        // Bước 3: Tính tổng
        int behaviorScore = Math.min(70, screenLeaveScore + clipboardScore + technicalScore + heartbeatScore);
        int totalRisk = Math.min(100, behaviorScore + identityScore + visualIdentityScore);
        RiskLevel level = resolveLevel(totalRisk);

        // Bước 4: Tạo breakdown
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("screenLeaveScore", screenLeaveScore);
        breakdown.put("clipboardScore", clipboardScore);
        breakdown.put("technicalScore", technicalScore);
        breakdown.put("identityScore", identityScore);
        breakdown.put("visualIdentityScore", visualIdentityScore);
        breakdown.put("heartbeatScore", heartbeatScore);
        breakdown.put("behaviorScore", behaviorScore);
        breakdown.put("totalScore", totalRisk);

        // Bước 5: Cập nhật attempt
        attempt.setRiskScore(totalRisk);
        attempt.setRiskLevel(level);
        attempt.setSuspicious(level.isSuspicious());
        examAttemptRepository.save(attempt);

        // Bước 6: Đồng bộ flag
        ProctorFlag flag = syncRiskFlag(attempt, totalRisk, level, now);

        // Bước 7: Lưu snapshot
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

        // Bước 8: Gửi thông báo realtime cho signal mới nhất
        FraudSignal latestSignal = fraudSignalRepository
                .findTopByAttemptOrderByCreatedAtDesc(attempt)
                .orElse(null);
        Map<Long, Integer> scoreContributions = latestSignal != null
                ? computeScoreContributions(signals)
                : Map.of();
        if (latestSignal != null) {
            realtimeNotificationService.notifyFraudSignalRecorded(
                    attempt,
                    latestSignal,
                    scoreContributions.get(latestSignal.getId()),
                    totalRisk,
                    level,
                    breakdown
            );
        }

        // Bước 9: Gửi thông báo mức rủi ro nếu là suspicious+ (bao gồm flag và signal mới nhất để FE patch)
        if (level.isSuspicious()) {
            realtimeNotificationService.notifyRiskUpdated(
                    attempt, level, breakdown,
                    RiskActionType.NONE,
                    buildDecisionSummary(level, breakdown),
                    latestSignal,
                    latestSignal != null ? scoreContributions.get(latestSignal.getId()) : null,
                    flag
            );
        }

        return toResponse(attempt, totalRisk, level, breakdown, signals, flag, now);
    }

    /**
     * Tính lại điểm risk NHƯNG KHÔNG thực hiện các hành động tự động (không đồng bộ flag).
     * Được sử dụng bởi các read endpoints để tránh side effects.
     */
    @Transactional
    public RiskScoreResponse recomputeRiskSkipAutoActions(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);

        Map<String, Integer> categoryScores = computeCategoryScores(signals, now);
        int screenLeaveScore = Math.min(categoryScores.getOrDefault("SCREEN_LEAVE", 0), categoryCap("SCREEN_LEAVE"));
        int clipboardScore = Math.min(categoryScores.getOrDefault("CLIPBOARD", 0), categoryCap("CLIPBOARD"));
        int technicalScore = Math.min(categoryScores.getOrDefault("TECHNICAL", 0), categoryCap("TECHNICAL"));
        int identityScore = Math.min(categoryScores.getOrDefault("IDENTITY", 0), categoryCap("IDENTITY"));
        int heartbeatScore = Math.min(categoryScores.getOrDefault("HEARTBEAT", 0), categoryCap("HEARTBEAT"));
        int visualIdentityScore = Math.min(categoryScores.getOrDefault("VISUAL_IDENTITY", 0), categoryCap("VISUAL_IDENTITY"));

        int behaviorScore = Math.min(70, screenLeaveScore + clipboardScore + technicalScore + heartbeatScore);
        int totalRisk = Math.min(100, behaviorScore + identityScore + visualIdentityScore);
        RiskLevel level = resolveLevel(totalRisk);

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("screenLeaveScore", screenLeaveScore);
        breakdown.put("clipboardScore", clipboardScore);
        breakdown.put("technicalScore", technicalScore);
        breakdown.put("identityScore", identityScore);
        breakdown.put("visualIdentityScore", visualIdentityScore);
        breakdown.put("heartbeatScore", heartbeatScore);
        breakdown.put("behaviorScore", behaviorScore);
        breakdown.put("totalScore", totalRisk);

        return toResponse(attempt, totalRisk, level, breakdown, signals, null, now);
    }

    /**
     * Tính điểm category sử dụng clustering theo cửa sổ thời gian.
     *
     * Thuật toán:
     * 1. Nhóm signals theo category
     * 2. Sắp xếp signals trong mỗi category theo thời gian tạo
     * 3. Chia thành clusters: cluster mới bắt đầu khi khoảng cách >= cửa sổ category
     * 4. Mỗi cluster đóng góp điểm MAX của nó (signal đại diện)
     * 5. Điểm category = min(cap, tổng điểm các cluster)
     *
     * Ví dụ:
     * - TAB_SWITCH lúc 10:00:00 (+10) -> cluster 1 bắt đầu
     * - TAB_SWITCH lúc 10:00:30 (+10) -> trong cửa sổ 60s, cùng cluster (max=10)
     * - TAB_SWITCH lúc 10:01:10 (+10) -> khoảng cách từ cluster start >= 60s, cluster mới (max=10)
     * - Kết quả: 2 clusters, điểm = 10 + 10 = 20
     */
    private Map<String, Integer> computeCategoryScores(List<FraudSignal> signals, LocalDateTime now) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("SCREEN_LEAVE", 0);
        result.put("CLIPBOARD", 0);
        result.put("TECHNICAL", 0);
        result.put("IDENTITY", 0);
        result.put("HEARTBEAT", 0);
        result.put("VISUAL_IDENTITY", 0);

        // Nhóm signals theo category
        Map<String, List<FraudSignal>> signalsByCategory = new LinkedHashMap<>();
        for (FraudSignal signal : signals) {
            String signalType = normalizeSignalType(signal.getSignalType());
            String category = SIGNAL_CATEGORY.getOrDefault(signalType, "OTHER");
            if (category.equals("OTHER")) continue;
            signalsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(signal);
        }

        // Tính điểm cho mỗi category sử dụng clustering
        for (Map.Entry<String, List<FraudSignal>> entry : signalsByCategory.entrySet()) {
            String category = entry.getKey();
            List<FraudSignal> categorySignals = entry.getValue();
            int categoryScore = computeClusteredScore(category, categorySignals);
            result.put(category, categoryScore);
        }

        return result;
    }

    /**
     * Tính điểm cho một category sử dụng clustering theo cửa sổ thời gian.
     * Signals được sắp xếp theo thời gian, chia thành clusters dựa trên khoảng cách thời gian,
     * mỗi cluster đóng góp điểm max của nó, tổng bị giới hạn bởi category limit.
     */
    private int computeClusteredScore(String category, List<FraudSignal> signals) {
        if (signals == null || signals.isEmpty()) return 0;

        long dedupWindow = categoryDedupSeconds(category);

        // Sắp xếp theo thời gian tạo tăng dần
        List<FraudSignal> sorted = sortSignalsByCreatedAt(signals);

        // Xử lý đặc biệt cho IDENTITY: lấy điểm max (không clustering, mức session)
        if ("IDENTITY".equals(category)) {
            return sorted.stream()
                    .mapToInt(s -> signalScore(s.getSignalType()))
                    .max()
                    .orElse(0);
        }

        // Clustering cho các category khác
        int totalScore = 0;
        int clusterStartIndex = 0;
        int clusterMaxScore = 0;
        LocalDateTime clusterStartTime = null;

        for (int i = 0; i < sorted.size(); i++) {
            FraudSignal signal = sorted.get(i);
            String signalType = normalizeSignalType(signal.getSignalType());
            int signalScore = signalScore(signalType);
            LocalDateTime signalTime = signal.getCreatedAt() != null ? signal.getCreatedAt() : LocalDateTime.MIN;

            if (clusterStartTime == null) {
                // Signal đầu tiên: bắt đầu cluster mới
                clusterStartTime = signalTime;
                clusterStartIndex = i;
                clusterMaxScore = signalScore;
            } else {
                // Kiểm tra signal này có thuộc cluster hiện tại không
                long secondsSinceClusterStart = java.time.Duration.between(clusterStartTime, signalTime).getSeconds();

                if (secondsSinceClusterStart < dedupWindow) {
                    // Trong cửa sổ cluster: cập nhật điểm max nếu cao hơn
                    clusterMaxScore = Math.max(clusterMaxScore, signalScore);
                } else {
                    // Khoảng cách >= cửa sổ: đóng cluster hiện tại và bắt đầu cluster mới
                    totalScore += clusterMaxScore;

                    // Bắt đầu cluster mới
                    clusterStartTime = signalTime;
                    clusterMaxScore = signalScore;
                }
            }
        }

        // Thêm điểm cluster cuối cùng
        totalScore += clusterMaxScore;

        // Áp dụng giới hạn category
        return Math.min(totalScore, categoryCap(category));
    }

    public Map<Long, Integer> computeScoreContributions(List<FraudSignal> signals) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (signals == null || signals.isEmpty()) {
            return result;
        }

        List<FraudSignal> prefix = new ArrayList<>();
        int previousScore = 0;
        for (FraudSignal signal : sortSignalsByCreatedAt(signals)) {
            prefix.add(signal);
            int nextScore = computeTotalRisk(prefix);
            int contribution = Math.max(0, nextScore - previousScore);
            if (signal.getId() != null) {
                result.put(signal.getId(), contribution);
            }
            previousScore = nextScore;
        }
        return result;
    }

    private int computeTotalRisk(List<FraudSignal> signals) {
        Map<String, Integer> categoryScores = computeCategoryScores(signals, VietNamTime.now());
        int screenLeaveScore = Math.min(categoryScores.getOrDefault("SCREEN_LEAVE", 0), categoryCap("SCREEN_LEAVE"));
        int clipboardScore = Math.min(categoryScores.getOrDefault("CLIPBOARD", 0), categoryCap("CLIPBOARD"));
        int technicalScore = Math.min(categoryScores.getOrDefault("TECHNICAL", 0), categoryCap("TECHNICAL"));
        int identityScore = Math.min(categoryScores.getOrDefault("IDENTITY", 0), categoryCap("IDENTITY"));
        int heartbeatScore = Math.min(categoryScores.getOrDefault("HEARTBEAT", 0), categoryCap("HEARTBEAT"));
        int visualIdentityScore = Math.min(categoryScores.getOrDefault("VISUAL_IDENTITY", 0), categoryCap("VISUAL_IDENTITY"));
        int behaviorScore = Math.min(70, screenLeaveScore + clipboardScore + technicalScore + heartbeatScore);
        return Math.min(100, behaviorScore + identityScore + visualIdentityScore);
    }

    private List<FraudSignal> sortSignalsByCreatedAt(List<FraudSignal> signals) {
        if (signals == null) {
            return List.of();
        }
        return signals.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((FraudSignal signal) -> signal.getCreatedAt() != null ? signal.getCreatedAt() : LocalDateTime.MIN)
                        .thenComparing(signal -> signal.getId() != null ? signal.getId() : Long.MIN_VALUE))
                .toList();
    }

    private long categoryDedupSeconds(String category) {
        return switch (category) {
            case "SCREEN_LEAVE" -> Math.max(0L, screenLeaveDedupSeconds);
            case "CLIPBOARD" -> Math.max(0L, clipboardDedupSeconds);
            case "TECHNICAL" -> Math.max(0L, technicalDedupSeconds);
            case "VISUAL_IDENTITY" -> Math.max(0L, visualIdentityDedupSeconds);
            case "IDENTITY", "HEARTBEAT" -> 0L;
            default -> 0L;
        };
    }

    private int categoryCap(String category) {
        return switch (category) {
            case "SCREEN_LEAVE" -> Math.max(0, screenLeaveCap);
            case "CLIPBOARD" -> Math.max(0, clipboardCap);
            case "TECHNICAL" -> Math.max(0, technicalCap);
            case "IDENTITY" -> Math.max(0, identityCap);
            case "HEARTBEAT" -> Math.max(0, heartbeatCap);
            case "VISUAL_IDENTITY" -> Math.max(0, visualIdentityCap);
            default -> 100;
        };
    }

    private String normalizeSignalType(String signalType) {
        return FraudSignalTypeNormalizer.canonical(signalType);
    }

    private int signalScore(String signalType) {
        String normalized = normalizeSignalType(signalType);
        int fallback = SIGNAL_SCORES.getOrDefault(normalized, 0);
        return riskSignalScoreProperties.resolve(normalized, fallback);
    }

    /**
     * Đồng bộ flag dựa trên mức rủi ro.
     * < 50: không flag
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

        // Cập nhật flag OPEN hiện có hoặc tạo mới
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
                ? "Cần review gian lận nghiêm trọng"
                : "Cần review nguy cơ gian lận");
        flag.setDescription(buildDescription(score, level));
        flag.setStatus(ProctorFlagStatus.OPEN);

        return proctorFlagRepository.save(flag);
    }

    private String buildDescription(int score, RiskLevel level) {
        return String.format("Điểm rủi ro: %d — Mức độ: %s. Cần giáo viên review thêm.", score, level.name());
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
        Map<Long, Integer> scoreContributions = computeScoreContributions(signals);
        List<RiskScoreResponse.LatestSignalItem> latestSignals = fraudSignalRepository
                .findTop20ByAttemptOrderByCreatedAtDesc(attempt)
                .stream()
                .limit(5)
                .map(signal -> RiskScoreResponse.LatestSignalItem.builder()
                        .signalType(signal.getSignalType())
                        .category(signal.getCategory())
                        .displayMessage(signal.getDisplayMessage())
                        .riskImpact(signal.getRiskImpact())
                        .scoreContribution(scoreContributions.get(signal.getId()))
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

        // Các category đóng góp cao nhất
        if (breakdown.getOrDefault("screenLeaveScore", 0) > 0) {
            reasons.add("Hành vi rời màn hình bất thường");
            evidence.add("SCREEN_LEAVE: " + breakdown.get("screenLeaveScore") + " điểm");
        }
        if (breakdown.getOrDefault("clipboardScore", 0) > 0) {
            reasons.add("Hành vi clipboard bất thường");
            evidence.add("CLIPBOARD: " + breakdown.get("clipboardScore") + " điểm");
        }
        if (breakdown.getOrDefault("technicalScore", 0) > 0) {
            reasons.add("Kỹ thuật đáng nghi ngờ");
            evidence.add("TECHNICAL: " + breakdown.get("technicalScore") + " điểm");
        }
        if (breakdown.getOrDefault("identityScore", 0) > 0) {
            reasons.add("Phát hiện bất thường về định danh");
            evidence.add("IDENTITY: " + breakdown.get("identityScore") + " điểm");
        }

        if (breakdown.getOrDefault("visualIdentityScore", 0) > 0) {
            reasons.add("Camera AI phat hien bat thuong khuon mat hoac huong nhin");
            evidence.add("VISUAL_IDENTITY: " + breakdown.get("visualIdentityScore") + " diem");
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
