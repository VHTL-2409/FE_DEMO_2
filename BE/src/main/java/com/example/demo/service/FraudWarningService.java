package com.example.demo.service;

import com.example.demo.api.dto.fraud.FraudWarningResponse;
import com.example.demo.api.dto.fraud.FraudWarningSummaryResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudWarningService {

    private final FraudWarningRepository fraudWarningRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final ObjectMapper objectMapper;
    private final RealtimeNotificationService realtimeNotificationService;

    @Value("${demo.fraud.warning.dedup-minutes:10}")
    private long dedupMinutes;

    @Transactional
    public FraudWarning recordWarning(
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
        if (exam == null) {
            throw new IllegalArgumentException("exam is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        String normalizedType = normalizeType(warningType);
        String relatedIdsJson = writeRelatedAttemptIds(relatedAttemptIds, attempt);
        LocalDateTime cutoff = VietNamTime.now().minusMinutes(Math.max(dedupMinutes, 1));
        List<FraudWarning> existing = fraudWarningRepository.findRecentSimilar(
                exam,
                attempt,
                category,
                normalizedType,
                source,
                relatedIdsJson,
                cutoff
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        FraudWarning warning = FraudWarning.builder()
                .exam(exam)
                .attempt(attempt)
                .student(attempt != null ? attempt.getStudent() : null)
                .category(category)
                .warningType(normalizedType)
                .severity(severity != null ? severity : SignalSeverity.MEDIUM)
                .confidence(normalizeConfidence(confidence))
                .message(message == null || message.isBlank() ? normalizedType : message.trim())
                .evidence(writeJson(evidence == null ? Map.of() : evidence))
                .source(source == null || source.isBlank() ? "fraud_warning" : source.trim())
                .relatedAttemptIds(relatedIdsJson)
                .reviewStatus(FraudWarningReviewStatus.NEEDS_REVIEW)
                .createdAt(VietNamTime.now())
                .updatedAt(VietNamTime.now())
                .build();
        FraudWarning saved = fraudWarningRepository.save(warning);
        realtimeNotificationService.notifyFraudWarningRecorded(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public FraudWarningSummaryResponse warningsByExam(Exam exam) {
        List<FraudWarningResponse> warnings = fraudWarningRepository.findByExamOrderByCreatedAtDesc(exam).stream()
                .map(this::toResponse)
                .toList();
        return FraudWarningSummaryResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalWarnings(warnings.size())
                .byCategory(countBy(warnings, FraudWarningResponse::getCategory))
                .bySeverity(countBy(warnings, FraudWarningResponse::getSeverity))
                .byReviewStatus(countBy(warnings, FraudWarningResponse::getReviewStatus))
                .warnings(warnings)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FraudWarningResponse> warningsByAttempt(ExamAttempt attempt) {
        return fraudWarningRepository.findByAttemptOrRelatedAttemptId(attempt.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FraudWarningResponse reviewWarning(Long warningId, FraudWarningReviewStatus reviewStatus, String reviewNote, User actor) {
        FraudWarning warning = fraudWarningRepository.findById(warningId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Warning not found: " + warningId));
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to review this warning");
        }
        boolean isAdmin = actor.getRoles() != null && actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isOwner = warning.getExam() != null
                && warning.getExam().getCreatedBy() != null
                && warning.getExam().getCreatedBy().getId() != null
                && warning.getExam().getCreatedBy().getId().equals(actor.getId());
        if (!isAdmin && !isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to review this warning");
        }
        warning.setReviewStatus(reviewStatus == null ? FraudWarningReviewStatus.NEEDS_REVIEW : reviewStatus);
        warning.setReviewNote(reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim());
        warning.setReviewedBy(actor);
        warning.setReviewedAt(VietNamTime.now());
        return toResponse(fraudWarningRepository.save(warning));
    }

    @Transactional
    public int recordExistingSignalsAsWarningsForExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        int recorded = 0;
        for (ExamAttempt attempt : examAttemptRepository.findByExam(exam)) {
            for (FraudSignal signal : fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)) {
                if (recordFromFraudSignal(signal).isPresent()) {
                    recorded++;
                }
            }
        }
        return recorded;
    }

    @Transactional
    public Optional<FraudWarning> recordFromFraudSignal(FraudSignal signal) {
        FraudWarningCategory category = mapSignalCategory(signal.getSignalType(), signal.getCategory());
        if (category == null) {
            return Optional.empty();
        }
        ExamAttempt attempt = signal.getAttempt();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceSignalId", signal.getId());
        evidence.put("signalType", signal.getSignalType());
        evidence.put("signalCategory", signal.getCategory());
        evidence.put("riskImpactIgnored", true);
        evidence.put("rawEvidence", parseJsonOrRaw(signal.getEvidence()));
        FraudWarning warning = recordWarning(
                attempt.getExam(),
                attempt,
                category,
                signal.getSignalType(),
                signal.getSeverity(),
                signal.getConfidence() != null ? signal.getConfidence() : 0.7,
                signal.getDisplayMessage(),
                evidence,
                "existing_fraud_signal",
                List.of(attempt.getId())
        );
        return Optional.of(warning);
    }

    public FraudWarningResponse toResponse(FraudWarning warning) {
        User student = warning.getStudent();
        return FraudWarningResponse.builder()
                .id(warning.getId())
                .examId(warning.getExam() != null ? warning.getExam().getId() : null)
                .examTitle(warning.getExam() != null ? warning.getExam().getTitle() : null)
                .attemptId(warning.getAttempt() != null ? warning.getAttempt().getId() : null)
                .studentId(student != null ? student.getId() : null)
                .studentUsername(student != null ? student.getUsername() : null)
                .studentName(resolveDisplayName(student))
                .category(warning.getCategory() != null ? warning.getCategory().name() : null)
                .type(warning.getWarningType())
                .severity(warning.getSeverity() != null ? warning.getSeverity().name() : null)
                .confidence(warning.getConfidence())
                .message(warning.getMessage())
                .evidence(parseJsonOrRaw(warning.getEvidence()))
                .source(warning.getSource())
                .relatedAttemptIds(readRelatedAttemptIds(warning.getRelatedAttemptIds()))
                .reviewStatus(warning.getReviewStatus() != null ? warning.getReviewStatus().name() : null)
                .reviewNote(warning.getReviewNote())
                .reviewedBy(warning.getReviewedBy() != null ? warning.getReviewedBy().getUsername() : null)
                .reviewedAt(warning.getReviewedAt())
                .createdAt(warning.getCreatedAt())
                .build();
    }

    private FraudWarningCategory mapSignalCategory(String signalType, String signalCategory) {
        String type = normalizeType(signalType);
        if (Set.of("ANSWER_SIMILARITY", "EXACT_ANSWER_MATCH", "ANSWER_CHANGE_BURST").contains(type)) {
            return FraudWarningCategory.ANSWER_PATTERN;
        }
        if (Set.of("SYNC_BEHAVIOR").contains(type)) {
            return FraudWarningCategory.SYNCHRONIZATION;
        }
        if (Set.of("TIMING_ANOMALY", "FAST_SUBMIT", "IMPOSSIBLE_SPEED", "IMPOSSIBLE_EXAM_SPEED", "FAST_ANSWER", "QUESTION_TIMING_ANOMALY", "RAPID_QUESTION_SWITCH").contains(type)) {
            return FraudWarningCategory.TIMING_PATTERN;
        }
        if (Set.of("DUPLICATE_IP", "IP_FINGERPRINT_GRAPH", "IP_ANOMALY", "DEVICE_FINGERPRINT_CHANGED", "MULTIPLE_DEVICE_SESSION", "IP_CHANGED").contains(type)) {
            return FraudWarningCategory.IDENTITY_NETWORK;
        }
        if (Set.of("TAB_SWITCH", "WINDOW_BLUR", "EXIT_FULLSCREEN", "FULLSCREEN_VIOLATION", "FULLSCREEN_EVASION", "HEARTBEAT_STALE", "NETWORK_INSTABILITY", "SESSION_RECOVERY").contains(type)) {
            return FraudWarningCategory.SESSION_INTEGRITY;
        }
        if ("AI_CAMERA".equalsIgnoreCase(signalCategory) || isCameraSignal(type)) {
            return FraudWarningCategory.CAMERA_PROCTORING;
        }
        return null;
    }

    private boolean isCameraSignal(String type) {
        return Set.of(
                "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
                "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
                "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
                "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
                "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
                "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
                "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE",
                "FLAT_IMAGE", "SCREEN_DISPLAY", "AI_MULTIPLE_FACES", "AI_FACE_MISSING",
                "AI_PHONE_DETECTED", "AI_LOOKING_AWAY", "AI_SPEAKING_DETECTED"
        ).contains(type);
    }

    private String writeRelatedAttemptIds(List<Long> relatedAttemptIds, ExamAttempt attempt) {
        List<String> ids = new ArrayList<>();
        if (relatedAttemptIds != null) {
            relatedAttemptIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .distinct()
                    .sorted()
                    .forEach(ids::add);
        }
        if (ids.isEmpty() && attempt != null && attempt.getId() != null) {
            ids.add(String.valueOf(attempt.getId()));
        }
        return ids.isEmpty() ? null : writeJson(ids);
    }

    private List<Long> readRelatedAttemptIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(raw, new TypeReference<>() {});
            return values.stream()
                    .map(value -> {
                        try {
                            return Long.valueOf(value);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Object parseJsonOrRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception ex) {
            return raw;
        }
    }

    private Map<String, Long> countBy(List<FraudWarningResponse> warnings, java.util.function.Function<FraudWarningResponse, String> keyFn) {
        return warnings.stream()
                .map(keyFn)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(k -> k, LinkedHashMap::new, Collectors.counting()));
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getUsername();
    }

    private String normalizeType(String type) {
        return type == null || type.isBlank() ? "UNKNOWN_WARNING" : type.trim().toUpperCase(Locale.ROOT);
    }

    private double normalizeConfidence(double confidence) {
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            return 0.6;
        }
        return Math.max(0.1, Math.min(1.0, confidence));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fraud warning payload", ex);
        }
    }
}
