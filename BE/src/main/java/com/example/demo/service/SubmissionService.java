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
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

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

    @Transactional
    public StartAttemptResponse startAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime nowInExamZone = LocalDateTime.now(DateTimeUtils.toZoneId(exam.getTimezone()));
        examService.validateExamAvailability(exam, nowInExamZone);

        ExamAttempt existing = examAttemptRepository
                .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (existing != null) {
            enforceIpConsistency(existing, normalizeClientIp(clientIp));
            return toStartResponse(existing);
        }

        String normalizedIp = normalizeClientIp(clientIp);

        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
                        .startedAt(LocalDateTime.now())
                        .status(AttemptStatus.IN_PROGRESS)
                        .score(0.0)
                        .riskScore(0)
                        .suspicious(false)
                        .clientIp(normalizedIp)
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam attempt has been suspended by proctor");
        }

        LocalDateTime now = LocalDateTime.now();
        Exam exam = attempt.getExam();
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has ended");
        }

        // Use Helper - khi hết hạn: lưu đáp án hiện tại rồi tự động nộp
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                submissionHelper.saveAnswers(attempt, request.getAnswers());
            }
            autoSubmitFromDraft(attempt, now);
            return toSubmitResponse(attempt);
        }

        submissionHelper.saveAnswers(attempt, request.getAnswers());

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

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        LocalDateTime now = LocalDateTime.now();

        // Use Helper - khi hết hạn: lưu đáp án rồi tự động nộp, không throw
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            submissionHelper.saveAnswers(attempt, answers);
            autoSubmitFromDraft(attempt, now);
            return DraftSaveResponse.builder()
                    .attemptId(attempt.getId())
                    .savedAt(DateTimeUtils.toOffset(now, attempt.getExam().getTimezone()))
                    .answeredCount(answerRepository.findByAttempt(attempt).size())
                    .remainingSeconds(0L)
                    .build();
        }

        String normalizedIp = normalizeClientIp(clientIp);
        enforceIpConsistency(attempt, normalizedIp);

        submissionHelper.saveAnswers(attempt, answers);
        duplicateIpDetectionService.detect(attempt);

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        long remainingSeconds = submissionHelper.remainingSeconds(attempt);

        realtimeNotificationService.notifyDraftSaved(attempt, answeredCount, remainingSeconds);

        return DraftSaveResponse.builder()
                .attemptId(attempt.getId())
                .savedAt(DateTimeUtils.toOffset(now, attempt.getExam().getTimezone()))
                .answeredCount(answeredCount)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    public DraftAnswersResponse getDraftAnswers(Long attemptId, User student) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        List<DraftAnswerItem> answers = answerRepository.findByAttempt(attempt).stream()
                .map(answer -> DraftAnswerItem.builder()
                        .questionId(answer.getQuestion().getId())
                        .selectedAnswer(answer.getSelectedAnswer())
                        .build())
                .toList();

        return DraftAnswersResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), attempt.getExam().getTimezone()))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .answers(answers)
                .build();
    }

    public AttemptDetailResponse getAttemptDetail(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        int totalQuestions = questionRepository.findByExam(attempt.getExam()).size();

        String tz = attempt.getExam().getTimezone();
        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), tz))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), tz))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), tz))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .examTitle(attempt.getExam().getTitle())
                .answeredCount(answeredCount)
                .totalQuestions(totalQuestions)
                .build();
    }

    public AttemptReportResponse getAttemptReport(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        List<Answer> answers = answerRepository.findByAttempt(attempt);
        long correctCount = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();

        List<AttemptReportAnswerItem> answerRows = answers.stream()
                .map(answer -> AttemptReportAnswerItem.builder()
                        .questionId(answer.getQuestion().getId())
                        .question(answer.getQuestion().getContent())
                        .options(answer.getQuestion().getOptions())
                        .selectedAnswer(answer.getSelectedAnswer())
                        .correctAnswer(answer.getQuestion().getCorrectAnswer())
                        .correct(answer.getCorrect())
                        .scoreWeight(answer.getQuestion().getScoreWeight())
                        .build())
                .toList();

        String tz = attempt.getExam().getTimezone();
        return AttemptReportResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), tz))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), tz))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), tz))
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
     * Danh sách attempts của đợt thi hiện tại (theo exam.startTime, exam.endTime).
     * Khi tạo đợt thi mới, chỉ lấy attempts trong khoảng thời gian đó, không lấy dữ liệu cũ.
     */
    public List<ExamAttempt> listByExam(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime());
        }
        return examAttemptRepository.findByExam(exam);
    }

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
                exam,
                sessionFrom,
                sessionTo,
                parsedStatus,
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
        return examAttemptRepository.findById(attemptId)
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
        String tz = attempt.getExam().getTimezone();
        return AttemptSummaryResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .examTitle(attempt.getExam().getTitle())
                .isPractice(isPractice)
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), tz))
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), tz))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), tz))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .cameraOn(attempt.getCameraOn())
                .micOn(attempt.getMicOn())
                .build();
    }

    public byte[] exportExamAttemptsCsv(Exam exam) {
        List<ExamAttempt> attempts = listByExam(exam);
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
        String tz = attempt.getExam().getTimezone();
        return StartAttemptResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .startedAt(DateTimeUtils.toOffset(attempt.getStartedAt(), tz))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), tz))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .status(attempt.getStatus().name())
                .build();
    }

    private SubmitAttemptResponse toSubmitResponse(ExamAttempt attempt) {
        String tz = attempt.getExam().getTimezone();
        return SubmitAttemptResponse.builder()
                .attemptId(attempt.getId())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .submittedAt(DateTimeUtils.toOffset(attempt.getSubmittedAt(), tz))
                .deadlineAt(DateTimeUtils.toOffset(submissionHelper.deadlineAt(attempt), tz))
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

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }
}
