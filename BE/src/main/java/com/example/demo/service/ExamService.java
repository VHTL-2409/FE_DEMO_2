package com.example.demo.service;

import com.example.demo.api.dto.exam.ExamRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.api.dto.exam.PracticeExamRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.AssignmentRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.RiskSnapshotRepository;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private static final String EXAM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int EXAM_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;

    @Transactional
    public ExamResponse createExam(ExamRequest request, User teacher) {
        validateExamRequest(request);

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .code(generateUniqueExamCode())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .createdBy(teacher)
                .build();
        return toResponse(examRepository.save(exam));
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listExams(User currentUser) {
        boolean isTeacher = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.TEACHER || r.getName() == RoleName.ADMIN);

        return examRepository.findAllWithCreatedBy().stream()
                .filter(e -> {
                    if (isTeacher) {
                        return e.getCreatedBy().getId().equals(currentUser.getId());
                    } else {
                        // Student sees exams created by Teachers OR created by themselves (practice)
                        boolean creatorIsTeacher = e.getCreatedBy().getRoles().stream()
                                .anyMatch(r -> r.getName() == RoleName.TEACHER || r.getName() == RoleName.ADMIN);
                        return creatorIsTeacher || e.getCreatedBy().getId().equals(currentUser.getId());
                    }
                })
                .map(this::toResponse).toList();
    }

    @Transactional
    public ExamResponse generatePracticeExam(User student, PracticeExamRequest request) {
        int questionCount = request == null || request.getQuestionCount() == null ? 20 : request.getQuestionCount();
        int durationMinutes = request == null || request.getDurationMinutes() == null ? 30 : request.getDurationMinutes();

        Exam practiceExam = Exam.builder()
                .title("Luyện Tập - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .code(generateUniqueExamCode())
                .description("Bài thi luyện tập ngẫu nhiên. (Hệ thống tự tạo)")
                .durationMinutes(durationMinutes)
                .isActive(true)
                .createdBy(student)
                .build();
        practiceExam = examRepository.save(practiceExam);

        List<Question> randQs = questionRepository.findRandomQuestions(questionCount);
        for (Question q : randQs) {
            Question newQ = Question.builder()
                    .exam(practiceExam)
                    .content(q.getContent())
                    .scoreWeight(q.getScoreWeight())
                    .options(q.getOptions())
                    .correctAnswer(q.getCorrectAnswer())
                    .build();
            questionRepository.save(newQ);
        }
        return toResponse(practiceExam);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(Long examId) {
        return toResponse(requireExam(examId));
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamForUser(Long examId, User actor) {
        return toResponse(requireAccessibleExam(examId, actor));
    }

    @Transactional(readOnly = true)
    public ExamResponse resolveJoinableExam(String query, User actor) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query is required");
        }

        String trimmed = query.trim();
        Exam exam = examRepository.findFirstByCodeIgnoreCase(trimmed)
                .or(() -> {
                    try {
                        Long examId = Long.parseLong(trimmed);
                        return examRepository.findById(examId);
                    } catch (NumberFormatException ex) {
                        return java.util.Optional.empty();
                    }
                })
                .or(() -> examRepository.findFirstByTitleContainingIgnoreCase(trimmed))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));

        Exam accessibleExam = requireAccessibleExam(exam.getId(), actor);
        validateExamAvailability(accessibleExam, LocalDateTime.now());
        return toResponse(accessibleExam);
    }

    @Transactional
    public ExamResponse updateExam(Long examId, ExamRequest request, User actor) {
        validateExamRequest(request);

        Exam exam = requireManageableExam(examId, actor);
        exam.setTitle(request.getTitle());
        if (exam.getCode() == null || exam.getCode().isBlank()) {
            exam.setCode(generateUniqueExamCode());
        }
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setIsActive(request.getIsActive() == null ? exam.getIsActive() : request.getIsActive());
        return toResponse(examRepository.save(exam));
    }

    @Transactional
    public void deleteExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);

        try {
            riskSnapshotRepository.deleteByExam(exam);
            monitoringEventRepository.deleteByExam(exam);
            answerRepository.deleteByExam(exam);
            examAttemptRepository.deleteByExam(exam);

            assignmentRepository.deleteByExam(exam);
            questionRepository.deleteByExam(exam);
            examRepository.delete(exam);
        } catch (DataAccessException | PersistenceException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không thể xóa đề thi vì còn dữ liệu liên quan. Vui lòng thử lại hoặc liên hệ quản trị viên.");
        }
    }

    public Exam requireExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    public Exam requireManageableExam(Long examId, User actor) {
        Exam exam = requireExam(examId);
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin) {
            return exam;
        }
        if (!exam.getCreatedBy().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this exam");
        }
        return exam;
    }

    public Exam requireAccessibleExam(Long examId, User actor) {
        Exam exam = requireExam(examId);
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin) {
            return exam;
        }

        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        if (isTeacher) {
            if (!exam.getCreatedBy().getId().equals(actor.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this exam");
            }
            return exam;
        }

        boolean creatorIsTeacher = exam.getCreatedBy().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.TEACHER || role.getName() == RoleName.ADMIN);
        if (!(creatorIsTeacher || exam.getCreatedBy().getId().equals(actor.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this exam");
        }
        return exam;
    }

    public void validateExamAvailability(Exam exam, LocalDateTime now) {
        if (Boolean.FALSE.equals(exam.getIsActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam is not active");
        }

        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has not started yet");
        }

        if (exam.getEndTime() != null && !now.isBefore(exam.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has ended");
        }
    }

    private void validateExamRequest(ExamRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getStartTime().isBefore(request.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startTime must be strictly before endTime");
        }
    }

    private String generateUniqueExamCode() {
        String code;
        do {
            code = randomCode();
        } while (examRepository.existsByCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(EXAM_CODE_LENGTH);
        for (int i = 0; i < EXAM_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(EXAM_CODE_CHARS.length());
            builder.append(EXAM_CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private ExamResponse toResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .durationMinutes(exam.getDurationMinutes())
                .isActive(exam.getIsActive())
                .createdBy(exam.getCreatedBy() == null ? null : exam.getCreatedBy().getUsername())
                .questionCount(questionRepository.countByExam(exam)) // We keep this simple count query per row
                .build();
    }
}
