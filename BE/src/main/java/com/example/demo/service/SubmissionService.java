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
import com.example.demo.domain.entity.Assignment;
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
import com.example.demo.repository.AssignmentRepository;
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

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final ExamAttemptRepository examAttemptRepository;
    private final AssignmentRepository assignmentRepository;
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
    private final ClassService classService;

    @Value("${demo.monitoring.integrity-check.cooldown-seconds:45}")
    private long integrityCheckCooldownSeconds;

    @Transactional
    public StartAttemptResponse prepareAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = VietNamTime.now();
        examService.validateExamJoinable(exam, nowInExamZone);
        ensureStudentCanEnterExam(exam, student);

        ExamAttempt existing = findReusableAttempt(exam, student);
        if (existing != null) {
            enforceIpConsistency(existing, normalizeClientIp(clientIp));
            return toStartResponse(existing);
        }

        enforceMaxAttempts(exam, student);

        String normalizedIp = normalizeClientIp(clientIp);

        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
                        .joinedAt(VietNamTime.now())
                        .status(AttemptStatus.WAITING)
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
        return toStartResponse(saved);
    }

    @Transactional
    public StartAttemptResponse startAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = VietNamTime.now();
        examService.validateExamAvailability(exam, nowInExamZone);
        ensureStudentCanEnterExam(exam, student);

        ExamAttempt existing = findReusableAttempt(exam, student);
        if (existing != null) {
            enforceIpConsistency(existing, normalizeClientIp(clientIp));
            if (existing.getStatus() == AttemptStatus.WAITING) {
                List<String> blocked = entryBlockedReasons(existing);
                if (!blocked.isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Exam entry requirements are incomplete: " + String.join(",", blocked));
                }
                existing.setStartedAt(VietNamTime.now());
                existing.setStatus(AttemptStatus.IN_PROGRESS);
                existing.setLastHeartbeatAt(VietNamTime.now());
                existing = examAttemptRepository.save(existing);
                duplicateIpDetectionService.detect(existing);
                monitoringService.recordAttemptHistoryEvent(
                        existing,
                        MonitoringEventType.ATTEMPT_START,
                        buildAttemptStartDetails(existing, normalizeClientIp(clientIp)));
                publishAttemptStartedAfterCommit(existing);
            } else {
                publishAttemptJoinedAfterCommit(existing);
            }
            return toStartResponse(existing);
        }

        enforceMaxAttempts(exam, student);

        String normalizedIp = normalizeClientIp(clientIp);

        if (!Boolean.TRUE.equals(exam.getPractice())
                && (rulesAgreementRequired(exam) || cameraRequired(exam) || identityRequired(exam))) {
            ExamAttempt waiting = examAttemptRepository.save(
                    ExamAttempt.builder()
                            .exam(exam)
                            .student(student)
                            .joinedAt(VietNamTime.now())
                            .status(AttemptStatus.WAITING)
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
            List<String> blocked = entryBlockedReasons(waiting);
            if (!blocked.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Exam entry requirements are incomplete: " + String.join(",", blocked));
            }
        }

        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
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

        duplicateIpDetectionService.detect(saved);
        monitoringService.recordAttemptHistoryEvent(
                saved,
                MonitoringEventType.ATTEMPT_START,
                buildAttemptStartDetails(saved, normalizedIp));
        publishAttemptStartedAfterCommit(saved);

        return toStartResponse(saved);
    }

    private ExamAttempt findReusableAttempt(Exam exam, User student) {
        ExamAttempt existing = examAttemptRepository
                .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (existing == null) {
            existing = examAttemptRepository
                    .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.PAUSED)
                    .orElse(null);
        }
        if (existing == null) {
            existing = examAttemptRepository
                    .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.WAITING)
                    .orElse(null);
        }
        return existing;
    }

    private void enforceMaxAttempts(Exam exam, User student) {
        if (Boolean.TRUE.equals(exam.getPractice())) {
            return;
        }
        int maxAttempts = resolveMaxAttempts(exam);
        long usedAttempts = examAttemptRepository.countByExamAndStudentAndStatusIn(
                exam,
                student,
                List.of(AttemptStatus.SUBMITTED, AttemptStatus.AUTO_SUBMITTED));
        if (usedAttempts >= maxAttempts) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Báº¡n Ä‘Ã£ háº¿t sá»‘ láº§n lÃ m bÃ i cho Ä‘á» thi nÃ y.");
        }
    }

    private int resolveMaxAttempts(Exam exam) {
        return assignmentRepository.findPublishedByExamOrderByCreatedAtDesc(exam).stream()
                .findFirst()
                .map(Assignment::getMaxAttempts)
                .filter(value -> value != null && value > 0)
                .orElseGet(() -> exam.getMaxAttempts() == null || exam.getMaxAttempts() <= 0 ? 1 : exam.getMaxAttempts());
    }

    private boolean resolveAllowReviewAfterSubmit(Exam exam) {
        return assignmentRepository.findPublishedByExamOrderByCreatedAtDesc(exam).stream()
                .findFirst()
                .map(Assignment::getAllowReviewAfterSubmit)
                .orElseGet(() -> exam.getAllowReviewAfterSubmit() == null || Boolean.TRUE.equals(exam.getAllowReviewAfterSubmit()));
    }

    private void publishAttemptStartedAfterCommit(ExamAttempt attempt) {
        publishAttemptPresenceAfterCommit(attempt, true);
    }

    private void publishAttemptJoinedAfterCommit(ExamAttempt attempt) {
        publishAttemptPresenceAfterCommit(attempt, false);
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
        ensureCanOperateStudentAttempt(attempt, student);

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
        ensureCanOperateStudentAttempt(attempt, student);

        if (attempt.getStatus() == AttemptStatus.PAUSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is paused pending proctor review");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        LocalDateTime now = VietNamTime.now();

        
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
        ensureCanOperateStudentAttempt(attempt, student);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }
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

    

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestionsForStudentAttempt(Long attemptId, User student) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanOperateStudentAttempt(attempt, student);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }
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

    private static boolean aiProctoringEnabled(Exam exam) {
        return Boolean.TRUE.equals(exam.getEnableAiProctoring())
                || aiFaceDetectionEnabled(exam)
                || aiEyeTrackingEnabled(exam);
    }

    private static boolean aiFaceDetectionEnabled(Exam exam) {
        if (exam.getAiFaceDetection() != null) {
            return Boolean.TRUE.equals(exam.getAiFaceDetection());
        }
        return Boolean.TRUE.equals(exam.getEnableAiProctoring());
    }

    private static boolean aiEyeTrackingEnabled(Exam exam) {
        if (exam.getAiEyeTracking() != null) {
            return Boolean.TRUE.equals(exam.getAiEyeTracking());
        }
        return Boolean.TRUE.equals(exam.getEnableAiProctoring());
    }

    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        int totalQuestions = questionRepository.findByExam(attempt.getExam()).size();
        ReviewSummary reviewSummary = buildReviewSummary(attempt);
        StudentIdentityCheck latestIdentity = latestIdentityCheck(attempt);

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(canViewScore(attempt, actor) ? attempt.getScore() : null)
                .riskScore(attempt.getRiskScore())
                .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : null)
                .suspicious(attempt.getSuspicious())
                .violationCount(reviewSummary.violationCount())
                .warningCount(reviewSummary.warningCount())
                .reviewRequired(reviewSummary.reviewRequired())
                .allowReviewAfterSubmit(resolveAllowReviewAfterSubmit(attempt.getExam()))
                .showScoreAfterSubmit(attempt.getExam().getShowScoreAfterSubmit() == null || Boolean.TRUE.equals(attempt.getExam().getShowScoreAfterSubmit()))
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
                .enableAiProctoring(aiProctoringEnabled(attempt.getExam()))
                .aiFaceDetection(aiFaceDetectionEnabled(attempt.getExam()))
                .aiEyeTracking(aiEyeTrackingEnabled(attempt.getExam()))
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
                .inExamIdentityCheckEnabled(inExamIdentityCheckEnabled(attempt.getExam()))
                .identityCheckIntervalSeconds(identityCheckIntervalSeconds(attempt.getExam()))
                .identityStatus(latestIdentity == null ? "NOT_CHECKED" : latestIdentity.getStatus())
                .identityCheckId(latestIdentity == null ? null : latestIdentity.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public AttemptReportResponse getAttemptReport(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);
        enforceReviewAllowedForStudent(attempt, actor);

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

    

    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listAttemptSummariesForStudent(User student) {
        return examAttemptRepository.findByStudent(student).stream()
                .map(this::toSummary)
                .toList();
    }

    

    private static final int LIST_EXAM_ATTEMPTS_MAX = 10_000;

    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listAttemptSummariesForExam(Exam exam) {
        return examAttemptRepository.findByExamWithStudent(exam).stream()
                .limit(LIST_EXAM_ATTEMPTS_MAX)
                .map(this::toSummary)
                .toList();
    }

    

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
        int safeSize = Math.max(size, 1);

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
                .items(filteredPage.getContent().stream().map(this::toSummary).toList())
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

    private void ensureCanOperateStudentAttempt(ExamAttempt attempt, User actor) {
        if (attempt == null || actor == null || attempt.getStudent() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
        }
        if (attempt.getStudent().getId().equals(actor.getId()) || hasRole(actor, RoleName.ADMIN)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
    }

    private void enforceReviewAllowedForStudent(ExamAttempt attempt, User actor) {
        if (attempt.getStudent().getId().equals(actor.getId())
                && !resolveAllowReviewAfterSubmit(attempt.getExam())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Teacher has disabled review for this exam");
        }
    }

    private boolean canViewScore(ExamAttempt attempt, User actor) {
        if (hasRole(actor, RoleName.ADMIN)) return true;
        boolean isTeacher = hasRole(actor, RoleName.TEACHER);
        if (isTeacher && attempt.getExam().getCreatedBy().getId().equals(actor.getId())) return true;
        return attempt.getExam().getShowScoreAfterSubmit() == null || Boolean.TRUE.equals(attempt.getExam().getShowScoreAfterSubmit());
    }

    public AttemptSummaryResponse toSummary(ExamAttempt attempt) {
        boolean isPractice = Boolean.TRUE.equals(attempt.getExam().getPractice());
        ReviewSummary reviewSummary = buildReviewSummary(attempt);
        StudentIdentityCheck latestIdentity = latestIdentityCheck(attempt);
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
                .identityStatus(latestIdentity == null ? "NOT_CHECKED" : latestIdentity.getStatus())
                .identityCheckId(latestIdentity == null ? null : latestIdentity.getId())
                .build();
    }

    private static final int EXPORT_MAX_RECORDS = 10_000;

    @Transactional(readOnly = true)
    public byte[] exportExamAttemptsCsv(Exam exam) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamWithStudent(exam)
                .stream().limit(EXPORT_MAX_RECORDS).toList();
        StringBuilder csv = new StringBuilder();
        csv.append("attemptId,examId,student,status,score,riskScore,suspicious,startedAt,submittedAt,deadlineAt,remainingSeconds\n");

        for (ExamAttempt attempt : attempts) {
            AttemptSummaryResponse summary = toSummary(attempt);
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

    @Transactional(readOnly = true)
    public AttemptEntryStatusResponse getEntryStatus(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);
        return toEntryStatusResponse(attempt);
    }

    @Transactional
    public AttemptEntryStatusResponse recordRulesAgreement(Long attemptId, User student, String clientIp, String userAgent) {
        ExamAttempt attempt = requireAttempt(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId()) && !hasRole(student, RoleName.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to confirm rules for this attempt");
        }
        Exam exam = attempt.getExam();
        attempt.setRulesAgreedAt(VietNamTime.now());
        attempt.setRulesVersionAgreed(resolveRulesVersion(exam));
        attempt.setRulesAgreementIp(normalizeClientIp(clientIp));
        attempt.setRulesAgreementUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 512)));
        examAttemptRepository.save(attempt);
        monitoringService.recordAttemptHistoryEvent(
                attempt,
                MonitoringEventType.RULES_AGREEMENT,
                "rulesVersion=" + safe(attempt.getRulesVersionAgreed()) + ";clientIp=" + safe(attempt.getRulesAgreementIp()));
        return toEntryStatusResponse(attempt);
    }

    private AttemptEntryStatusResponse toEntryStatusResponse(ExamAttempt attempt) {
        StudentIdentityCheck latestIdentity = latestIdentityCheck(attempt);
        String identityStatus = latestIdentity == null ? "NOT_CHECKED" : latestIdentity.getStatus();
        Exam exam = attempt.getExam();
        return AttemptEntryStatusResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .classId(examClassId(exam))
                .className(exam.getClassName())
                .status(attempt.getStatus() == null ? null : attempt.getStatus().name())
                .canStart(entryBlockedReasons(attempt).isEmpty())
                .blockedReasons(entryBlockedReasons(attempt))
                .rulesRequired(rulesAgreementRequired(exam))
                .rulesAccepted(rulesAccepted(attempt))
                .rulesText(resolveRulesText(exam))
                .rulesVersion(resolveRulesVersion(exam))
                .identityRequired(identityRequired(exam))
                .identityStatus(identityStatus)
                .identityCheckId(latestIdentity == null ? null : latestIdentity.getId())
                .identityReviewPolicy(resolveIdentityReviewPolicy(exam))
                .classMembershipRequired(classMembershipRequired(exam))
                .classMembershipValid(studentHasClassAccess(attempt))
                .cameraRequired(cameraRequired(exam))
                .cameraReady(Boolean.TRUE.equals(attempt.getCameraOn()))
                .micReady(Boolean.TRUE.equals(attempt.getMicOn()))
                .inExamIdentityCheckEnabled(inExamIdentityCheckEnabled(exam))
                .identityCheckIntervalSeconds(identityCheckIntervalSeconds(exam))
                .rulesAgreedAt(DateTimeUtils.toOffset(attempt.getRulesAgreedAt(), VietNamTime.zone()))
                .build();
    }

    private StartAttemptResponse toStartResponse(ExamAttempt attempt) {
        StudentIdentityCheck latestIdentity = latestIdentityCheck(attempt);
        boolean started = attempt.getStartedAt() != null;
        Exam exam = attempt.getExam();
        return StartAttemptResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .classId(examClassId(exam))
                .className(exam.getClassName())
                .joinedAt(DateTimeUtils.toOffset(attempt.getJoinedAt(), VietNamTime.zone()))
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .deadlineAt(started ? DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()) : null)
                .remainingSeconds(started ? submissionHelper.remainingSeconds(attempt) : null)
                .status(attempt.getStatus().name())
                .rulesAccepted(rulesAccepted(attempt))
                .identityStatus(latestIdentity == null ? "NOT_CHECKED" : latestIdentity.getStatus())
                .identityCheckId(latestIdentity == null ? null : latestIdentity.getId())
                .classMembershipRequired(classMembershipRequired(exam))
                .classMembershipValid(studentHasClassAccess(attempt))
                .inExamIdentityCheckEnabled(inExamIdentityCheckEnabled(exam))
                .identityCheckIntervalSeconds(identityCheckIntervalSeconds(exam))
                .entryBlockedReasons(entryBlockedReasons(attempt))
                .build();
    }

    private List<String> entryBlockedReasons(ExamAttempt attempt) {
        List<String> reasons = new ArrayList<>();
        Exam exam = attempt.getExam();
        if (rulesAgreementRequired(exam) && !rulesAccepted(attempt)) {
            reasons.add("MISSING_RULES_AGREEMENT");
        }
        if (cameraRequired(exam) && !Boolean.TRUE.equals(attempt.getCameraOn())) {
            reasons.add("CAMERA_NOT_READY");
        }
        if (!studentHasClassAccess(attempt)) {
            reasons.add("CLASS_MEMBERSHIP_REQUIRED");
        }
        if (identityRequired(exam) && !identityAccepted(latestIdentityCheck(attempt), exam)) {
            reasons.add("IDENTITY_NOT_VERIFIED");
        }
        return reasons;
    }

    private boolean rulesAgreementRequired(Exam exam) {
        if (exam != null && Boolean.TRUE.equals(exam.getPractice())) {
            return false;
        }
        return exam == null || exam.getRequireRulesAgreement() == null || Boolean.TRUE.equals(exam.getRequireRulesAgreement());
    }

    private boolean rulesAccepted(ExamAttempt attempt) {
        return !rulesAgreementRequired(attempt.getExam()) || attempt.getRulesAgreedAt() != null;
    }

    private boolean cameraRequired(Exam exam) {
        if (exam != null && Boolean.TRUE.equals(exam.getPractice())) {
            return false;
        }
        return exam != null && (Boolean.TRUE.equals(exam.getRequireCameraMic()) || aiProctoringEnabled(exam));
    }

    private boolean studentHasClassAccess(ExamAttempt attempt) {
        if (attempt == null || attempt.getExam() == null || attempt.getStudent() == null) {
            return true;
        }
        Exam exam = attempt.getExam();
        if (!classMembershipRequired(exam)) {
            return true;
        }
        return classService.findStudentEnrolledClassForExam(attempt.getStudent(), exam).isPresent();
    }

    private void ensureStudentCanEnterExam(Exam exam, User student) {
        if (student == null || hasRole(student, RoleName.ADMIN) || !classMembershipRequired(exam)) {
            return;
        }
        if (classService.findStudentEnrolledClassForExam(student, exam).isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student is not enrolled in the class assigned to this exam");
        }
    }

    private boolean classMembershipRequired(Exam exam) {
        if (exam == null || Boolean.TRUE.equals(exam.getPractice())) {
            return false;
        }
        return exam.getClassEntity() != null || (exam.getClassName() != null && !exam.getClassName().isBlank());
    }

    private Long examClassId(Exam exam) {
        return exam == null || exam.getClassEntity() == null ? null : exam.getClassEntity().getId();
    }

    private boolean identityRequired(Exam exam) {
        if (exam != null && Boolean.TRUE.equals(exam.getPractice())) {
            return false;
        }
        if (exam == null) {
            return true;
        }
        if (exam.getRequireIdentityVerification() != null) {
            return Boolean.TRUE.equals(exam.getRequireIdentityVerification());
        }
        return cameraRequired(exam);
    }

    private boolean identityAccepted(StudentIdentityCheck check, Exam exam) {
        if (!identityRequired(exam)) {
            return true;
        }
        if (check == null || check.getStatus() == null) {
            return false;
        }
        String status = check.getStatus().trim().toUpperCase();
        if ("VERIFIED".equals(status)) {
            return true;
        }
        return "NEEDS_REVIEW".equals(status) && !"STRICT_VERIFIED_ONLY".equals(resolveIdentityReviewPolicy(exam));
    }

    private StudentIdentityCheck latestIdentityCheck(ExamAttempt attempt) {
        if (attempt == null || attempt.getId() == null) {
            return null;
        }
        return studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(attempt.getId()).orElse(null);
    }

    private String resolveRulesText(Exam exam) {
        String text = exam == null ? null : exam.getRulesText();
        if (text != null && !text.isBlank()) {
            return text.trim();
        }
        return String.join("\n",
                "Không sử dụng tài liệu, thiết bị hoặc phần mềm không được phép.",
                "Luôn bật camera và microphone khi kỳ thi yêu cầu giám sát.",
                "Không rời khỏi màn hình thi, không chuyển tab, không sao chép hoặc chia sẻ nội dung đề.",
                "Người làm bài phải đúng danh tính đã xác minh; hệ thống có thể kiểm tra lại trong quá trình thi.",
                "Mọi bất thường sẽ được ghi nhận để giám thị xem xét và xử lý theo quy định.");
    }

    private String resolveRulesVersion(Exam exam) {
        String version = exam == null ? null : exam.getRulesVersion();
        return version == null || version.isBlank() ? "default-v1" : version.trim();
    }

    private String resolveIdentityReviewPolicy(Exam exam) {
        String policy = exam == null ? null : exam.getIdentityReviewPolicy();
        if (policy == null || policy.isBlank()) {
            return "ALLOW_WITH_WARNING";
        }
        String normalized = policy.trim().toUpperCase();
        return "STRICT_VERIFIED_ONLY".equals(normalized) ? normalized : "ALLOW_WITH_WARNING";
    }

    private boolean inExamIdentityCheckEnabled(Exam exam) {
        if (exam == null) {
            return true;
        }
        if (exam.getInExamIdentityCheckEnabled() != null) {
            return Boolean.TRUE.equals(exam.getInExamIdentityCheckEnabled());
        }
        return identityRequired(exam);
    }

    private int identityCheckIntervalSeconds(Exam exam) {
        Integer value = exam == null ? null : exam.getIdentityCheckIntervalSeconds();
        if (value == null) {
            return 60;
        }
        return Math.max(30, Math.min(value, 600));
    }

    private SubmitAttemptResponse toSubmitResponse(ExamAttempt attempt) {
        return SubmitAttemptResponse.builder()
                .attemptId(attempt.getId())
                .score(attempt.getExam().getShowScoreAfterSubmit() == null || Boolean.TRUE.equals(attempt.getExam().getShowScoreAfterSubmit()) ? attempt.getScore() : null)
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .showScoreAfterSubmit(attempt.getExam().getShowScoreAfterSubmit() == null || Boolean.TRUE.equals(attempt.getExam().getShowScoreAfterSubmit()))
                .allowReviewAfterSubmit(resolveAllowReviewAfterSubmit(attempt.getExam()))
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
