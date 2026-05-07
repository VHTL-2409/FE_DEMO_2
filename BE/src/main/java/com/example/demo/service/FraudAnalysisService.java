package com.example.demo.service;

import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Phân tích Gian lận cho thi trắc nghiệm.
 * Cung cấp các phương pháp phân tích gian lận: đạo văn đáp án, thời gian, thống kê, danh tiếng IP,
 * và phân tích kết hợp toàn diện.
 */
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final AnswerRepository answerRepository;
    private final AnswerSimilarityService answerSimilarityService;
    private final FraudWarningService fraudWarningService;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    // --- Phân tích đạo văn (so sánh đáp án) ---

    public PlagiarismAnalysisResponse analyzePlagiarismByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildPlagiarismResponse(exam, null);
    }

    public PlagiarismAnalysisResponse analyzePlagiarismByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        Exam exam = attempt.getExam();
        PlagiarismAnalysisResponse full = buildPlagiarismResponse(exam, null);
        String username = attempt.getStudent().getUsername();
        List<PlagiarismAnalysisResponse.PlagiarismReport> filtered = full.getPlagiarismReports().stream()
                .filter(r -> username.equals(r.getStudent1Name()) || username.equals(r.getStudent2Name()))
                .collect(Collectors.toList());
        return PlagiarismAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalSubmitted(full.getTotalSubmitted())
                .plagiarismReports(filtered)
                .build();
    }

    private PlagiarismAnalysisResponse buildPlagiarismResponse(Exam exam, ExamAttempt filterAttempt) {
        List<AnswerSimilarityService.SimilarityPair> pairs = answerSimilarityService.findSuspiciousPairs(exam);
        List<PlagiarismAnalysisResponse.PlagiarismReport> reports = new ArrayList<>();

        List<ExamAttempt> allAttempts = filterSubmittedAttempts(exam);
        Map<String, ExamAttempt> attemptsByUsername = allAttempts.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUsername(), a -> a, (l, r) -> l));

        for (int i = 0; i < pairs.size(); i++) {
            AnswerSimilarityService.SimilarityPair p = pairs.get(i);
            ExamAttempt a1 = attemptsByUsername.get(p.student1());
            ExamAttempt a2 = attemptsByUsername.get(p.student2());

            boolean timeCorrelation = hasTimeCorrelation(a1, a2);

            PlagiarismAnalysisResponse.PlagiarismReport report = PlagiarismAnalysisResponse.PlagiarismReport.builder()
                    .id(p.student1() + "-" + p.student2() + "-" + i)
                    .student1Name(p.student1())
                    .student2Name(p.student2())
                    .similarityScore(p.similarity())
                    .commonQuestions(p.commonQuestions())
                    .sameAnswers(p.sameAnswers())
                    .verdict(mapVerdict(p.similarity()))
                    .recommendation(generateRecommendation(p.similarity(), timeCorrelation))
                    .timeCorrelation(timeCorrelation)
                    .build();
            reports.add(report);
            recordAnswerPatternWarning(exam, a1, a2, p, timeCorrelation);
        }

        return PlagiarismAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalSubmitted(allAttempts.size())
                .plagiarismReports(reports)
                .build();
    }

    private boolean hasTimeCorrelation(ExamAttempt a1, ExamAttempt a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getStartedAt() == null || a2.getStartedAt() == null) return false;
        long diffSeconds = Math.abs(java.time.Duration.between(a1.getStartedAt(), a2.getStartedAt()).getSeconds());
        return diffSeconds <= 300;
    }

    private String mapVerdict(double similarity) {
        if (similarity >= 0.95) return "NGHIEM_TRONG";
        if (similarity >= 0.90) return "CAO";
        return "TRUNG_BINH";
    }

    private String generateRecommendation(double similarity, boolean timeCorrelation) {
        if (similarity >= 0.95 || (similarity >= 0.90 && timeCorrelation)) {
            return "Canh cao: can xem xet ky luong va có the yeu cau thi lai.";
        }
        if (similarity >= 0.90) {
            return "Theo doi: nen giam sat them trong cac buoi thi tiep theo.";
        }
        return "Bat thuong nhe: can ghi nhan nhung khong can hanh dong ngay.";
    }

    // --- Phân tích thời gian (hành vi trong giờ thi) ---

    public TimingAnalysisResponse analyzeTimingByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildTimingResponse(exam, null);
    }

    public TimingAnalysisResponse analyzeTimingByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildTimingResponse(attempt.getExam(), attempt);
    }

    private TimingAnalysisResponse buildTimingResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        List<TimingAnalysisResponse.TimingResultItem> items = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            Map<String, Long> eventCounts = signals.stream()
                    .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));

            Long tabSwitchCount = eventCounts.getOrDefault("TAB_SWITCH", 0L);
            Long blurCount = eventCounts.getOrDefault("WINDOW_BLUR", 0L);
            Long exitFsCount = eventCounts.getOrDefault("EXIT_FULLSCREEN", 0L)
                    + eventCounts.getOrDefault("FULLSCREEN_EVASION", 0L);

            if (tabSwitchCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("HIGH_TAB_SWITCH")
                        .severity(tabSwitchCount > 10 ? "HIGH" : tabSwitchCount > 5 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần chuyển tab: " + tabSwitchCount, tabSwitchCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "HIGH_TAB_SWITCH", tabSwitchCount, "So lan chuyen tab: " + tabSwitchCount);
            }
            if (blurCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("EXCESSIVE_BLUR")
                        .severity(blurCount > 15 ? "HIGH" : blurCount > 5 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần mất tiêu điểm cửa sổ: " + blurCount, blurCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "EXCESSIVE_BLUR", blurCount, "So lan mat focus: " + blurCount);
            }
            if (exitFsCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("MULTIPLE_FULLSCREEN_EXIT")
                        .severity(exitFsCount > 5 ? "HIGH" : exitFsCount > 2 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần thoát toàn màn hình: " + exitFsCount, exitFsCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "MULTIPLE_FULLSCREEN_EXIT", exitFsCount, "So lan thoat fullscreen: " + exitFsCount);
            }

            long heartbeatEventCount = eventCounts.getOrDefault("HEARTBEAT_STALE", 0L)
                    + eventCounts.getOrDefault("NETWORK_INSTABILITY", 0L)
                    + eventCounts.getOrDefault("SESSION_RECOVERY", 0L);
            if (heartbeatEventCount > 0) {
                recordSessionIntegrityWarning(attempt, "HEARTBEAT_OR_RECONNECT", heartbeatEventCount, "Bat thuong heartbeat/reconnect: " + heartbeatEventCount);
            }
            recordFastCompletionWarning(exam, attempt);

            if (attempt.getRiskScore() != null && attempt.getRiskScore() >= 31) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("RISK_LEVEL_SUSPICIOUS")
                        .severity(attempt.getRiskScore() >= 61 ? "HIGH" : "MEDIUM")
                        .evidence(buildEvidenceMap("Điểm risk: " + attempt.getRiskScore(), attempt.getRiskScore()))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
            }
        }

        return TimingAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .timingResults(items)
                .build();
    }

    private Map<String, Object> buildEvidenceMap(String message, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", message);
        map.put("value", value);
        return map;
    }

    // --- Phân tích thống kê (phân tích điểm thi) ---

    public StatisticalAnalysisResponse analyzeStatisticalByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildStatisticalResponse(exam, null);
    }

    public StatisticalAnalysisResponse analyzeStatisticalByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildStatisticalResponse(attempt.getExam(), attempt);
    }

    private StatisticalAnalysisResponse buildStatisticalResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        if (attempts.isEmpty()) {
            return StatisticalAnalysisResponse.builder()
                    .examId(exam.getId())
                    .examTitle(exam.getTitle())
                    .totalAttempts(0)
                    .statisticalResults(List.of())
                    .build();
        }

        List<Double> scores = attempts.stream()
                .filter(a -> a.getScore() != null)
                .map(a -> a.getScore())
                .collect(Collectors.toList());

        double mean = scores.isEmpty() ? 0 : scores.stream().mapToDouble(Double::doubleValue).sum() / scores.size();
        double variance = scores.isEmpty() ? 0 : scores.stream().mapToDouble(s -> (s - mean) * (s - mean)).sum() / scores.size();
        double stdDev = Math.sqrt(variance);
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        StatisticalAnalysisResponse.ScoreStats scoreStats = StatisticalAnalysisResponse.ScoreStats.builder()
                .mean(Math.round(mean * 100.0) / 100.0)
                .stdDev(Math.round(stdDev * 100.0) / 100.0)
                .min(min)
                .max(max)
                .count(scores.size())
                .build();

        List<StatisticalAnalysisResponse.StatisticalResultItem> results = new ArrayList<>();

        for (ExamAttempt a : attempts) {
            if (a.getScore() == null) continue;
            double score = a.getScore();

            if (stdDev > 0 && Math.abs(score - mean) > 2 * stdDev) {
                double zScore = Math.abs(score - mean) / stdDev;
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("OUTLIER_SCORE")
                        .severity(zScore > 3 ? "HIGH" : "MEDIUM")
                        .evidence(Map.of(
                                "description", String.format("Điểm %.1f cách xa trung bình %.1f (%.1f độ lệch chuẩn)", score, mean, zScore),
                                "zScore", Math.round(zScore * 100.0) / 100.0,
                                "score", score,
                                "mean", mean
                        ))
                        .build());
                recordStatisticalWarning(exam, a, "OUTLIER_SCORE", zScore > 3 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM, Map.of(
                        "description", String.format("Diem %.1f cach xa trung binh %.1f (%.1f do lech chuan)", score, mean, zScore),
                        "zScore", Math.round(zScore * 100.0) / 100.0,
                        "score", score,
                        "mean", mean
                ));
            }

            if (isHighScoreFastCompletion(exam, a, mean, stdDev)) {
                recordStatisticalWarning(exam, a, "HIGH_SCORE_FAST_TIME", SignalSeverity.MEDIUM, Map.of(
                        "description", "Diem cao nhung thoi gian lam bai thap, can kiem tra lai.",
                        "score", score,
                        "mean", mean,
                        "stdDev", stdDev,
                        "durationSeconds", attemptDurationSeconds(a)
                ));
            }

            if (Boolean.TRUE.equals(a.getSuspicious()) && a.getRiskScore() != null && a.getRiskScore() >= 61) {
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("HIGH_RISK_ATTEMPT")
                        .severity(a.getRiskScore() >= 81 ? "HIGH" : "MEDIUM")
                        .evidence(Map.of(
                                "description", "Thí sinh có điểm risk cao (" + a.getRiskScore() + ") và điểm thi " + score,
                                "riskScore", a.getRiskScore(),
                                "score", score
                        ))
                        .build());
            }
        }

        return StatisticalAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .scoreStats(scoreStats)
                .statisticalResults(results)
                .build();
    }

    // --- Phân tích hành vi click (cho thi trắc nghiệm) ---

    public BehaviorAnalysisResponse analyzeBehaviorByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildBehaviorResponse(exam, null);
    }

    public BehaviorAnalysisResponse analyzeBehaviorByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildBehaviorResponse(attempt.getExam(), attempt);
    }

    private BehaviorAnalysisResponse buildBehaviorResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        Map<Long, List<String>> attemptAnomalies = new HashMap<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            List<String> anomalies = new ArrayList<>();

            for (FraudSignal signal : signals) {
                String type = signal.getSignalType();
                // Các signal liên quan đến hành vi trắc nghiệm
                if (type.equals("AUTOMATED_INPUT_DETECTED")
                        || type.equals("MOUSE_SIGNATURE_ANOMALY")
                        || type.equals("INTERRUPTION_PATTERN")) {
                    anomalies.add(type);
                }
            }

            if (!anomalies.isEmpty()) {
                attemptAnomalies.put(attempt.getId(), anomalies);
            }
        }

        List<String> allAnomalies = attemptAnomalies.values().stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return BehaviorAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .anomalies(allAnomalies)
                .build();
    }

    // --- Phân tích danh tiếng IP ---

    public IpReputationAnalysisResponse analyzeIpReputation(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildIpResponse(exam);
    }

    private IpReputationAnalysisResponse buildIpResponse(Exam exam) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        Map<String, List<ExamAttempt>> ipGroups = new HashMap<>();
        for (ExamAttempt attempt : attempts) {
            String ip = attempt.getClientIp();
            if (ip != null && !ip.isBlank()) {
                ipGroups.computeIfAbsent(ip, k -> new ArrayList<>()).add(attempt);
            }
        }

        List<IpReputationAnalysisResponse.IpResultItem> suspicious = new ArrayList<>();
        for (Map.Entry<String, List<ExamAttempt>> entry : ipGroups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                List<String> usernames = entry.getValue().stream()
                        .map(a -> a.getStudent().getUsername())
                        .distinct()
                        .collect(Collectors.toList());
                String riskLevel = entry.getValue().size() >= 3 ? "HIGH" : "MEDIUM";

                Map<String, String> geoLocation = new LinkedHashMap<>();
                geoLocation.put("city", "Unknown");
                geoLocation.put("country", "Unknown");

                suspicious.add(IpReputationAnalysisResponse.IpResultItem.builder()
                        .ipAddress(maskIp(entry.getKey()))
                        .isVpn(false)
                        .isProxy(false)
                        .isTor(false)
                        .hostname(null)
                        .geoLocation(geoLocation)
                        .subnetCount(usernames.size())
                        .attemptCount(entry.getValue().size())
                        .studentCount(usernames.size())
                        .studentUsernames(usernames)
                        .riskLevel(riskLevel)
                        .build());
                recordIdentityNetworkWarning(exam, entry.getKey(), entry.getValue(), riskLevel);
            }
        }

        return IpReputationAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .uniqueIpCount(ipGroups.size())
                .ipResults(suspicious)
                .build();
    }

    // --- Phân tích toàn diện ---

    public ComprehensiveAnalysisResponse analyzeComprehensiveByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildComprehensiveResponse(exam, null);
    }

    public ComprehensiveAnalysisResponse analyzeComprehensiveByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildComprehensiveResponse(attempt.getExam(), attempt);
    }

    private ComprehensiveAnalysisResponse buildComprehensiveResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        int flaggedCount = (int) attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getSuspicious()) || (a.getRiskScore() != null && a.getRiskScore() >= 31))
                .count();

        PlagiarismAnalysisResponse plagiarism = buildPlagiarismResponse(exam, filterAttempt);
        TimingAnalysisResponse timing = buildTimingResponse(exam, filterAttempt);
        StatisticalAnalysisResponse statistical = buildStatisticalResponse(exam, filterAttempt);
        BehaviorAnalysisResponse behavior = buildBehaviorResponse(exam, filterAttempt);
        IpReputationAnalysisResponse ipReputation = buildIpResponse(exam);

        List<ComprehensiveAnalysisResponse.FlaggedAttemptItem> flagged = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getSuspicious()) || (attempt.getRiskScore() != null && attempt.getRiskScore() >= 31)) {
                List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
                List<String> indicators = signals.stream()
                        .filter(s -> s.getSeverity() == SignalSeverity.HIGH || s.getSeverity() == SignalSeverity.CRITICAL)
                        .map(FraudSignal::getSignalType)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                flagged.add(ComprehensiveAnalysisResponse.FlaggedAttemptItem.builder()
                        .attemptId(attempt.getId())
                        .studentUsername(attempt.getStudent().getUsername())
                        .riskScore(attempt.getRiskScore() != null ? attempt.getRiskScore() : 0)
                        .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : "CLEAN")
                        .fraudIndicators(indicators)
                        .build());
            }
        }
        flagged.sort((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()));

        List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns = buildSuspiciousPatterns(attempts);

        return ComprehensiveAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .flaggedAttempts(flaggedCount)
                .plagiarism(plagiarism)
                .timing(timing)
                .statistical(statistical)
                .behavior(behavior)
                .ipReputation(ipReputation)
                .flaggedAttemptItems(flagged)
                .suspiciousPatterns(patterns)
                .build();
    }

    /**
     * Tính toán các mẫu hành vi đáng ngờ cho thi trắc nghiệm.
     */
    private List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> buildSuspiciousPatterns(List<ExamAttempt> attempts) {
        List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            Map<String, Long> typeCounts = signals.stream()
                    .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));

            addPatternIf(patterns, attempt, "IP_ANOMALY", "Phat hien IP trung lap",
                    typeCounts.getOrDefault("IP_ANOMALY", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "SYNC_BEHAVIOR", "Hanh vi dong bo voi thi sinh khac",
                    typeCounts.getOrDefault("SYNC_BEHAVIOR", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "TIMING_ANOMALY", "Lam bai qua nhanh",
                    typeCounts.getOrDefault("TIMING_ANOMALY", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "ANSWER_SIMILARITY", "Tuong dong dap an cao voi thi sinh khac",
                    typeCounts.getOrDefault("ANSWER_SIMILARITY", 0L) + typeCounts.getOrDefault("EXACT_ANSWER_MATCH", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "AUTOMATED_INPUT", "Phat hien input tu dong",
                    typeCounts.getOrDefault("AUTOMATED_INPUT_DETECTED", 0L), 1L, 2L);
        }
        return patterns;
    }

    private void addPatternIf(
            List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns,
            ExamAttempt attempt,
            String patternType,
            String title,
            long count,
            long mediumThreshold,
            long highThreshold
    ) {
        if (count < mediumThreshold) return;
        String severity = count >= highThreshold ? "HIGH" : "MEDIUM";
        patterns.add(ComprehensiveAnalysisResponse.SuspiciousPatternItem.builder()
                .id(attempt.getId() + "-" + patternType)
                .patternType(patternType)
                .title(title)
                .description(count + " lan " + title.toLowerCase() + (count >= highThreshold ? " (cao)" : ""))
                .severity(severity)
                .attemptId(attempt.getId())
                .studentUsername(attempt.getStudent().getUsername())
                .count((int) count)
                .build());
    }

    // --- Các hàm hỗ trợ ---

    private void recordAnswerPatternWarning(
            Exam exam,
            ExamAttempt a1,
            ExamAttempt a2,
            AnswerSimilarityService.SimilarityPair pair,
            boolean timeCorrelation
    ) {
        if (a1 == null || a2 == null) {
            return;
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("student1", pair.student1());
        evidence.put("student2", pair.student2());
        evidence.put("similarity", pair.similarity());
        evidence.put("commonQuestions", pair.commonQuestions());
        evidence.put("sameAnswers", pair.sameAnswers());
        evidence.put("timeCorrelation", timeCorrelation);
        fraudWarningService.recordWarning(
                exam,
                null,
                FraudWarningCategory.ANSWER_PATTERN,
                "ANSWER_SIMILARITY",
                pair.similarity() >= 0.95 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                pair.similarity(),
                "Nghi van trung mau dap an giua nhieu thi sinh",
                evidence,
                "answer_similarity",
                List.of(a1.getId(), a2.getId())
        );
        if (timeCorrelation) {
            fraudWarningService.recordWarning(
                    exam,
                    null,
                    FraudWarningCategory.SYNCHRONIZATION,
                    "ANSWER_SYNC",
                    SignalSeverity.MEDIUM,
                    Math.min(1.0, pair.similarity()),
                    "Nghi van dong bo thoi diem tra loi",
                    evidence,
                    "answer_similarity",
                    List.of(a1.getId(), a2.getId())
            );
        }
    }

    private void recordSessionIntegrityWarning(ExamAttempt attempt, String type, long count, String message) {
        SignalSeverity severity = count >= 10 ? SignalSeverity.HIGH : count >= 3 ? SignalSeverity.MEDIUM : SignalSeverity.LOW;
        fraudWarningService.recordWarning(
                attempt.getExam(),
                attempt,
                FraudWarningCategory.SESSION_INTEGRITY,
                type,
                severity,
                Math.min(0.95, 0.55 + count * 0.05),
                message,
                Map.of("count", count, "message", message),
                "monitoring_event",
                List.of(attempt.getId())
        );
    }

    private void recordFastCompletionWarning(Exam exam, ExamAttempt attempt) {
        Long durationSeconds = attemptDurationSeconds(attempt);
        if (durationSeconds == null || durationSeconds <= 0) {
            return;
        }
        long questionCount = Math.max(questionRepository.countByExam(exam), 1);
        long suspiciousThreshold = Math.max(60, questionCount * 15);
        if (durationSeconds > suspiciousThreshold) {
            return;
        }
        fraudWarningService.recordWarning(
                exam,
                attempt,
                FraudWarningCategory.TIMING_PATTERN,
                "FAST_COMPLETION",
                durationSeconds < Math.max(45, questionCount * 10) ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                0.75,
                "Thoi gian hoan thanh bai trac nghiem thap bat thuong",
                Map.of(
                        "durationSeconds", durationSeconds,
                        "questionCount", questionCount,
                        "thresholdSeconds", suspiciousThreshold
                ),
                "timing_analysis",
                List.of(attempt.getId())
        );
    }

    private void recordStatisticalWarning(Exam exam, ExamAttempt attempt, String type, SignalSeverity severity, Map<String, Object> evidence) {
        fraudWarningService.recordWarning(
                exam,
                attempt,
                FraudWarningCategory.POST_EXAM_STATISTICAL,
                type,
                severity,
                severity == SignalSeverity.HIGH ? 0.85 : 0.7,
                "Bat thuong thong ke sau thi can kiem tra",
                evidence,
                "statistical_analysis",
                List.of(attempt.getId())
        );
    }

    private void recordIdentityNetworkWarning(Exam exam, String ipAddress, List<ExamAttempt> attempts, String riskLevel) {
        List<Long> relatedAttemptIds = attempts.stream()
                .map(ExamAttempt::getId)
                .filter(Objects::nonNull)
                .toList();
        List<String> students = attempts.stream()
                .map(a -> a.getStudent().getUsername())
                .distinct()
                .toList();
        fraudWarningService.recordWarning(
                exam,
                null,
                FraudWarningCategory.IDENTITY_NETWORK,
                "DUPLICATE_IP_GROUP",
                "HIGH".equalsIgnoreCase(riskLevel) ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                0.8,
                "Nhieu thi sinh dung chung IP trong cung bai thi",
                Map.of(
                        "ipAddress", maskIp(ipAddress),
                        "attemptCount", attempts.size(),
                        "studentUsernames", students
                ),
                "ip_reputation",
                relatedAttemptIds
        );
    }

    private boolean isHighScoreFastCompletion(Exam exam, ExamAttempt attempt, double mean, double stdDev) {
        if (attempt.getScore() == null) {
            return false;
        }
        Long durationSeconds = attemptDurationSeconds(attempt);
        if (durationSeconds == null || durationSeconds <= 0) {
            return false;
        }
        long questionCount = Math.max(questionRepository.countByExam(exam), 1);
        boolean highScore = stdDev > 0 ? attempt.getScore() >= mean + stdDev : attempt.getScore() >= mean;
        return highScore && durationSeconds <= Math.max(90, questionCount * 20);
    }

    private Long attemptDurationSeconds(ExamAttempt attempt) {
        if (attempt.getStartedAt() == null || attempt.getSubmittedAt() == null) {
            return null;
        }
        return java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
    }

    private List<ExamAttempt> filterSubmittedAttempts(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime()).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .collect(Collectors.toList());
        }
        return examAttemptRepository.findByExam(exam).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                .collect(Collectors.toList());
    }

    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return ip;
    }
}
