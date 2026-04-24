package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Phat hien gian lan bang cach phan tich do tuong dong cau tra loi
 * giua cac hoc sinh trong cung mot bai thi.
 *
 * Ky thuat:
 * - Jaccard similarity tren n-gram (word-level, character-level)
 * - Levenshtein normalized distance
 * - Longest Common Subsequence ratio
 * - Exact match detection
 * - Time correlation analysis
 *
 * Cac loai gian lan phat hien:
 * - COPY_PASTE: Tra loi giong nhau chinh xac
 * - SUSPICIOUS_SIMILARITY: Tra loi giong nhau nhung khong chinh xac
 * - COLLABORATION: Nhieu hoc sinh tra loi giong nhau cung thoi gian
 * - ANSWER_TEMPLATE: Chung mot bieu mau tra loi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerPlagiarismService {

    private final AnswerRepository answerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;

    /** So n-gram toi thieu de tinh similarity */
    @Value("${demo.plagiarism.min-ngram:3}")
    private int minNgram;

    /** Nguong similarity de danh dau SUSPICIOUS_SIMILARITY */
    @Value("${demo.plagiarism.similarity-threshold:0.75}")
    private double similarityThreshold;

    /** Nguong similarity de danh dau EXACT_MATCH */
    @Value("${demo.plagiarism.exact-match-threshold:0.98}")
    private double exactMatchThreshold;

    /** So cau hoi chung toi thieu de kiem tra */
    @Value("${demo.plagiarism.min-common-questions:3}")
    private int minCommonQuestions;

    /** Do dai toi thieu cau tra loi de kiem tra (tu) */
    @Value("${demo.plagiarism.min-answer-length:20}")
    private int minAnswerLength;

    /** Cho phep kiem tra offline hay khong */
    @Value("${demo.plagiarism.check-on-submit:true}")
    private boolean checkOnSubmit;

    /**
     * Kiem tra gian lan cho tat ca hoc sinh trong mot bai thi.
     * Goi khi bai thi ket thuc hoac khi giao vien yeu cau.
     */
    @Transactional
    public List<PlagiarismReport> analyzeExam(Long examId) {
        log.info("Starting plagiarism analysis for exam: {}", examId);
        List<PlagiarismReport> reports = new ArrayList<>();

        List<ExamAttempt> submittedAttempts = examAttemptRepository
                .findByExamIdAndStatus(examId, AttemptStatus.SUBMITTED);

        if (submittedAttempts.size() < 2) {
            log.info("Not enough submitted attempts ({}), skipping plagiarism check", submittedAttempts.size());
            return reports;
        }

        List<PlagiarismPair> allPairs = findPlagiarismPairs(submittedAttempts);

        for (PlagiarismPair pair : allPairs) {
            FraudSignal signal = recordPlagiarismSignal(pair);
            reports.add(buildReport(pair, signal));
        }

        log.info("Plagiarism analysis complete: {} suspicious pairs out of {} total pairs",
                reports.size(), totalPairs(submittedAttempts.size()));

        return reports;
    }

    /**
     * Kiem tra gian lan chi cho mot hoc sinh cu the.
     */
    @Transactional
    public List<PlagiarismReport> analyzeStudent(Long attemptId) {
        ExamAttempt target = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<ExamAttempt> competitors = examAttemptRepository
                .findByExamIdAndStatus(target.getExam().getId(), AttemptStatus.SUBMITTED)
                .stream()
                .filter(a -> !a.getId().equals(attemptId))
                .toList();

        List<PlagiarismReport> reports = new ArrayList<>();

        for (ExamAttempt competitor : competitors) {
            PlagiarismPair pair = compareAttempts(target, competitor);
            if (pair.similarityScore() >= similarityThreshold
                    && pair.commonQuestions() >= minCommonQuestions) {
                FraudSignal signal = recordPlagiarismSignal(pair);
                reports.add(buildReport(pair, signal));
            }
        }

        return reports;
    }

    /**
     * So sanh tat ca cac cap hoc sinh trong bai thi.
     */
    private List<PlagiarismPair> findPlagiarismPairs(List<ExamAttempt> attempts) {
        List<PlagiarismPair> suspiciousPairs = new ArrayList<>();

        for (int i = 0; i < attempts.size(); i++) {
            for (int j = i + 1; j < attempts.size(); j++) {
                PlagiarismPair pair = compareAttempts(attempts.get(i), attempts.get(j));

                if (pair.similarityScore() >= similarityThreshold
                        && pair.commonQuestions() >= minCommonQuestions) {
                    suspiciousPairs.add(pair);
                    log.debug("Suspicious pair found: {} vs {} - similarity={}, common={}",
                            pair.student1Name(), pair.student2Name(),
                            pair.similarityScore(), pair.commonQuestions());
                }
            }
        }

        suspiciousPairs.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
        return suspiciousPairs;
    }

    /**
     * So sanh hai lan thi.
     */
    private PlagiarismPair compareAttempts(ExamAttempt a1, ExamAttempt a2) {
        List<Answer> answers1 = answerRepository.findByAttemptId(a1.getId());
        List<Answer> answers2 = answerRepository.findByAttemptId(a2.getId());

        Map<Long, Answer> map1 = toQuestionMap(answers1);
        Map<Long, Answer> map2 = toQuestionMap(answers2);

        Set<Long> commonQuestions = new HashSet<>(map1.keySet());
        commonQuestions.retainAll(map2.keySet());

        if (commonQuestions.size() < minCommonQuestions) {
            return new PlagiarismPair(
                    a1.getId(), a1.getStudent().getId(), a1.getStudent().getUsername(),
                    a2.getId(), a2.getStudent().getId(), a2.getStudent().getUsername(),
                    0.0, 0, Collections.emptyList(), Collections.emptyMap(), null
            );
        }

        List<QuestionSimilarity> questionSimilarities = new ArrayList<>();
        Map<Long, Double> perQuestionScores = new LinkedHashMap<>();
        int totalEssayQuestions = 0;
        double totalSimilarity = 0.0;

        for (Long questionId : commonQuestions) {
            Answer ans1 = map1.get(questionId);
            Answer ans2 = map2.get(questionId);

            if (ans1 == null || ans2 == null) continue;
            if (isEmpty(ans1.getEssayContent()) || isEmpty(ans2.getEssayContent())) continue;

            ExamQuestion question = examQuestionRepository.findById(questionId).orElse(null);
            if (question == null || !"ESSAY".equalsIgnoreCase(question.getQuestionType())) continue;

            totalEssayQuestions++;

            double similarity = computeSimilarity(
                    ans1.getEssayContent(), ans2.getEssayContent());

            perQuestionScores.put(questionId, similarity);

            questionSimilarities.add(new QuestionSimilarity(
                    questionId,
                    question.getContent(),
                    ans1.getEssayContent(),
                    ans2.getEssayContent(),
                    similarity,
                    classifySimilarity(similarity)
            ));

            totalSimilarity += similarity;
        }

        double avgSimilarity = totalEssayQuestions > 0
                ? totalSimilarity / totalEssayQuestions
                : 0.0;

        boolean timeCorrelated = analyzeTimeCorrelation(a1, a2);

        return new PlagiarismPair(
                a1.getId(), a1.getStudent().getId(), a1.getStudent().getUsername(),
                a2.getId(), a2.getStudent().getId(), a2.getStudent().getUsername(),
                avgSimilarity,
                commonQuestions.size(),
                questionSimilarities,
                perQuestionScores,
                timeCorrelated ? "HIGH_TIME_CORRELATION" : null
        );
    }

    /**
     * Tinh do tuong dong giua hai cau tra loi.
     * Su dung nhieu ky thuat ket hop.
     */
    private double computeSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.isBlank() || text2.isBlank()) return 0.0;

        String t1 = normalizeText(text1);
        String t2 = normalizeText(text2);

        if (t1.length() < minAnswerLength && t2.length() < minAnswerLength) {
            return exactStringMatch(t1, t2) ? 1.0 : 0.0;
        }

        double wordNgramSim = wordNgramSimilarity(t1, t2, 2);
        double charNgramSim = charNgramSimilarity(t1, t2, 4);
        double levenshteinSim = 1.0 - normalizedLevenshtein(t1, t2);
        double lcsRatio = lcsRatio(t1, t2);
        double exactRatio = exactMatchRatio(t1, t2);

        double weightedSim =
                wordNgramSim * 0.30 +
                charNgramSim * 0.25 +
                levenshteinSim * 0.20 +
                lcsRatio * 0.15 +
                exactRatio * 0.10;

        return Math.max(0.0, Math.min(1.0, weightedSim));
    }

    /** Jaccard similarity tren word-level n-gram */
    private double wordNgramSimilarity(String text1, String text2, int n) {
        Set<String> ngrams1 = getWordNgrams(text1, n);
        Set<String> ngrams2 = getWordNgrams(text2, n);

        if (ngrams1.isEmpty() && ngrams2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /** Jaccard similarity tren character-level n-gram */
    private double charNgramSimilarity(String text1, String text2, int n) {
        Set<String> ngrams1 = getCharNgrams(text1, n);
        Set<String> ngrams2 = getCharNgrams(text2, n);

        if (ngrams1.isEmpty() && ngrams2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /** Word n-gram extraction */
    private Set<String> getWordNgrams(String text, int n) {
        String[] words = text.toLowerCase().split("\\s+");
        Set<String> ngrams = new HashSet<>();
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(' ');
                sb.append(words[i + j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    /** Character n-gram extraction */
    private Set<String> getCharNgrams(String text, int n) {
        text = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        Set<String> ngrams = new HashSet<>();
        for (int i = 0; i <= text.length() - n; i++) {
            ngrams.add(text.substring(i, i + n));
        }
        return ngrams;
    }

    /** Normalized Levenshtein distance: 0 = giong nhau, 1 = khac nhau hoan toan */
    private double normalizedLevenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 0.0 : (double) dp[s1.length()][s2.length()] / maxLen;
    }

    /** Longest Common Subsequence ratio */
    private double lcsRatio(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        int lcsLen = dp[m][n];
        int maxLen = Math.max(m, n);
        return maxLen == 0 ? 0.0 : (double) lcsLen / maxLen;
    }

    /** Exact substring match ratio */
    private double exactMatchRatio(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.length() < 10 || s2.length() < 10) return 0.0;

        int maxMatch = 0;
        String shorter = s1.length() <= s2.length() ? s1 : s2;
        String longer = s1.length() <= s2.length() ? s2 : s1;

        for (int len = shorter.length(); len >= 10; len--) {
            for (int i = 0; i <= shorter.length() - len; i++) {
                String sub = shorter.substring(i, i + len);
                if (longer.contains(sub)) {
                    maxMatch = Math.max(maxMatch, len);
                    break;
                }
            }
            if (maxMatch == len) break;
        }

        return (double) maxMatch / Math.max(shorter.length(), 1);
    }

    /** Kiem tra exact string equality */
    private boolean exactStringMatch(String s1, String s2) {
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    /** Phan tich tuong quan thoi gian giua hai attempt */
    private boolean analyzeTimeCorrelation(ExamAttempt a1, ExamAttempt a2) {
        if (a1.getStartedAt() == null || a2.getStartedAt() == null) {
            return false;
        }

        long startDiff = Math.abs(
                a1.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) -
                a2.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC)
        );

        // Neu bat dau trong vong 60 giay -> HIGH_TIME_CORRELATION
        return startDiff <= 60;
    }

    /** Phan loai muc do tuong dong */
    private String classifySimilarity(double similarity) {
        if (similarity >= exactMatchThreshold) return "EXACT_MATCH";
        if (similarity >= 0.90) return "NEAR_EXACT";
        if (similarity >= 0.75) return "HIGH_SIMILARITY";
        if (similarity >= 0.50) return "MODERATE_SIMILARITY";
        return "LOW_SIMILARITY";
    }

    /** Ghi nhan plagiarism signal */
    private FraudSignal recordPlagiarismSignal(PlagiarismPair pair) {
        String signalType = pair.similarityScore() >= exactMatchThreshold
                ? "EXACT_ANSWER_MATCH"
                : "ANSWER_SIMILARITY";

        SignalSeverity severity = pair.similarityScore() >= 0.95
                ? SignalSeverity.CRITICAL
                : pair.similarityScore() >= 0.85
                        ? SignalSeverity.HIGH
                        : SignalSeverity.MEDIUM;

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "plagiarism_check");
        evidence.put("peerAttemptId", pair.attempt2Id());
        evidence.put("peerStudent", pair.student2Name());
        evidence.put("similarityScore", Math.round(pair.similarityScore() * 100.0) / 100.0);
        evidence.put("commonQuestions", pair.commonQuestions());
        evidence.put("timeCorrelated", pair.timeCorrelation() != null);
        evidence.put("perQuestionScores", pair.perQuestionScores());
        evidence.put("method", "ngram_jaccard+levenshtein+lcs");

        ExamAttempt owner = examAttemptRepository.findById(pair.attempt1Id()).orElse(null);
        if (owner == null) return null;

        return fraudSignalService.recordServerSignal(
                owner, signalType, severity, pair.similarityScore(), evidence
        );
    }

    /** Xay dung bao cao plagiarism */
    private PlagiarismReport buildReport(PlagiarismPair pair, FraudSignal signal) {
        String verdict;
        String recommendation;

        if (pair.similarityScore() >= 0.95) {
            verdict = "NGHIEM_TRONG";
            recommendation = "CANH_bao_ngay + Ho_so_kiem_tra_chuyen_sau";
        } else if (pair.similarityScore() >= 0.85) {
            verdict = "CAO";
            recommendation = "Xac_minh_thu_cong + Yeu_cau_giai_trinh";
        } else {
            verdict = "TRUNG_BINH";
            recommendation = "Danh_dau_theo_doi";
        }

        return new PlagiarismReport(
                pair.attempt1Id(),
                pair.student1Name(),
                pair.attempt2Id(),
                pair.student2Name(),
                pair.similarityScore(),
                pair.commonQuestions(),
                verdict,
                recommendation,
                pair.questionSimilarities(),
                pair.timeCorrelation(),
                signal != null ? signal.getId() : null
        );
    }

    private Map<Long, Answer> toQuestionMap(List<Answer> answers) {
        Map<Long, Answer> map = new LinkedHashMap<>();
        for (Answer a : answers) {
            if (a.getQuestion() != null) {
                map.put(a.getQuestion().getId(), a);
            }
        }
        return map;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim();
    }

    private int totalPairs(int n) {
        return n * (n - 1) / 2;
    }

    // ========== Record Classes ==========

    public record PlagiarismPair(
            Long attempt1Id,
            Long student1Id,
            String student1Name,
            Long attempt2Id,
            Long student2Id,
            String student2Name,
            double similarityScore,
            int commonQuestions,
            List<QuestionSimilarity> questionSimilarities,
            Map<Long, Double> perQuestionScores,
            String timeCorrelation
    ) {}

    public record QuestionSimilarity(
            Long questionId,
            String questionContent,
            String answer1,
            String answer2,
            double similarityScore,
            String classification
    ) {}

    public record PlagiarismReport(
            Long attempt1Id,
            String student1Name,
            Long attempt2Id,
            String student2Name,
            double similarityScore,
            int commonQuestions,
            String verdict,
            String recommendation,
            List<QuestionSimilarity> questionSimilarities,
            String timeCorrelation,
            Long fraudSignalId
    ) {}
}
