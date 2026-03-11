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
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.helper.SubmissionHelper;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public StartAttemptResponse startAttempt(Exam exam, User student, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        examService.validateExamAvailability(exam, now);

        ExamAttempt existing = examAttemptRepository
                .findFirstByExamAndStudentAndStatus(exam, student, AttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (existing != null) {
            return toStartResponse(existing);
        }

        ExamAttempt saved = examAttemptRepository.save(
                ExamAttempt.builder()
                        .exam(exam)
                        .student(student)
                        .startedAt(LocalDateTime.now())
                        .status(AttemptStatus.IN_PROGRESS)
                        .score(0.0)
                        .riskScore(0)
                        .suspicious(false)
                        .clientIp(clientIp)
                        .build());

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

        // Use Helper
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            autoSubmitFromDraft(attempt, now);
            return toSubmitResponse(attempt);
        }

        submissionHelper.saveAnswers(attempt, request.getAnswers());

        attempt.setScore(submissionHelper.calculateScore(attempt));
        attempt.setSubmittedAt(now);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        examAttemptRepository.save(attempt);

        return toSubmitResponse(attempt);
    }

    @Transactional
    public DraftSaveResponse saveDraftAnswers(Long attemptId, User student, List<AnswerInput> answers) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        LocalDateTime now = LocalDateTime.now();

        // Use Helper
        if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
            autoSubmitFromDraft(attempt, now);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt time is over");
        }

        submissionHelper.saveAnswers(attempt, answers);

        return DraftSaveResponse.builder()
                .attemptId(attempt.getId())
                .savedAt(now)
                .answeredCount(answerRepository.findByAttempt(attempt).size())
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
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
                .deadlineAt(submissionHelper.deadlineAt(attempt))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .answers(answers)
                .build();
    }

    public AttemptDetailResponse getAttemptDetail(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanViewAttempt(attempt, actor);

        int answeredCount = answerRepository.findByAttempt(attempt).size();
        int totalQuestions = questionRepository.findByExam(attempt.getExam()).size();

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .deadlineAt(submissionHelper.deadlineAt(attempt))
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
                        .selectedAnswer(answer.getSelectedAnswer())
                        .correct(answer.getCorrect())
                        .scoreWeight(answer.getQuestion().getScoreWeight())
                        .build())
                .toList();

        return AttemptReportResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .deadlineAt(submissionHelper.deadlineAt(attempt))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
                .answeredCount(answers.size())
                .correctCount(correctCount)
                .answers(answerRows)
                .build();
    }

    public List<ExamAttempt> listByStudent(User student) {
        return examAttemptRepository.findByStudent(student);
    }

    public List<ExamAttempt> listByExam(Exam exam) {
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

        Page<ExamAttempt> filteredPage = examAttemptRepository.searchByExam(
                exam,
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
        return AttemptSummaryResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .student(attempt.getStudent().getUsername())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .deadlineAt(submissionHelper.deadlineAt(attempt))
                .remainingSeconds(submissionHelper.remainingSeconds(attempt))
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
        return StartAttemptResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .startedAt(attempt.getStartedAt())
                .deadlineAt(submissionHelper.deadlineAt(attempt))
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
                .submittedAt(attempt.getSubmittedAt())
                .deadlineAt(submissionHelper.deadlineAt(attempt))
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

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }
}
