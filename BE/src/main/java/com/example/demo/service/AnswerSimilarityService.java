package com.example.demo.service;

import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AnswerSimilarityService {

    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final FraudWarningService fraudWarningService;

    @Value("${demo.anti-cheat.answer-similarity-threshold:0.85}")
    private double similarityThreshold;

    

    public List<SimilarityPair> findSuspiciousPairs(Exam exam) {
        List<ExamAttempt> submitted = loadSubmittedAttempts(exam);
        if (submitted.size() < 2) {
            return List.of();
        }

        Map<Long, Map<Long, String>> attemptAnswers = loadAttemptAnswers(submitted);

        Map<String, List<ExamAttempt>> ipGroups = buildBlockingGroups(submitted, ExamAttempt::getClientIp);
        Map<String, List<ExamAttempt>> fpGroups = buildBlockingGroups(submitted, ExamAttempt::getDeviceFingerprint);

        Set<String> processedPairs = new HashSet<>();
        List<SimilarityPair> pairs = new ArrayList<>();

        Set<String> allKeys = new HashSet<>(ipGroups.keySet());
        fpGroups.keySet().forEach(k -> { if (k != null && !k.isBlank()) allKeys.add(k); });

        for (String groupKey : allKeys) {
            List<ExamAttempt> group = new ArrayList<>();
            if (ipGroups.containsKey(groupKey)) group.addAll(ipGroups.get(groupKey));
            if (fpGroups.containsKey(groupKey)) {
                for (ExamAttempt fpAttempt : fpGroups.get(groupKey)) {
                    if (group.stream().noneMatch(a -> a.getId().equals(fpAttempt.getId()))) {
                        group.add(fpAttempt);
                    }
                }
            }
            if (group.size() < 2) continue;

            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    ExamAttempt a1 = group.get(i);
                    ExamAttempt a2 = group.get(j);
                    String pairKey = pairKey(a1.getId(), a2.getId());
                    if (processedPairs.contains(pairKey)) continue;
                    processedPairs.add(pairKey);

                    SimilarityPair pair = computeSimilarity(a1, a2, attemptAnswers);
                    if (pair != null) {
                        pairs.add(pair);
                    }
                }
            }
        }

        if (pairs.isEmpty() && submitted.size() <= 50) {
            return fallbackFullComparison(submitted, attemptAnswers);
        }

        return pairs.stream()
                .sorted(Comparator.comparingDouble(SimilarityPair::similarity).reversed())
                .toList();
    }

    

    private List<SimilarityPair> fallbackFullComparison(
            List<ExamAttempt> submitted,
            Map<Long, Map<Long, String>> attemptAnswers
    ) {
        List<SimilarityPair> pairs = new ArrayList<>();
        for (int i = 0; i < submitted.size(); i++) {
            for (int j = i + 1; j < submitted.size(); j++) {
                SimilarityPair pair = computeSimilarity(submitted.get(i), submitted.get(j), attemptAnswers);
                if (pair != null) {
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

    private Map<Long, Map<Long, String>> loadAttemptAnswers(List<ExamAttempt> attempts) {
        Map<Long, Map<Long, String>> attemptAnswers = new HashMap<>();
        for (ExamAttempt a : attempts) {
            Map<Long, String> qToAnswer = answerRepository.findByAttempt(a).stream()
                    .collect(Collectors.toMap(
                            ans -> ans.getQuestion().getId(),
                            ans -> ans.getSelectedAnswer(),
                            (x, y) -> x));
            attemptAnswers.put(a.getId(), qToAnswer);
        }
        return attemptAnswers;
    }

    private Map<String, List<ExamAttempt>> buildBlockingGroups(
            List<ExamAttempt> attempts,
            java.util.function.Function<ExamAttempt, String> keyExtractor
    ) {
        Map<String, List<ExamAttempt>> groups = new HashMap<>();
        for (ExamAttempt attempt : attempts) {
            String key = keyExtractor.apply(attempt);
            if (key == null || key.isBlank()) continue;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(attempt);
        }
        return groups;
    }

    private SimilarityPair computeSimilarity(
            ExamAttempt a1,
            ExamAttempt a2,
            Map<Long, Map<Long, String>> attemptAnswers
    ) {
        Map<Long, String> m1 = attemptAnswers.get(a1.getId());
        Map<Long, String> m2 = attemptAnswers.get(a2.getId());
        if (m1 == null || m2 == null || m1.isEmpty() || m2.isEmpty()) {
            return null;
        }

        int size1 = m1.size();
        int size2 = m2.size();

        int maxCommon = Math.min(size1, size2);
        double maxPossibleSimilarity = (double) maxCommon / Math.max(size1, size2);
        if (maxPossibleSimilarity < similarityThreshold) {
            return null;
        }

        Set<Long> commonQuestions = new HashSet<>(m1.keySet());
        commonQuestions.retainAll(m2.keySet());
        if (commonQuestions.isEmpty()) {
            return null;
        }

        long sameCount = commonQuestions.stream()
                .filter(q -> Objects.equals(m1.get(q), m2.get(q)))
                .count();
        double similarity = (double) sameCount / commonQuestions.size();

        if (similarity >= similarityThreshold && commonQuestions.size() >= 5) {
            return new SimilarityPair(
                    a1.getStudent().getUsername(),
                    a2.getStudent().getUsername(),
                    similarity,
                    commonQuestions.size(),
                    (int) sameCount
            );
        }
        return null;
    }

    private String pairKey(Long id1, Long id2) {
        return id1 < id2 ? id1 + "-" + id2 : id2 + "-" + id1;
    }

    @Transactional
    public int recordSignalsForExam(Exam exam) {
        if (!Boolean.TRUE.equals(exam.getMonitorAnswerSimilarity())) {
            return 0;
        }
        List<SimilarityPair> pairs = findSuspiciousPairs(exam);
        if (pairs.isEmpty()) {
            return 0;
        }

        List<ExamAttempt> submittedAttempts = loadSubmittedAttempts(exam);
        Map<String, ExamAttempt> attemptsByStudent = submittedAttempts.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUsername(), a -> a, (left, right) -> left));

        int recorded = 0;
        for (SimilarityPair pair : pairs) {
            ExamAttempt attempt1 = attemptsByStudent.get(pair.student1());
            ExamAttempt attempt2 = attemptsByStudent.get(pair.student2());
            if (attempt1 == null || attempt2 == null) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "answer_similarity");
            evidence.put("peerStudent", pair.student2());
            evidence.put("similarity", pair.similarity());
            evidence.put("commonQuestions", pair.commonQuestions());
            evidence.put("sameAnswers", pair.sameAnswers());
            fraudWarningService.recordWarning(
                    exam,
                    null,
                    FraudWarningCategory.ANSWER_PATTERN,
                    "ANSWER_SIMILARITY",
                    pair.similarity() >= 0.95 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM,
                    pair.similarity(),
                    "Nghi van trung mau dap an giua 2 thi sinh",
                    evidence,
                    "answer_similarity",
                    List.of(attempt1.getId(), attempt2.getId())
            );
            recorded++;
        }
        return recorded;
    }

    private List<ExamAttempt> loadSubmittedAttempts(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime()).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .toList();
        }
        return examAttemptRepository.findByExam(exam).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                .toList();
    }

    public record SimilarityPair(
            String student1,
            String student2,
            double similarity,
            int commonQuestions,
            int sameAnswers
    ) {}
}
