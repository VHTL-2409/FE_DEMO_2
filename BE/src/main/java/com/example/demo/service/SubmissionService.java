package com.example.demo.service;

import com.example.demo.api.dto.submission.AnswerInput;
import com.example.demo.api.dto.submission.AttemptDetailResponse;
import com.example.demo.api.dto.submission.AttemptEntryStatusResponse;
import com.example.demo.api.dto.submission.AttemptFilterResponse;
import com.example.demo.api.dto.submission.AttemptReportAnswerItem;
import com.example.demo.api.dto.submission.AttemptReportResponse;
import com.example.demo.api.dto.submission.AttemptSummaryResponse;
import com.example.demo.api.dto.submission.DraftAnswerItem;
import com.example.demo.api.dto.submission.DraftAnswersResponse;
import com.example.demo.api.dto.submission.DraftSaveResponse;
import com.example.demo.api.dto.submission.StartAttemptResponse;
import com.example.demo.api.dto.submission.SubmitAttemptRequest;
import com.example.demo.api.dto.submission.SubmitAttemptResponse;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.DateTimeUtils;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.AutoPausedBy;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.StudentIdentityCheck;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.StudentIdentityCheckRepository;
import com.example.demo.service.helper.SubmissionHelper;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final ExamAttemptRepository examAttemptRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final SubmissionHelper submissionHelper;
    private final ExamService examService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final DuplicateIpDetectionService duplicateIpDetectionService;
    private final AuditLogService auditLogService;
    private final MonitoringService monitoringService;
    private final QuestionService questionService;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudWarningRepository fraudWarningRepository;
    private final StudentIdentityCheckRepository studentIdentityCheckRepository;

    @Value("${demo.monitoring.integrity-check.cooldown-seconds:45}")
    private long integrityCheckCooldownSeconds;

    @Transactional
    public StartAttemptResponse prepareAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = VietNamTime.now();
        examService.validateExamJoinable(exam, nowInExamZone);

        ExamAttempt existing = examAttemptRepository
                .findFirstByExamAndStudentAndStatusInOrderByIdDesc(
                        exam,
                        student,
                        List.of(AttemptStatus.WAITING, AttemptStatus.IN_PROGRESS, AttemptStatus.PAUSED))
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() != AttemptStatus.WAITING) {
                enforceIpConsistency(existing, normalizeClientIp(clientIp));
            }
            publishAttemptWaitingAfterCommit(existing);
            return toStartResponse(existing);
        }

        String normalizedIp = normalizeClientIp(clientIp);
        LocalDateTime joinedAt = VietNamTime.now();
        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
                        .joinedAt(joinedAt)
                        .status(AttemptStatus.WAITING)
                        .score(0.0)
                        .riskScore(0)
                        .riskLevel(RiskLevel.CLEAN)
                        .suspicious(false)
                        .clientIp(normalizedIp)
                        .sessionTokenVersion(1)
                        .fullscreenRequired(true)
                        .build());

        publishAttemptWaitingAfterCommit(saved);
        return toStartResponse(saved);
    }

    @Transactional
    public StartAttemptResponse startAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = VietNamTime.now();
        examService.validateExamAvailability(exam, nowInExamZone);

        ExamAttempt waiting = examAttemptRepository
                .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.WAITING)
                .orElse(null);
        if (waiting != null) {
            String normalizedIp = normalizeClientIp(clientIp);
            if (waiting.getClientIp() == null || waiting.getClientIp().isBlank()) {
                waiting.setClientIp(normalizedIp);
            } else {
                enforceIpConsistency(waiting, normalizedIp);
            }
            LocalDateTime startedAt = VietNamTime.now();
            waiting.setStartedAt(startedAt);
            if (waiting.getJoinedAt() == null) {
                waiting.setJoinedAt(startedAt);
            }
            waiting.setStatus(AttemptStatus.IN_PROGRESS);
            waiting.setSessionTokenVersion(waiting.getSessionTokenVersion() == null ? 1 : waiting.getSessionTokenVersion());
            waiting.setFullscreenRequired(waiting.getFullscreenRequired() == null || waiting.getFullscreenRequired());
            waiting.setLastHeartbeatAt(startedAt);
            waiting.setLastIntegrityCheckAt(startedAt);
            enforceEntryGate(waiting);
            ExamAttempt saved = examAttemptRepository.save(waiting);
            duplicateIpDetectionService.detect(saved);
            monitoringService.recordAttemptHistoryEvent(
                    saved,
                    MonitoringEventType.ATTEMPT_START,
                    buildAttemptStartDetails(saved, normalizedIp));
            publishAttemptStartedAfterCommit(saved);
            return toStartResponse(saved);
        }

        ExamAttempt existing = examAttemptRepository
                .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (existing == null) {
            existing = examAttemptRepository
                    .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.PAUSED)
                    .orElse(null);
        }
        if (existing != null) {
            enforceIpConsistency(existing, normalizeClientIp(clientIp));
            enforceEntryGate(existing);
            publishAttemptJoinedAfterCommit(existing);
            return toStartResponse(existing);
        }

        String normalizedIp = normalizeClientIp(clientIp);

        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
                        .joinedAt(VietNamTime.now())
                        .startedAt(VietNamTime.now())
                        .status(AttemptStatus.IN_PROGRESS)
                        .score(0.0)
                        .riskScore(0)
                        .riskLevel(RiskLevel.CLEAN)
                        .suspicious(false)
                        .clientIp(normalizedIp)
                        .sessionTokenVersion(1)
                        .fullscreenRequired(true)
                        .lastHeartbeatAt(VietNamTime.now())
                        .lastIntegrityCheckAt(VietNamTime.now())
                        .build());
        enforceEntryGate(saved);

        duplicateIpDetectionService.detect(saved);
        monitoringService.recordAttemptHistoryEvent(
                saved,
                MonitoringEventType.ATTEMPT_START,
                buildAttemptStartDetails(saved, normalizedIp));
        publishAttemptStartedAfterCommit(saved);

        return toStartResponse(saved);
    }

    @Transactional(readOnly = true)
    public AttemptEntryStatusResponse getEntryStatus(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);
        return buildEntryStatus(attempt);
    }

    @Transactional
    public AttemptEntryStatusResponse recordRulesAgreement(Long attemptId, User actor, String clientIp, String userAgent) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanEditAttempt(attempt, actor);
        if (attempt.getStatus() != AttemptStatus.WAITING && attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not waiting for exam entry");
        }
        attempt.setRulesAgreedAt(VietNamTime.now());
        attempt.setRulesVersionAgreed(resolveRulesVersion(attempt.getExam()));
        attempt.setRulesAgreementIp(normalizeClientIp(clientIp));
        attempt.setRulesAgreementUserAgent(truncate(userAgent, 512));
        examAttemptRepository.save(attempt);
        monitoringService.recordAttemptHistoryEvent(
                attempt,
                MonitoringEventType.RULES_AGREEMENT,
                "rulesVersion=" + safe(attempt.getRulesVersionAgreed()) + ";ip=" + safe(attempt.getRulesAgreementIp()));
        return buildEntryStatus(attempt);
    }

    private void enforceEntryGate(ExamAttempt attempt) {
        AttemptEntryStatusResponse status = buildEntryStatus(attempt);
        if (Boolean.TRUE.equals(status.getCanStart())) {
            if ("NEEDS_REVIEW".equalsIgnoreCase(status.getIdentityStatus())) {
                monitoringService.recordAttemptHistoryEvent(
                        attempt,
                        MonitoringEventType.IDENTITY_REVIEW_REQUIRED,
                        "identityStatus=NEEDS_REVIEW;policy=" + safe(status.getIdentityReviewPolicy()));
            }
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Exam entry requirements are incomplete: " + String.join(", ", status.getBlockedReasons()));
    }

    private AttemptEntryStatusResponse buildEntryStatus(ExamAttempt attempt) {
        Exam exam = attempt.getExam();
        boolean rulesRequired = exam.getRequireRulesAgreement() == null || Boolean.TRUE.equals(exam.getRequireRulesAgreement());
        boolean rulesAccepted = !rulesRequired || attempt.getRulesAgreedAt() != null;
        boolean cameraRequired = Boolean.TRUE.equals(exam.getRequireCameraMic()) || Boolean.TRUE.equals(exam.getEnableAiProctoring());
        boolean cameraReady = !cameraRequired || Boolean.TRUE.equals(attempt.getCameraOn());
        boolean micReady = !cameraRequired || Boolean.TRUE.equals(attempt.getMicOn()) || Boolean.TRUE.equals(attempt.getCameraOn());
        boolean identityRequired = resolveRequireIdentityVerification(exam);
        StudentIdentityCheck latestIdentity = latestIdentityCheck(attempt.getId());
        String identityStatus = latestIdentity == null ? "NOT_CHECKED" : safe(latestIdentity.getStatus()).toUpperCase();
        String identityPolicy = normalizeIdentityReviewPolicy(exam.getIdentityReviewPolicy());

        List<String> blocked = new ArrayList<>();
        if (rulesRequired && !rulesAccepted) {
            blocked.add("MISSING_RULES_AGREEMENT");
        }
        if (cameraRequired && !cameraReady) {
            blocked.add("CAMERA_NOT_READY");
        }
        if (identityRequired) {
            boolean identityAccepted = "VERIFIED".equals(identityStatus)
                    || ("ALLOW_WITH_WARNING".equals(identityPolicy) && "NEEDS_REVIEW".equals(identityStatus));
            if (!identityAccepted) {
                blocked.add("IDENTITY_NOT_VERIFIED");
            }
        }

        return AttemptEntryStatusResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .status(attempt.getStatus() == null ? null : attempt.getStatus().name())
                .canStart(blocked.isEmpty())
                .blockedReasons(blocked)
                .rulesRequired(rulesRequired)
                .rulesAccepted(rulesAccepted)
                .rulesText(resolveRulesText(exam))
                .rulesVersion(resolveRulesVersion(exam))
                .identityRequired(identityRequired)
                .identityStatus(identityStatus)
                .identityCheckId(latestIdentity == null ? null : latestIdentity.getId())
                .identityReviewPolicy(identityPolicy)
                .cameraRequired(cameraRequired)
                .cameraReady(cameraReady)
                .micReady(micReady)
                .inExamIdentityCheckEnabled(resolveInExamIdentityCheckEnabled(exam))
                .identityCheckIntervalSeconds(resolveIdentityCheckInterval(exam))
                .rulesAgreedAt(DateTimeUtils.toOffset(attempt.getRulesAgreedAt(), VietNamTime.zone()))
                .build();
    }

    private StudentIdentityCheck latestIdentityCheck(Long attemptId) {
        if (attemptId == null) {
            return null;
        }
        return studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(attemptId).orElse(null);
    }

    private void publishAttemptStartedAfterCommit(ExamAttempt attempt) {
        publishAttemptPresenceAfterCommit(attempt, true);
    }

    private void publishAttemptJoinedAfterCommit(ExamAttempt attempt) {
        publishAttemptPresenceAfterCommit(attempt, false);
    }

    private void publishAttemptWaitingAfterCommit(ExamAttempt attempt) {
        Long examId = attempt.getExam().getId();
        Long attemptId = attempt.getId();
        String studentUsername = attempt.getStudent().getUsername();
        String studentName = attempt.getStudent().getFullName();
        String email = attempt.getStudent().getEmail();
        String studentCode = attempt.getStudent().getStudentCode();
        LocalDateTime joinedAt = attempt.getJoinedAt() != null ? attempt.getJoinedAt() : attempt.getStartedAt();
        Integer riskScore = attempt.getRiskScore();
        String riskLevel = attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name();
        Boolean cameraOn = attempt.getCameraOn();
        Boolean micOn = attempt.getMicOn();
        String clientIp = attempt.getClientIp();

        Runnable publish = () -> realtimeNotificationService.notifyAttemptWaiting(
                examId,
                attemptId,
                studentUsername,
                studentName,
                email,
                studentCode,
                joinedAt,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private void publishAttemptPresenceAfterCommit(ExamAttempt attempt, boolean firstStart) {
        Long examId = attempt.getExam().getId();
        Long attemptId = attempt.getId();
        String studentUsername = attempt.getStudent().getUsername();
        String studentName = attempt.getStudent().getFullName();
        String email = attempt.getStudent().getEmail();
        String studentCode = attempt.getStudent().getStudentCode();
        String status = attempt.getStatus() != null ? attempt.getStatus().name() : AttemptStatus.IN_PROGRESS.name();
        LocalDateTime startedAt = attempt.getStartedAt();
        LocalDateTime deadlineAt = submissionHelper.deadlineAt(attempt);
        Long remainingSeconds = submissionHelper.remainingSeconds(attempt);
        Integer riskScore = attempt.getRiskScore();
        String riskLevel = attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name();
        Boolean cameraOn = attempt.getCameraOn();
        Boolean micOn = attempt.getMicOn();
        String clientIp = attempt.getClientIp();

        Runnable publish = firstStart
                ? () -> realtimeNotificationService.notifyAttemptStarted(
                        examId,
                        attemptId,
                        studentUsername,
                        studentName,
                        email,
                        studentCode,
                        status,
                        startedAt,
                        deadlineAt,
                        remainingSeconds,
                        riskScore,
                        riskLevel,
                        cameraOn,
                        micOn,
                        clientIp
                )
                : () -> realtimeNotificationService.notifyAttemptJoined(
                        examId,
                        attemptId,
                        studentUsername,
                        studentName,
                        email,
                        studentCode,
                        status,
                        startedAt,
                        deadlineAt,
                        remainingSeconds,
                        riskScore,
                        riskLevel,
                        cameraOn,
                        micOn,
                        clientIp
                );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(Long attemptId, User student, SubmitAttemptRequest request) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            return toSubmitResponse(attempt);
        }

        if (attempt.getStatus() == AttemptStatus.STOPPED) {
            if (VietNamTime.now().isAfter(submissionHelper.deadlineAt(attempt))) {
                autoSubmitFromDraft(attempt, VietNamTime.now());
                return toSubmitResponse(attempt);
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam attempt has been suspended by proctor");
        }
        if (attempt.getStatus() == AttemptStatus.PAUSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam attempt is paused pending proctor review");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        LocalDateTime now = VietNamTime.now();
        Exam exam = attempt.getExam();
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has ended");
        }

        // Use Helper - khi hết hạn: lưu đáp án hiện tại rồi tự động nộp
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                submissionHelper.saveAnswers(attempt, normalizeAttemptAnswers(attempt, request.getAnswers()));
            }
            autoSubmitFromDraft(attempt, now);
            return toSubmitResponse(attempt);
        }

        submissionHelper.saveAnswers(attempt, normalizeAttemptAnswers(attempt, request.getAnswers()));

        attempt.setScore(submissionHelper.calculateScore(attempt));
        attempt.setSubmittedAt(now);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setAutoPausedBy(AutoPausedBy.NONE);
        attempt.setPausedAt(null);
        attempt.setSubmitCount(nextCount(attempt.getSubmitCount()));
        examAttemptRepository.save(attempt);

        monitoringService.recordAttemptHistoryEvent(
                attempt,
                MonitoringEventType.ATTEMPT_SUBMIT,
                buildAttemptSubmitDetails(attempt, answerRepository.findByAttempt(attempt).size()));
        detectAndLogFastSubmit(attempt, now);
        publishAttemptSubmittedAfterCommit(attempt, "Thi sinh da nop bai");

        return toSubmitResponse(attempt);
    }

    @Transactional
    public DraftSaveResponse saveDraftAnswers(Long attemptId, User student, String clientIp, List<AnswerInput> answers) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (attempt.getStatus() == AttemptStatus.PAUSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is paused pending proctor review");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        LocalDateTime now = VietNamTime.now();

        // Use Helper - khi hết hạn: lưu đáp án rồi tự động nộp, không throw
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            submissionHelper.saveAnswers(attempt, normalizeAttemptAnswers(attempt, answers));
            int answeredCount = answerRepository.findByAttempt(attempt).size();
            attempt.setLastSavedAt(now);
            attempt.setSaveCount(nextCount(attempt.getSaveCount()));
            examAttemptRepository.save(attempt);
            monitoringService.recordAttemptHistoryEvent(
                    attempt,
                    MonitoringEventType.DRAFT_SAVE,
                    buildDraftSaveDetails(attempt, answeredCount, 0L, normalizeClientIp(clientIp)));
            autoSubmitFromDraft(attempt, now);
            return DraftSaveResponse.builder()
                    .attemptId(attempt.getId())
                    .savedAt(DateTimeUtils.toOffset(now, VietNamTime.zone()))
                    .answeredCount(answeredCount)
                    .remainingSeconds(0L)
                    .build();
        }

        String normalizedIp = normalizeClientIp(clientIp);
        enforceIpConsistency(attempt, normalizedIp);
        submissionHelper.saveAnswers(attempt, normalizeAttemptAnswers(attempt, answers));
        if (shouldRunIntegrityCheck(attempt, now)) {
            duplicateIpDetectionService.detect(attempt);
            attempt.setLastIntegrityCheckAt(now);
        }

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        long remainingSeconds = submissionHelper.remainingSeconds(attempt);
        attempt.setLastSavedAt(now);
        attempt.setSaveCount(nextCount(attempt.getSaveCount()));
        examAttemptRepository.save(attempt);
        monitoringService.recordAttemptHistoryEvent(
                attempt,
                MonitoringEventType.DRAFT_SAVE,
                buildDraftSaveDetails(attempt, answeredCount, remainingSeconds, normalizedIp));

        realtimeNotificationService.notifyDraftSaved(attempt, answeredCount, remainingSeconds);

        return DraftSaveResponse.builder()
                .attemptId(attempt.getId())
                .savedAt(DateTimeUtils.toOffset(now, VietNamTime.zone()))
                .answeredCount(answeredCount)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    @Transactional(readOnly = true)
    public DraftAnswersResponse getDraftAnswers(Long attemptId, User student) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        List<com.example.demo.domain.entity.Question> orderedQuestions =
                questionService.orderQuestionsForAttempt(attempt.getExam(), attempt.getId());
        Map<Long, Integer> questionOrder = new java.util.LinkedHashMap<>();
        for (int i = 0; i < orderedQuestions.size(); i++) {
            questionOrder.put(orderedQuestions.get(i).getId(), i);
        }

        List<DraftAnswerItem> answers = answerRepository.findByAttempt(attempt).stream()
                .sorted(Comparator.comparingInt(answer -> questionOrder.getOrDefault(answer.getQuestion().getId(), Integer.MAX_VALUE)))
                .map(answer -> DraftAnswerItem.builder()
                        .questionId(answer.getQuestion().getId())
                        .selectedAnswer(questionService.renderAttemptAnswer(
                                answer.getQuestion(),
                                attempt.getId(),
                                answer.getSelectedAnswer()
                        ))
                        .build())
                .toList();

        return DraftAnswersResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .answers(answers)
                .build();
    }

    /**
     * Danh sách câu hỏi cho lượt làm bài của học sinh (không gồm đáp án đúng; thứ tự/shuffle theo attempt).
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestionsForStudentAttempt(Long attemptId, User student) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        return questionService.listByExam(attempt.getExam(), false, student, attemptId);
    }

    private List<AnswerInput> normalizeAttemptAnswers(ExamAttempt attempt, List<AnswerInput> answers) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }
        List<AnswerInput> normalized = new ArrayList<>(answers.size());
        for (AnswerInput input : answers) {
            if (input == null || input.getQuestionId() == null) {
                continue;
            }
            var question = questionRepository.findById(input.getQuestionId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Question not found: " + input.getQuestionId()));
            if (!question.getExam().getId().equals(attempt.getExam().getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Question not in this exam: " + input.getQuestionId());
            }
            AnswerInput normalizedInput = new AnswerInput();
            normalizedInput.setQuestionId(input.getQuestionId());
            normalizedInput.setSelectedAnswer(
                    questionService.normalizeAttemptAnswer(question, attempt.getId(), input.getSelectedAnswer())
            );
            normalized.add(normalizedInput);
        }
        return normalized;
    }

    private static boolean monitoringFlagEnabled(Boolean value) {
        return !Boolean.FALSE.equals(value);
    }

    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        int totalQuestions = questionRepository.findByExam(attempt.getExam()).size();
        ReviewSummary reviewSummary = buildReviewSummary(attempt);

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : null)
                .suspicious(attempt.getSuspicious())
                .violationCount(reviewSummary.violationCount())
                .warningCount(reviewSummary.warningCount())
                .reviewRequired(reviewSummary.reviewRequired())
                .recommendedAction(reviewSummary.recommendedAction())
                .reasons(reviewSummary.reasons())
                .evidenceSummary(reviewSummary.evidenceSummary())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .examTitle(attempt.getExam().getTitle())
                .answeredCount(answeredCount)
                .totalQuestions(totalQuestions)
                .clientIp(attempt.getClientIp())
                .deviceFingerprint(attempt.getDeviceFingerprint())
                .originalDeviceFingerprint(attempt.getOriginalDeviceFingerprint())
                .cameraOn(attempt.getCameraOn())
                .micOn(attempt.getMicOn())
                .saveCount(attempt.getSaveCount())
                .submitCount(attempt.getSubmitCount())
                .fullscreenRequired(attempt.getFullscreenRequired())
                .enableAiProctoring(Boolean.TRUE.equals(attempt.getExam().getEnableAiProctoring()))
                .requireCameraMic(monitoringFlagEnabled(attempt.getExam().getRequireCameraMic()))
                .monitorTabSwitch(monitoringFlagEnabled(attempt.getExam().getMonitorTabSwitch()))
                .monitorBlur(monitoringFlagEnabled(attempt.getExam().getMonitorBlur()))
                .monitorExitFullscreen(monitoringFlagEnabled(attempt.getExam().getMonitorExitFullscreen()))
                .monitorCopyPaste(monitoringFlagEnabled(attempt.getExam().getMonitorCopyPaste()))
                .monitorIdleTime(monitoringFlagEnabled(attempt.getExam().getMonitorIdleTime()))
                .monitorDevtools(monitoringFlagEnabled(attempt.getExam().getMonitorDevtools()))
                .monitorDuplicateIp(monitoringFlagEnabled(attempt.getExam().getMonitorDuplicateIp()))
                .monitorFastSubmit(monitoringFlagEnabled(attempt.getExam().getMonitorFastSubmit()))
                .monitorRightClick(monitoringFlagEnabled(attempt.getExam().getMonitorRightClick()))
                .monitorPrintScreen(monitoringFlagEnabled(attempt.getExam().getMonitorPrintScreen()))
                .monitorRapidQuestionSwitch(monitoringFlagEnabled(attempt.getExam().getMonitorRapidQuestionSwitch()))
                .monitorMultiMonitor(monitoringFlagEnabled(attempt.getExam().getMonitorMultiMonitor()))
                .monitorNetworkInstability(monitoringFlagEnabled(attempt.getExam().getMonitorNetworkInstability()))
                .monitorSessionRecovery(monitoringFlagEnabled(attempt.getExam().getMonitorSessionRecovery()))
                .monitorQuestionTimingAnomaly(monitoringFlagEnabled(attempt.getExam().getMonitorQuestionTimingAnomaly()))
                .monitorAnswerChangeBurst(monitoringFlagEnabled(attempt.getExam().getMonitorAnswerChangeBurst()))
                .monitorClipboardBurst(monitoringFlagEnabled(attempt.getExam().getMonitorClipboardBurst()))
                .monitorFullscreenEvasion(monitoringFlagEnabled(attempt.getExam().getMonitorFullscreenEvasion()))
                .inExamIdentityCheckEnabled(resolveInExamIdentityCheckEnabled(attempt.getExam()))
                .identityCheckIntervalSeconds(resolveIdentityCheckInterval(attempt.getExam()))
                .identityStatus(latestIdentityCheck(attempt.getId()) == null ? "NOT_CHECKED" : latestIdentityCheck(attempt.getId()).getStatus())
                .identityCheckId(latestIdentityCheck(attempt.getId()) == null ? null : latestIdentityCheck(attempt.getId()).getId())
                .build();
    }

    @Transactional(readOnly = true)
    public AttemptReportResponse getAttemptReport(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        List<Answer> answers = answerRepository.findByAttempt(attempt);
        long correctCount = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        List<com.example.demo.domain.entity.Question> orderedQuestions =
                questionService.orderQuestionsForAttempt(attempt.getExam(), attempt.getId());
        Map<Long, Answer> answersByQuestionId = new java.util.LinkedHashMap<>();
        for (Answer answer : answers) {
            answersByQuestionId.put(answer.getQuestion().getId(), answer);
        }

        List<AttemptReportAnswerItem> answerRows = orderedQuestions.stream()
                .map(question -> {
                    Answer answer = answersByQuestionId.get(question.getId());
                    return AttemptReportAnswerItem.builder()
                        .questionId(question.getId())
                        .question(question.getContent())
                        .questionType(question.getType() == null ? "SINGLE_CHOICE" : question.getType().name())
                        .options(questionService.renderAttemptOptions(question, attempt.getId()))
                        .selectedAnswer(answer == null ? null : questionService.renderAttemptAnswer(question, attempt.getId(), answer.getSelectedAnswer()))
                        .correctAnswer(questionService.renderAttemptAnswer(question, attempt.getId(), question.getCorrectAnswer()))
                        .correct(answer == null ? null : answer.getCorrect())
                        .scoreWeight(question.getScoreWeight())
                        .metadata(question.getMetadata())
                        .build();
                })
                .toList();

        return AttemptReportResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .answeredCount(answers.size())
                .correctCount(correctCount)
                .answers(answerRows)
                .build();
    }

    public List<ExamAttempt> listByStudent(User student) {
        return examAttemptRepository.findByStudent(student);
    }

    /**
     * Phải chạy trong một transaction: open-in-view=false nên lazy Exam/User không thể đọc sau khi trả list về controller.
     */
    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listAttemptSummariesForStudent(User student) {
        List<ExamAttempt> attempts = examAttemptRepository.findByStudentWithExamAndUsers(
                        student,
                        PageRequest.of(0, LIST_STUDENT_ATTEMPTS_MAX));
        return toSummaries(attempts);
    }

    /**
     * Giáo viên / admin xem danh sách lượt làm theo đề — cùng lý do {@link #listAttemptSummariesForStudent}.
     */
    private static final int LIST_STUDENT_ATTEMPTS_MAX = 1_000;
    private static final int LIST_EXAM_ATTEMPTS_MAX = 10_000;

    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listAttemptSummariesForExam(Exam exam) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamWithStudent(
                        exam,
                        PageRequest.of(0, LIST_EXAM_ATTEMPTS_MAX));
        return toSummaries(attempts);
    }

    /**
     * Danh sách attempts của đợt thi hiện tại (theo exam.startTime, exam.endTime).
     * Khi tạo đợt thi mới, chỉ lấy attempts trong khoảng thời gian đó, không lấy dữ liệu cũ.
     */
    @Transactional(readOnly = true)
    public List<ExamAttempt> listByExam(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime());
        }
        return examAttemptRepository.findByExam(exam);
    }

    @Transactional(readOnly = true)
    public AttemptFilterResponse listByExamFiltered(Exam exam,
            String status,
            Boolean suspicious,
            String student,
            Integer riskMin,
            Integer riskMax,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        AttemptStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = AttemptStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported status");
            }
        }

        String studentKeyword = (student == null || student.isBlank()) ? "" : student.trim().toLowerCase();

        java.time.LocalDateTime sessionFrom = exam.getStartTime();
        java.time.LocalDateTime sessionTo = exam.getEndTime();

        Page<ExamAttempt> filteredPage = examAttemptRepository.searchByExam(
                exam.getId(),
                sessionFrom,
                sessionTo,
                parsedStatus != null ? parsedStatus.name() : null,
                suspicious,
                studentKeyword,
                riskMin,
                riskMax,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "started_at")));

        return AttemptFilterResponse.builder()
                .items(toSummaries(filteredPage.getContent()))
                .total(filteredPage.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public ExamAttempt requireAttempt(Long attemptId) {
        return examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
    }

    public void ensureCanViewAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = hasRole(actor, RoleName.ADMIN);
        boolean isTeacher = hasRole(actor, RoleName.TEACHER);
        boolean isOwnerStudent = attempt.getStudent().getId().equals(actor.getId());
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());

        if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
        }
    }

    private void ensureCanEditAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = hasRole(actor, RoleName.ADMIN);
        boolean isOwnerStudent = attempt.getStudent().getId().equals(actor.getId());
        if (!(isAdmin || isOwnerStudent)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to update this attempt");
        }
    }

    public AttemptSummaryResponse toSummary(ExamAttempt attempt) {
        return toSummary(attempt, buildReviewSummary(attempt));
    }

    private List<AttemptSummaryResponse> toSummaries(List<ExamAttempt> attempts) {
        Map<Long, ReviewSummary> reviewSummaries = buildReviewSummaries(attempts);
        return attempts.stream()
                .map(attempt -> toSummary(attempt, reviewSummaries.getOrDefault(attempt.getId(), buildReviewSummary(attempt, 0, 0))))
                .toList();
    }

    private AttemptSummaryResponse toSummary(ExamAttempt attempt, ReviewSummary reviewSummary) {
        boolean isPractice = attempt.getExam().getCreatedBy().getId().equals(attempt.getStudent().getId());
        StudentIdentityCheck identity = latestIdentityCheck(attempt.getId());
        return AttemptSummaryResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .examTitle(attempt.getExam().getTitle())
                .isPractice(isPractice)
                .student(attempt.getStudent().getUsername())
                .email(attempt.getStudent().getEmail())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : null)
                .suspicious(attempt.getSuspicious())
                .violationCount(reviewSummary.violationCount())
                .warningCount(reviewSummary.warningCount())
                .reviewRequired(reviewSummary.reviewRequired())
                .recommendedAction(reviewSummary.recommendedAction())
                .reasons(reviewSummary.reasons())
                .evidenceSummary(reviewSummary.evidenceSummary())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .cameraOn(attempt.getCameraOn())
                .micOn(attempt.getMicOn())
                .clientIp(attempt.getClientIp())
                .deviceFingerprint(attempt.getDeviceFingerprint())
                .originalDeviceFingerprint(attempt.getOriginalDeviceFingerprint())
                .saveCount(attempt.getSaveCount())
                .submitCount(attempt.getSubmitCount())
                .fullscreenRequired(attempt.getFullscreenRequired())
                .identityStatus(identity == null ? "NOT_CHECKED" : identity.getStatus())
                .identityCheckId(identity == null ? null : identity.getId())
                .build();
    }

    private static final int EXPORT_MAX_RECORDS = 10_000;

    @Transactional(readOnly = true)
    public byte[] exportExamAttemptsCsv(Exam exam) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamWithStudent(
                exam,
                PageRequest.of(0, EXPORT_MAX_RECORDS));
        Map<Long, AttemptSummaryResponse> summariesById = toSummaries(attempts).stream()
                .collect(java.util.stream.Collectors.toMap(AttemptSummaryResponse::getId, summary -> summary));
        StringBuilder csv = new StringBuilder();
        csv.append("attemptId,examId,student,status,score,riskScore,suspicious,startedAt,submittedAt,deadlineAt,remainingSeconds\n");

        for (ExamAttempt attempt : attempts) {
            AttemptSummaryResponse summary = summariesById.get(attempt.getId());
            csv.append(summary.getId()).append(',')
                    .append(summary.getExamId()).append(',')
                    .append(escapeCsv(summary.getStudent())).append(',')
                    .append(summary.getStatus()).append(',')
                    .append(summary.getScore() == null ? "" : summary.getScore()).append(',')
                    .append(summary.getRiskScore() == null ? "" : summary.getRiskScore()).append(',')
                    .append(summary.getSuspicious() == null ? "" : summary.getSuspicious()).append(',')
                    .append(summary.getStartedAt() == null ? "" : summary.getStartedAt()).append(',')
                    .append(summary.getSubmittedAt() == null ? "" : summary.getSubmittedAt()).append(',')
                    .append(summary.getDeadlineAt() == null ? "" : summary.getDeadlineAt()).append(',')
                    .append(summary.getRemainingSeconds() == null ? "" : summary.getRemainingSeconds())
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private StartAttemptResponse toStartResponse(ExamAttempt attempt) {
        StudentIdentityCheck identity = latestIdentityCheck(attempt.getId());
        AttemptEntryStatusResponse entryStatus = buildEntryStatus(attempt);
        return StartAttemptResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .joinedAt(DateTimeUtils.toOffset(attempt.getJoinedAt(), VietNamTime.zone()))
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .status(attempt.getStatus().name())
                .rulesAccepted(Boolean.TRUE.equals(entryStatus.getRulesAccepted()))
                .identityStatus(identity == null ? "NOT_CHECKED" : identity.getStatus())
                .identityCheckId(identity == null ? null : identity.getId())
                .inExamIdentityCheckEnabled(resolveInExamIdentityCheckEnabled(attempt.getExam()))
                .identityCheckIntervalSeconds(resolveIdentityCheckInterval(attempt.getExam()))
                .entryBlockedReasons(entryStatus.getBlockedReasons())
                .build();
    }

    private SubmitAttemptResponse toSubmitResponse(ExamAttempt attempt) {
        return SubmitAttemptResponse.builder()
                .attemptId(attempt.getId())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .status(attempt.getStatus().name())
                .build();
    }

    private void autoSubmitFromDraft(ExamAttempt attempt, LocalDateTime submittedAt) {
        attempt.setScore(submissionHelper.calculateScore(attempt));
        attempt.setSubmittedAt(submittedAt);
        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        attempt.setAutoPausedBy(AutoPausedBy.NONE);
        attempt.setPausedAt(null);
        attempt.setSubmitCount(nextCount(attempt.getSubmitCount()));
        examAttemptRepository.save(attempt);
        monitoringService.recordAttemptHistoryEvent(
                attempt,
                MonitoringEventType.AUTO_SUBMIT,
                buildAttemptSubmitDetails(attempt, answerRepository.findByAttempt(attempt).size()));
        publishAttemptSubmittedAfterCommit(attempt, "Thi sinh da tu dong nop bai");
    }

    private void publishAttemptSubmittedAfterCommit(ExamAttempt attempt, String message) {
        if (attempt == null || attempt.getExam() == null || attempt.getStudent() == null) {
            return;
        }
        Long examId = attempt.getExam().getId();
        Long attemptId = attempt.getId();
        String studentUsername = attempt.getStudent().getUsername();
        String studentName = attempt.getStudent().getFullName();
        String email = attempt.getStudent().getEmail();
        String studentCode = attempt.getStudent().getStudentCode();
        String status = attempt.getStatus() != null ? attempt.getStatus().name() : AttemptStatus.SUBMITTED.name();
        Double score = attempt.getScore();
        LocalDateTime startedAt = attempt.getStartedAt();
        LocalDateTime submittedAt = attempt.getSubmittedAt();
        LocalDateTime deadlineAt = submissionHelper.deadlineAt(attempt);
        Long remainingSeconds = submissionHelper.remainingSeconds(attempt);
        Integer riskScore = attempt.getRiskScore();
        String riskLevel = attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name();
        Boolean cameraOn = attempt.getCameraOn();
        Boolean micOn = attempt.getMicOn();
        String clientIp = attempt.getClientIp();

        Runnable publish = () -> realtimeNotificationService.notifyAttemptSubmitted(
                examId,
                attemptId,
                studentUsername,
                studentName,
                email,
                studentCode,
                status,
                score,
                startedAt,
                submittedAt,
                deadlineAt,
                remainingSeconds,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp,
                message
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private void detectAndLogFastSubmit(ExamAttempt attempt, LocalDateTime submittedAt) {
        if (!Boolean.TRUE.equals(attempt.getExam().getMonitorFastSubmit())) {
            return;
        }
        LocalDateTime startedAt = attempt.getStartedAt();
        if (startedAt == null) return;

        Integer durationMinutes = attempt.getExam().getDurationMinutes();
        long totalSeconds = Math.max(60L, (durationMinutes == null ? 60 : durationMinutes) * 60L);
        long elapsedSeconds = Duration.between(startedAt, submittedAt).getSeconds();
        if (elapsedSeconds <= 0) return;

        double progress = (double) elapsedSeconds / totalSeconds;
        int totalQuestions = questionRepository.findByExam(attempt.getExam()).size();
        int answeredCount = answerRepository.findByAttempt(attempt).size();

        if (totalQuestions <= 0) return;

        double progressAnswered = (double) answeredCount / totalQuestions;
        if (progress < 0.25 && progressAnswered >= 0.9) {
            String details = String.format("elapsed=%ds;total=%ds;progress=%.1f%%;answered=%d/%d",
                    elapsedSeconds, totalSeconds, progress * 100, answeredCount, totalQuestions);
            monitoringService.addSystemEvent(attempt, MonitoringEventType.FAST_SUBMIT, details);
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null) {
            return null;
        }
        String normalized = clientIp.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private boolean resolveRequireIdentityVerification(Exam exam) {
        if (exam == null) {
            return false;
        }
        if (exam.getRequireIdentityVerification() != null) {
            return exam.getRequireIdentityVerification();
        }
        return Boolean.TRUE.equals(exam.getRequireCameraMic()) || Boolean.TRUE.equals(exam.getEnableAiProctoring());
    }

    private boolean resolveInExamIdentityCheckEnabled(Exam exam) {
        if (exam == null) {
            return false;
        }
        if (exam.getInExamIdentityCheckEnabled() != null) {
            return exam.getInExamIdentityCheckEnabled();
        }
        return Boolean.TRUE.equals(exam.getRequireCameraMic()) || Boolean.TRUE.equals(exam.getEnableAiProctoring());
    }

    private int resolveIdentityCheckInterval(Exam exam) {
        Integer seconds = exam == null ? null : exam.getIdentityCheckIntervalSeconds();
        if (seconds == null) {
            return 60;
        }
        return Math.max(30, Math.min(seconds, 600));
    }

    private String normalizeIdentityReviewPolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return "ALLOW_WITH_WARNING";
        }
        String normalized = policy.trim().toUpperCase();
        return "BLOCK_UNTIL_VERIFIED".equals(normalized) ? normalized : "ALLOW_WITH_WARNING";
    }

    private String resolveRulesText(Exam exam) {
        if (exam != null && exam.getRulesText() != null && !exam.getRulesText().isBlank()) {
            return exam.getRulesText().trim();
        }
        return """
                1. Thí sinh phải tự làm bài, không trao đổi, không sử dụng tài liệu hoặc thiết bị ngoài phạm vi cho phép.
                2. Thí sinh phải duy trì camera trong suốt thời gian làm bài nếu kỳ thi yêu cầu giám sát.
                3. Không rời khỏi màn hình thi, không chuyển tab, không mở công cụ phát triển hoặc phần mềm hỗ trợ.
                4. Hệ thống có thể ghi nhận sự kiện giám sát, ảnh xác minh danh tính và tín hiệu rủi ro để giám thị xem xét.
                5. Vi phạm quy chế có thể khiến bài thi bị tạm dừng, đình chỉ hoặc chuyển sang trạng thái cần duyệt.
                """.trim();
    }

    private String resolveRulesVersion(Exam exam) {
        if (exam != null && exam.getRulesVersion() != null && !exam.getRulesVersion().isBlank()) {
            return exam.getRulesVersion().trim();
        }
        return "rules-" + Integer.toHexString(resolveRulesText(exam).hashCode());
    }

    private void enforceIpConsistency(ExamAttempt attempt, String currentIp) {
        String registeredIp = normalizeClientIp(attempt.getClientIp());
        if (registeredIp == null || currentIp == null || registeredIp.equals(currentIp)) {
            return;
        }

        String detail = "from=" + registeredIp + ";to=" + currentIp;
        auditLogService.logSystemIpChange(attempt, detail);
        attempt.setStatus(AttemptStatus.STOPPED);
        examAttemptRepository.save(attempt);
        realtimeNotificationService.notifyAttemptStopped(attempt,
                "Phát hiện thay đổi địa chỉ IP trong khi làm bài. Bài thi đã bị đình chỉ.");
        throw new ApiException(HttpStatus.BAD_REQUEST, "IP changed during attempt. Attempt has been suspended");
    }

    private boolean shouldRunIntegrityCheck(ExamAttempt attempt, LocalDateTime now) {
        if (attempt.getLastIntegrityCheckAt() == null) {
            return true;
        }
        return attempt.getLastIntegrityCheckAt().isBefore(now.minusSeconds(Math.max(integrityCheckCooldownSeconds, 1)));
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }

    private int nextCount(Integer current) {
        return (current == null ? 0 : current) + 1;
    }

    private String buildAttemptStartDetails(ExamAttempt attempt, String clientIp) {
        return "status=" + safeName(attempt.getStatus())
                + ";startedAt=" + attempt.getStartedAt()
                + ";clientIp=" + safe(clientIp);
    }

    private String buildDraftSaveDetails(ExamAttempt attempt, int answeredCount, long remainingSeconds, String clientIp) {
        return "savedAt=" + attempt.getLastSavedAt()
                + ";answeredCount=" + answeredCount
                + ";remainingSeconds=" + remainingSeconds
                + ";saveCount=" + safe(attempt.getSaveCount())
                + ";clientIp=" + safe(clientIp);
    }

    private String buildAttemptSubmitDetails(ExamAttempt attempt, int answeredCount) {
        return "status=" + safeName(attempt.getStatus())
                + ";submittedAt=" + attempt.getSubmittedAt()
                + ";answeredCount=" + answeredCount
                + ";score=" + safe(attempt.getScore())
                + ";submitCount=" + safe(attempt.getSubmitCount());
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private ReviewSummary buildReviewSummary(ExamAttempt attempt) {
        int violationCount = Math.toIntExact(fraudSignalRepository.countByAttempt(attempt));
        int warningCount = Math.toIntExact(fraudWarningRepository.countByAttemptOrRelatedAttemptId(attempt.getId()));
        return buildReviewSummary(attempt, violationCount, warningCount);
    }

    private Map<Long, ReviewSummary> buildReviewSummaries(List<ExamAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return Map.of();
        }
        List<Long> attemptIds = attempts.stream()
                .map(ExamAttempt::getId)
                .filter(Objects::nonNull)
                .toList();
        if (attemptIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> violationCounts = countMap(fraudSignalRepository.countGroupedByAttemptIds(attemptIds));
        Map<Long, Integer> warningCounts = countMap(fraudWarningRepository.countDirectGroupedByAttemptIds(attemptIds));

        Set<Long> attemptIdSet = new java.util.HashSet<>(attemptIds);
        List<Long> examIds = attempts.stream()
                .map(attempt -> attempt.getExam() != null ? attempt.getExam().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!examIds.isEmpty()) {
            for (var warning : fraudWarningRepository.findRelatedWarningsByExamIds(examIds)) {
                Long directAttemptId = warning.getAttempt() != null ? warning.getAttempt().getId() : null;
                for (Long relatedAttemptId : parseRelatedAttemptIds(warning.getRelatedAttemptIds())) {
                    if (!attemptIdSet.contains(relatedAttemptId) || Objects.equals(directAttemptId, relatedAttemptId)) {
                        continue;
                    }
                    warningCounts.merge(relatedAttemptId, 1, Integer::sum);
                }
            }
        }

        Map<Long, ReviewSummary> summaries = new java.util.HashMap<>();
        for (ExamAttempt attempt : attempts) {
            Long attemptId = attempt.getId();
            summaries.put(attemptId, buildReviewSummary(
                    attempt,
                    violationCounts.getOrDefault(attemptId, 0),
                    warningCounts.getOrDefault(attemptId, 0)));
        }
        return summaries;
    }

    private Map<Long, Integer> countMap(List<Object[]> rows) {
        Map<Long, Integer> result = new java.util.HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            Integer count = Math.toIntExact(((Number) row[1]).longValue());
            result.put(id, count);
        }
        return result;
    }

    private Set<Long> parseRelatedAttemptIds(String relatedAttemptIds) {
        if (relatedAttemptIds == null || relatedAttemptIds.isBlank()) {
            return Set.of();
        }
        Set<Long> result = new java.util.HashSet<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < relatedAttemptIds.length(); i++) {
            char ch = relatedAttemptIds.charAt(i);
            if (Character.isDigit(ch)) {
                current.append(ch);
            } else if (!current.isEmpty()) {
                result.add(Long.parseLong(current.toString()));
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            result.add(Long.parseLong(current.toString()));
        }
        return result;
    }

    private ReviewSummary buildReviewSummary(ExamAttempt attempt, int violationCount, int warningCount) {
        String recommendedAction = switch (attempt.getRiskLevel() != null ? attempt.getRiskLevel() : RiskLevel.CLEAN) {
            case CRITICAL -> "PAUSE_AND_REVIEW";
            case HIGH_RISK -> "WARN_AND_ESCALATE";
            case SUSPICIOUS -> "REVIEW_ATTEMPT";
            case CLEAN -> "CONTINUE_MONITORING";
        };
        boolean reviewRequired = Boolean.TRUE.equals(attempt.getSuspicious())
                || attempt.getStatus() == AttemptStatus.PAUSED
                || warningCount > 0;
        List<String> reasons = new ArrayList<>();
        if (attempt.getRiskLevel() == RiskLevel.CRITICAL) {
            reasons.add("Điểm rủi ro đang ở mức nghiêm trọng");
        } else if (attempt.getRiskLevel() == RiskLevel.HIGH_RISK) {
            reasons.add("Điểm rủi ro đang ở mức cao");
        } else if (attempt.getRiskLevel() == RiskLevel.SUSPICIOUS) {
            reasons.add("Phiên thi có tín hiệu đáng ngờ");
        }
        if (violationCount > 0) {
            reasons.add("Có " + violationCount + " tín hiệu gian lận đã ghi nhận");
        }
        if (warningCount > 0) {
            reasons.add("Có " + warningCount + " cảnh báo cần review");
        }
        List<String> evidenceSummary = new ArrayList<>();
        if (attempt.getClientIp() != null && !attempt.getClientIp().isBlank()) {
            evidenceSummary.add("IP hiện tại: " + attempt.getClientIp());
        }
        if (attempt.getDeviceFingerprint() != null && !attempt.getDeviceFingerprint().isBlank()) {
            evidenceSummary.add("Fingerprint thiết bị đã được ghi nhận");
        }
        if (violationCount > 0) {
            evidenceSummary.add("Tín hiệu gian lận: " + violationCount);
        }
        if (warningCount > 0) {
            evidenceSummary.add("Cảnh báo review: " + warningCount);
        }
        return new ReviewSummary(
                violationCount,
                warningCount,
                reviewRequired,
                recommendedAction,
                reasons,
                evidenceSummary
        );
    }

    private record ReviewSummary(
            int violationCount,
            int warningCount,
            boolean reviewRequired,
            String recommendedAction,
            List<String> reasons,
            List<String> evidenceSummary
    ) {
    }
}
