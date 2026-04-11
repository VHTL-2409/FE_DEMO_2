package com.example.demo.service;

import com.example.demo.api.dto.submission.AnswerInput;
import com.example.demo.api.dto.submission.AttemptDetailResponse;
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
import com.example.demo.common.ApiException;
import com.example.demo.common.DateTimeUtils;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.QuestionRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    @Value("${demo.monitoring.integrity-check.cooldown-seconds:45}")
    private long integrityCheckCooldownSeconds;

    @Transactional
    public StartAttemptResponse startAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = VietNamTime.now();
        examService.validateExamAvailability(exam, nowInExamZone);

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
            return toStartResponse(existing);
        }

        String normalizedIp = normalizeClientIp(clientIp);

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

        return toStartResponse(saved);
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(Long attemptId, User student, SubmitAttemptRequest request) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            return toSubmitResponse(attempt);
        }

        if (attempt.getStatus() == AttemptStatus.STOPPED) {
            if (VietNamTime.now().isAfter(submissionHelper.deadlineAt(attempt))) {
                attempt.setScore(submissionHelper.calculateScore(attempt));
                attempt.setSubmittedAt(VietNamTime.now());
                attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
                examAttemptRepository.save(attempt);
                return toSubmitResponse(attempt);
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam attempt has been suspended by proctor");
        }
        if (attempt.getStatus() == AttemptStatus.PAUSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam attempt is paused pending proctor review");
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
        examAttemptRepository.save(attempt);

        detectAndLogFastSubmit(attempt, now);

        return toSubmitResponse(attempt);
    }

    @Transactional
    public DraftSaveResponse saveDraftAnswers(Long attemptId, User student, String clientIp, List<AnswerInput> answers) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, student)
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
            autoSubmitFromDraft(attempt, now);
            return DraftSaveResponse.builder()
                    .attemptId(attempt.getId())
                    .savedAt(DateTimeUtils.toOffset(now, VietNamTime.zone()))
                    .answeredCount(answerRepository.findByAttempt(attempt).size())
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
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, student)
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
        return examAttemptRepository.findByStudent(student).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Giáo viên / admin xem danh sách lượt làm theo đề — cùng lý do {@link #listAttemptSummariesForStudent}.
     */
    private static final int LIST_EXAM_ATTEMPTS_MAX = 10_000;

    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listAttemptSummariesForExam(Exam exam) {
        return examAttemptRepository.findByExamWithStudent(exam).stream()
                .limit(LIST_EXAM_ATTEMPTS_MAX)
                .map(this::toSummary)
                .toList();
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
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt")));

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

    public AttemptSummaryResponse toSummary(ExamAttempt attempt) {
        boolean isPractice = attempt.getExam().getCreatedBy().getId().equals(attempt.getStudent().getId());
        ReviewSummary reviewSummary = buildReviewSummary(attempt);
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

    private StartAttemptResponse toStartResponse(ExamAttempt attempt) {
        return StartAttemptResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), VietNamTime.zone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .status(attempt.getStatus().name())
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
        examAttemptRepository.save(attempt);
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

    private ReviewSummary buildReviewSummary(ExamAttempt attempt) {
        int violationCount = Math.toIntExact(fraudSignalRepository.countByAttempt(attempt));
        int warningCount = Math.max(violationCount, attempt.getRiskScore() != null ? attempt.getRiskScore() : 0);
        String recommendedAction = switch (attempt.getRiskLevel() != null ? attempt.getRiskLevel() : RiskLevel.CLEAN) {
            case CRITICAL -> "PAUSE_AND_REVIEW";
            case HIGH_RISK -> "WARN_AND_ESCALATE";
            case SUSPICIOUS -> "REVIEW_ATTEMPT";
            case CLEAN -> "CONTINUE_MONITORING";
        };
        boolean reviewRequired = Boolean.TRUE.equals(attempt.getSuspicious()) || attempt.getStatus() == AttemptStatus.PAUSED;
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
        List<String> evidenceSummary = new ArrayList<>();
        if (attempt.getClientIp() != null && !attempt.getClientIp().isBlank()) {
            evidenceSummary.add("IP hiện tại: " + attempt.getClientIp());
        }
        if (attempt.getDeviceFingerprint() != null && !attempt.getDeviceFingerprint().isBlank()) {
            evidenceSummary.add("Fingerprint thiết bị đã được ghi nhận");
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
