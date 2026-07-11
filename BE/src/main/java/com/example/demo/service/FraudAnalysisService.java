package com.example.demo.service;

import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private static final Duration ANALYSIS_WARNING_DEDUP_WINDOW = Duration.ofDays(30);

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final AnswerSimilarityService answerSimilarityService;
    private final FraudWarningService fraudWarningService;
    private final QuestionRepository questionRepository;

    @Value("${demo.fraud.analysis.plagiarism.high-similarity-threshold:0.95}")
    private double plagiarismHighSimilarityThreshold;

    @Value("${demo.fraud.analysis.plagiarism.medium-similarity-threshold:0.90}")
    private double plagiarismMediumSimilarityThreshold;

    @Value("${demo.fraud.analysis.plagiarism.time-correlation-seconds:300}")
    private long plagiarismTimeCorrelationSeconds;

    @Value("${demo.fraud.analysis.session.tab-switch.medium-threshold:5}")
    private long tabSwitchMediumThreshold;

    @Value("${demo.fraud.analysis.session.tab-switch.high-threshold:10}")
    private long tabSwitchHighThreshold;

    @Value("${demo.fraud.analysis.session.window-blur.medium-threshold:5}")
    private long windowBlurMediumThreshold;

    @Value("${demo.fraud.analysis.session.window-blur.high-threshold:15}")
    private long windowBlurHighThreshold;

    @Value("${demo.fraud.analysis.session.fullscreen-exit.medium-threshold:2}")
    private long fullscreenExitMediumThreshold;

    @Value("${demo.fraud.analysis.session.fullscreen-exit.high-threshold:5}")
    private long fullscreenExitHighThreshold;

    @Value("${demo.fraud.analysis.session.heartbeat.medium-threshold:3}")
    private long heartbeatMediumThreshold;

    @Value("${demo.fraud.analysis.session.heartbeat.high-threshold:10}")
    private long heartbeatHighThreshold;

    @Value("${demo.fraud.analysis.timing.fast-completion.medium-factor-seconds-per-question:15}")
    private long fastCompletionMediumSecondsPerQuestion;

    @Value("${demo.fraud.analysis.timing.fast-completion.high-factor-seconds-per-question:10}")
    private long fastCompletionHighSecondsPerQuestion;

    @Value("${demo.fraud.analysis.timing.fast-completion.medium-min-seconds:60}")
    private long fastCompletionMediumMinSeconds;

    @Value("${demo.fraud.analysis.timing.fast-completion.high-min-seconds:45}")
    private long fastCompletionHighMinSeconds;

    @Value("${demo.fraud.analysis.ip-reputation.medium-attempts:2}")
    private int duplicateIpMediumAttempts;

    @Value("${demo.fraud.analysis.ip-reputation.high-attempts:3}")
    private int duplicateIpHighAttempts;

    

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
        return diffSeconds <= Math.max(plagiarismTimeCorrelationSeconds, 1L);
    }

    private String mapVerdict(double similarity) {
        if (similarity >= highSimilarityThreshold()) return "NGHIEM_TRONG";
        if (similarity >= mediumSimilarityThreshold()) return "CAO";
        return "TRUNG_BINH";
    }

    private String generateRecommendation(double similarity, boolean timeCorrelation) {
        if (similarity >= highSimilarityThreshold() || (similarity >= mediumSimilarityThreshold() && timeCorrelation)) {
            return "Cảnh báo cao: cần xem xét kỹ lưỡng và có thể yêu cầu thi lại.";
        }
        if (similarity >= mediumSimilarityThreshold()) {
            return "Theo dõi: nên giám sát thêm trong các buổi thi tiếp theo.";
        }
        return "Bất thường nhẹ: cần ghi nhận nhưng chưa cần hành động ngay.";
    }

    private double highSimilarityThreshold() {
        return normalizeSimilarityThreshold(plagiarismHighSimilarityThreshold, 0.95d);
    }

    private double mediumSimilarityThreshold() {
        double high = highSimilarityThreshold();
        double medium = normalizeSimilarityThreshold(plagiarismMediumSimilarityThreshold, 0.90d);
        return Math.min(Math.max(0.0d, high - 0.01d), medium);
    }

    private double normalizeSimilarityThreshold(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    

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
                    .collect(Collectors.groupingBy(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()), Collectors.counting()));

            long tabSwitchCount = countSignals(eventCounts, "TAB_SWITCH");
            long blurCount = countSignals(eventCounts, "WINDOW_BLUR");
            long exitFsCount = countSignals(eventCounts, "EXIT_FULLSCREEN");

            if (tabSwitchCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("HIGH_TAB_SWITCH")
                        .severity(severityForCount(tabSwitchCount, tabSwitchMediumThreshold, tabSwitchHighThreshold).name())
                        .evidence(buildEvidenceMap("Số lần chuyển tab: " + tabSwitchCount, tabSwitchCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "HIGH_TAB_SWITCH", tabSwitchCount,
                        "Số lần chuyển tab: " + tabSwitchCount, tabSwitchMediumThreshold, tabSwitchHighThreshold);
            }
            if (blurCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("EXCESSIVE_BLUR")
                        .severity(severityForCount(blurCount, windowBlurMediumThreshold, windowBlurHighThreshold).name())
                        .evidence(buildEvidenceMap("Số lần mất tiêu điểm cửa sổ: " + blurCount, blurCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "EXCESSIVE_BLUR", blurCount,
                        "Số lần mất focus: " + blurCount, windowBlurMediumThreshold, windowBlurHighThreshold);
            }
            if (exitFsCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("MULTIPLE_FULLSCREEN_EXIT")
                        .severity(severityForCount(exitFsCount, fullscreenExitMediumThreshold, fullscreenExitHighThreshold).name())
                        .evidence(buildEvidenceMap("Số lần thoát toàn màn hình: " + exitFsCount, exitFsCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
                recordSessionIntegrityWarning(attempt, "MULTIPLE_FULLSCREEN_EXIT", exitFsCount,
                        "Số lần thoát fullscreen: " + exitFsCount, fullscreenExitMediumThreshold, fullscreenExitHighThreshold);
            }

            long heartbeatEventCount = countSignals(eventCounts, "HEARTBEAT_STALE", "NETWORK_INSTABILITY", "SESSION_RECOVERY");
            if (heartbeatEventCount > 0) {
                recordSessionIntegrityWarning(attempt, "HEARTBEAT_OR_RECONNECT", heartbeatEventCount,
                        "Bất thường heartbeat/reconnect: " + heartbeatEventCount,
                        heartbeatMediumThreshold, heartbeatHighThreshold);
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

    private SignalSeverity severityForCount(long count, long mediumThreshold, long highThreshold) {
        long safeMedium = Math.max(mediumThreshold, 1L);
        long safeHigh = Math.max(highThreshold, safeMedium + 1L);
        long criticalThreshold = Math.max(safeHigh * 2L, safeMedium * 3L);
        if (count >= criticalThreshold) return SignalSeverity.CRITICAL;
        if (count >= safeHigh) return SignalSeverity.HIGH;
        if (count >= safeMedium) return SignalSeverity.MEDIUM;
        return SignalSeverity.LOW;
    }

    private long countSignals(Map<String, Long> counts, String... signalTypes) {
        if (counts == null || counts.isEmpty() || signalTypes == null) {
            return 0L;
        }
        long total = 0L;
        for (String signalType : signalTypes) {
            if (signalType != null) {
                total += counts.getOrDefault(signalType, 0L);
            }
        }
        return total;
    }

    

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
        List<ExamAttempt> examAttempts = filterSubmittedAttempts(exam);
        List<ExamAttempt> attempts = examAttempts;
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }
        List<Double> examScores = examAttempts.stream()
                .map(ExamAttempt::getScore)
                .filter(Objects::nonNull)
                .toList();

        double mean = examScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = computeStdDev(examScores, mean);
        double min = examScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = examScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        List<StatisticalAnalysisResponse.StatisticalResultItem> results = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (attempt.getScore() == null) {
                continue;
            }
            double zScore = stdDev > 0 ? (attempt.getScore() - mean) / stdDev : 0.0;
            Integer riskScore = attempt.getRiskScore();
            Long durationSeconds = attemptDurationSeconds(attempt);
            long questionCount = Math.max(questionRepository.countByExam(exam), 1);
            long fastThresholdSeconds = Math.max(
                    Math.max(fastCompletionMediumMinSeconds, 1L),
                    questionCount * Math.max(fastCompletionMediumSecondsPerQuestion, 1L)
            );

            if (Math.abs(zScore) >= 2.0 && examScores.size() >= 5) {
                SignalSeverity severity = Math.abs(zScore) >= 3.0 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM;
                Map<String, Object> evidence = buildStatisticalEvidence(attempt, mean, stdDev, zScore, durationSeconds, riskScore);
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType(attempt.getScore() >= mean ? "SCORE_HIGH_OUTLIER" : "SCORE_LOW_OUTLIER")
                        .severity(severity.name())
                        .evidence(evidence)
                        .build());
                recordStatisticalWarning(attempt, "SCORE_OUTLIER", severity,
                        "Điểm số lệch đáng kể so với phân bố lớp", evidence);
            }

            if (riskScore != null && riskScore >= 40 && attempt.getScore() >= Math.max(80.0, mean + stdDev)) {
                Map<String, Object> evidence = buildStatisticalEvidence(attempt, mean, stdDev, zScore, durationSeconds, riskScore);
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("HIGH_SCORE_WITH_HIGH_RISK")
                        .severity(riskScore >= 61 ? SignalSeverity.HIGH.name() : SignalSeverity.MEDIUM.name())
                        .evidence(evidence)
                        .build());
                recordStatisticalWarning(attempt, "HIGH_SCORE_WITH_HIGH_RISK",
                        riskScore >= 61 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                        "Điểm cao đi kèm mức rủi ro giám sát cao", evidence);
            }

            if (durationSeconds != null
                    && durationSeconds <= fastThresholdSeconds
                    && attempt.getScore() >= Math.max(75.0, mean + Math.max(stdDev, 5.0))) {
                Map<String, Object> evidence = buildStatisticalEvidence(attempt, mean, stdDev, zScore, durationSeconds, riskScore);
                evidence.put("fastThresholdSeconds", fastThresholdSeconds);
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("FAST_COMPLETION_HIGH_SCORE")
                        .severity(SignalSeverity.HIGH.name())
                        .evidence(evidence)
                        .build());
                recordStatisticalWarning(attempt, "FAST_COMPLETION_HIGH_SCORE", SignalSeverity.HIGH,
                        "Hoàn thành rất nhanh nhưng đạt điểm cao", evidence);
            }
        }

        return StatisticalAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .scoreStats(StatisticalAnalysisResponse.ScoreStats.builder()
                        .mean(round(mean))
                        .stdDev(round(stdDev))
                        .min(round(min))
                        .max(round(max))
                        .count(examScores.size())
                        .build())
                .statisticalResults(results)
                .build();
    }

    

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
        List<String> anomalies = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            if (signals.isEmpty()) {
                continue;
            }
            Map<String, Long> counts = signals.stream()
                    .collect(Collectors.groupingBy(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()), Collectors.counting()));
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "TAB_SWITCH", "Chuyển tab nhiều lần", tabSwitchMediumThreshold, tabSwitchHighThreshold,
                    FraudWarningCategory.SESSION_INTEGRITY);
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "WINDOW_BLUR", "Cửa sổ mất focus nhiều lần", windowBlurMediumThreshold, windowBlurHighThreshold,
                    FraudWarningCategory.SESSION_INTEGRITY);
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "EXIT_FULLSCREEN", "Thoát toàn màn hình nhiều lần", fullscreenExitMediumThreshold, fullscreenExitHighThreshold,
                    FraudWarningCategory.SESSION_INTEGRITY);
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "COPY_PASTE", "Sao chép hoặc dán nội dung bất thường", 1L, 2L,
                    FraudWarningCategory.SESSION_INTEGRITY);
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "DEVTOOLS_OPEN", "Mở công cụ nhà phát triển", 1L, 1L,
                    FraudWarningCategory.SESSION_INTEGRITY);
            addBehaviorAnomaly(anomalies, attempt, counts,
                    "PRINT_SCREEN", "Chụp màn hình trong lúc thi", 1L, 2L,
                    FraudWarningCategory.SESSION_INTEGRITY);

            long heartbeatCount = countSignals(counts, "HEARTBEAT_STALE", "NETWORK_INSTABILITY", "SESSION_RECOVERY");
            if (heartbeatCount >= heartbeatMediumThreshold) {
                SignalSeverity severity = severityForCount(heartbeatCount, heartbeatMediumThreshold, heartbeatHighThreshold);
                Map<String, Object> evidence = buildBehaviorEvidence(attempt, "HEARTBEAT_OR_RECONNECT", heartbeatCount, "Mất kết nối hoặc khôi phục phiên bất thường");
                anomalies.add(formatAnomaly(attempt, "HEARTBEAT_OR_RECONNECT", severity, heartbeatCount));
                recordBehaviorWarning(attempt, FraudWarningCategory.SESSION_INTEGRITY, "HEARTBEAT_OR_RECONNECT", severity,
                        "Mất kết nối hoặc khôi phục phiên bất thường", evidence);
            }

            Map<String, Long> cameraCounts = counts.entrySet().stream()
                    .filter(e -> isCameraSignal(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (Map.Entry<String, Long> entry : cameraCounts.entrySet()) {
                if (entry.getValue() <= 0) continue;
                SignalSeverity severity = severityForCount(entry.getValue(), 1L, 3L);
                Map<String, Object> evidence = buildBehaviorEvidence(attempt, entry.getKey(), entry.getValue(), "Tín hiệu AI camera cần rà soát");
                anomalies.add(formatAnomaly(attempt, entry.getKey(), severity, entry.getValue()));
                recordBehaviorWarning(attempt, FraudWarningCategory.CAMERA_PROCTORING, entry.getKey(), severity,
                        "Tín hiệu AI camera cần rà soát", evidence);
            }
        }
        return BehaviorAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .anomalies(anomalies)
                .analyzedAt(com.example.demo.common.VietNamTime.now())
                .build();
    }

    

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
            if (entry.getValue().size() >= Math.max(duplicateIpMediumAttempts, 2)) {
                List<String> usernames = entry.getValue().stream()
                        .map(a -> a.getStudent().getUsername())
                        .distinct()
                        .collect(Collectors.toList());
                String riskLevel = entry.getValue().size() >= Math.max(duplicateIpHighAttempts, duplicateIpMediumAttempts + 1)
                        ? "HIGH"
                        : "MEDIUM";

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
        IpReputationAnalysisResponse ipReputation = buildIpResponse(exam);
        StatisticalAnalysisResponse statistical = buildStatisticalResponse(exam, filterAttempt);
        BehaviorAnalysisResponse behavior = buildBehaviorResponse(exam, filterAttempt);

        List<ComprehensiveAnalysisResponse.FlaggedAttemptItem> flagged = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getSuspicious()) || (attempt.getRiskScore() != null && attempt.getRiskScore() >= 31)) {
                List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
                List<String> indicators = signals.stream()
                        .filter(s -> s.getSeverity() == SignalSeverity.HIGH || s.getSeverity() == SignalSeverity.CRITICAL)
                        .map(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()))
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

    

    private List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> buildSuspiciousPatterns(List<ExamAttempt> attempts) {
        List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            Map<String, Long> typeCounts = signals.stream()
                    .collect(Collectors.groupingBy(s -> FraudSignalTypeNormalizer.canonical(s.getSignalType()), Collectors.counting()));

            addPatternIf(patterns, attempt, "IP_CHANGED", "Phát hiện IP hoặc thiết bị đáng ngờ",
                    countSignals(typeCounts, "IP_CHANGED", "DUPLICATE_IP", "IP_FINGERPRINT_GRAPH",
                            "DEVICE_FINGERPRINT_CHANGED", "MULTIPLE_DEVICE_SESSION"), 1L, 2L);
            addPatternIf(patterns, attempt, "ANSWER_SIMILARITY", "Tương đồng đáp án cao với thí sinh khác",
                    countSignals(typeCounts, "ANSWER_SIMILARITY", "EXACT_ANSWER_MATCH"), 1L, 2L);
            addPatternIf(patterns, attempt, "CLIPBOARD_MANIPULATION", "Sao chép hoặc dán bất thường",
                    countSignals(typeCounts, "COPY_PASTE"), 1L, 2L);
            addPatternIf(patterns, attempt, "TECHNICAL_TOOL_USE", "Công cụ kỹ thuật đáng ngờ",
                    countSignals(typeCounts, "DEVTOOLS_OPEN",
                            "PRINT_SCREEN", "RIGHT_CLICK", "MULTI_MONITOR"), 1L, 2L);
            addPatternIf(patterns, attempt, "SESSION_INTEGRITY", "Vi phạm tính toàn vẹn phiên thi",
                    countSignals(typeCounts, "TAB_SWITCH", "WINDOW_BLUR", "EXIT_FULLSCREEN", "LONG_SCREEN_LEAVE"),
                    3L, 5L);
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
        recordAnalysisWarning(
                exam,
                null,
                FraudWarningCategory.ANSWER_PATTERN,
                "ANSWER_SIMILARITY",
                pair.similarity() >= highSimilarityThreshold() ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                pair.similarity(),
                "Nghi van trung mau dap an giua nhieu thi sinh",
                evidence,
                "answer_similarity",
                List.of(a1.getId(), a2.getId())
        );
    }

    private void recordSessionIntegrityWarning(
            ExamAttempt attempt,
            String type,
            long count,
            String message,
            long mediumThreshold,
            long highThreshold
    ) {
        SignalSeverity severity = severityForCount(count, mediumThreshold, highThreshold);
        recordAnalysisWarning(
                attempt.getExam(),
                attempt,
                FraudWarningCategory.SESSION_INTEGRITY,
                type,
                severity,
                Math.min(0.95, 0.55 + count * 0.05),
                message,
                Map.of(
                        "count", count,
                        "message", message,
                        "mediumThreshold", mediumThreshold,
                        "highThreshold", highThreshold
                ),
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
        long mediumThresholdSeconds = Math.max(
                Math.max(fastCompletionMediumMinSeconds, 1L),
                questionCount * Math.max(fastCompletionMediumSecondsPerQuestion, 1L)
        );
        if (durationSeconds > mediumThresholdSeconds) {
            return;
        }
        long highThresholdSeconds = Math.max(
                Math.max(fastCompletionHighMinSeconds, 1L),
                questionCount * Math.max(fastCompletionHighSecondsPerQuestion, 1L)
        );
        highThresholdSeconds = Math.min(highThresholdSeconds, mediumThresholdSeconds);
        recordAnalysisWarning(
                exam,
                attempt,
                FraudWarningCategory.TIMING_PATTERN,
                "FAST_COMPLETION",
                durationSeconds <= highThresholdSeconds ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                0.75,
                "Thời gian hoàn thành bài trắc nghiệm thấp bất thường",
                Map.of(
                        "durationSeconds", durationSeconds,
                        "questionCount", questionCount,
                        "mediumThresholdSeconds", mediumThresholdSeconds,
                        "highThresholdSeconds", highThresholdSeconds
                ),
                "timing_analysis",
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
        recordAnalysisWarning(
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

    private void addBehaviorAnomaly(
            List<String> anomalies,
            ExamAttempt attempt,
            Map<String, Long> counts,
            String signalType,
            String title,
            long mediumThreshold,
            long highThreshold,
            FraudWarningCategory category
    ) {
        long count = countSignals(counts, signalType);
        if (count < Math.max(mediumThreshold, 1L)) {
            return;
        }
        SignalSeverity severity = severityForCount(count, mediumThreshold, highThreshold);
        Map<String, Object> evidence = buildBehaviorEvidence(attempt, signalType, count, title);
        anomalies.add(formatAnomaly(attempt, signalType, severity, count));
        recordBehaviorWarning(attempt, category, signalType, severity, title, evidence);
    }

    private Map<String, Object> buildBehaviorEvidence(ExamAttempt attempt, String signalType, long count, String message) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attemptId", attempt.getId());
        evidence.put("studentUsername", attempt.getStudent() != null ? attempt.getStudent().getUsername() : null);
        evidence.put("signalType", signalType);
        evidence.put("count", count);
        evidence.put("message", message);
        evidence.put("riskScore", attempt.getRiskScore());
        evidence.put("riskLevel", attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : null);
        return evidence;
    }

    private String formatAnomaly(ExamAttempt attempt, String signalType, SignalSeverity severity, long count) {
        String username = attempt.getStudent() != null ? attempt.getStudent().getUsername() : "unknown";
        return username + " - " + signalType + " x" + count + " (" + severity.name() + ")";
    }

    private void recordBehaviorWarning(
            ExamAttempt attempt,
            FraudWarningCategory category,
            String type,
            SignalSeverity severity,
            String message,
            Map<String, Object> evidence
    ) {
        recordAnalysisWarning(
                attempt.getExam(),
                attempt,
                category,
                type,
                severity,
                confidenceForSeverity(severity),
                message,
                evidence,
                "behavior_analysis",
                List.of(attempt.getId())
        );
    }

    private Map<String, Object> buildStatisticalEvidence(
            ExamAttempt attempt,
            double mean,
            double stdDev,
            double zScore,
            Long durationSeconds,
            Integer riskScore
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attemptId", attempt.getId());
        evidence.put("studentUsername", attempt.getStudent() != null ? attempt.getStudent().getUsername() : null);
        evidence.put("score", attempt.getScore());
        evidence.put("mean", round(mean));
        evidence.put("stdDev", round(stdDev));
        evidence.put("zScore", round(zScore));
        evidence.put("durationSeconds", durationSeconds);
        evidence.put("riskScore", riskScore);
        evidence.put("riskLevel", attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : null);
        return evidence;
    }

    private void recordStatisticalWarning(
            ExamAttempt attempt,
            String type,
            SignalSeverity severity,
            String message,
            Map<String, Object> evidence
    ) {
        recordAnalysisWarning(
                attempt.getExam(),
                attempt,
                FraudWarningCategory.POST_EXAM_STATISTICAL,
                type,
                severity,
                confidenceForSeverity(severity),
                message,
                evidence,
                "statistical_analysis",
                List.of(attempt.getId())
        );
    }

    private void recordAnalysisWarning(
            Exam exam,
            ExamAttempt attempt,
            FraudWarningCategory category,
            String warningType,
            SignalSeverity severity,
            double confidence,
            String message,
            Object evidence,
            String source,
            List<Long> relatedAttemptIds
    ) {
        fraudWarningService.recordWarningWithDedupWindow(
                exam,
                attempt,
                category,
                warningType,
                severity,
                confidence,
                message,
                evidence,
                source,
                relatedAttemptIds,
                ANALYSIS_WARNING_DEDUP_WINDOW
        );
    }

    private double confidenceForSeverity(SignalSeverity severity) {
        if (severity == SignalSeverity.CRITICAL) return 0.95;
        if (severity == SignalSeverity.HIGH) return 0.85;
        if (severity == SignalSeverity.MEDIUM) return 0.72;
        return 0.6;
    }

    private double computeStdDev(List<Double> values, double mean) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double variance = values.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .sum() / values.size();
        return Math.sqrt(Math.max(variance, 0.0));
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean isCameraSignal(String type) {
        String canonical = FraudSignalTypeNormalizer.canonical(type);
        return Set.of(
                "NO_CAMERA", "NO_MIC",
                "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
                "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
                "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
                "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
                "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
                "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
                "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE", "LOW_LIVENESS",
                "FLAT_IMAGE", "SCREEN_DISPLAY", "AI_PHONE_DETECTED", "AI_SPEAKING_DETECTED"
        ).contains(canonical);
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
