package com.example.demo.service;

import com.example.demo.api.dto.assignment.NewSessionRequest;
import com.example.demo.api.dto.exam.ExamRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.api.dto.exam.PracticeExamRequest;
import com.example.demo.api.dto.exam.QuestionWrongStatsItem;
import com.example.demo.api.dto.exam.WaitingStudentResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Assignment;
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.AssignmentRepository;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.RiskSnapshotRepository;
import com.example.demo.service.ImportXlsxService;
import jakarta.persistence.PersistenceException;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.DateTimeUtils;
import com.example.demo.common.VietNamTime;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private static final String EXAM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int EXAM_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PRACTICE_EXAM_TITLE_PREFIX = "Luyện Tập - ";

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(VN).toOffsetDateTime();
    }

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final AuditLogRepository auditLogRepository;
    private final ImportXlsxService importXlsxService;
    private final CurrentUserService currentUserService;
    private final ClassService classService;

    @Transactional
    public ExamResponse createExam(ExamRequest request, User teacher) {
        currentUserService.requireTeacherOrAdmin(teacher);
        validateExamRequest(request);
        ensureAssignedClassBelongsToTeacher(request.getClassName(), teacher);

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .code(generateUniqueExamCode())
                .description(request.getDescription())
                .className(normalizeClassName(request.getClassName()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .createdBy(teacher)
                .monitorTabSwitch(Boolean.TRUE.equals(request.getMonitorTabSwitch()) || request.getMonitorTabSwitch() == null)
                .monitorBlur(Boolean.TRUE.equals(request.getMonitorBlur()) || request.getMonitorBlur() == null)
                .monitorExitFullscreen(Boolean.TRUE.equals(request.getMonitorExitFullscreen()) || request.getMonitorExitFullscreen() == null)
                .monitorCopyPaste(Boolean.TRUE.equals(request.getMonitorCopyPaste()) || request.getMonitorCopyPaste() == null)
                .monitorIdleTime(Boolean.TRUE.equals(request.getMonitorIdleTime()) || request.getMonitorIdleTime() == null)
                .monitorDevtools(Boolean.TRUE.equals(request.getMonitorDevtools()) || request.getMonitorDevtools() == null)
                .monitorDuplicateIp(Boolean.TRUE.equals(request.getMonitorDuplicateIp()) || request.getMonitorDuplicateIp() == null)
                .monitorFastSubmit(Boolean.TRUE.equals(request.getMonitorFastSubmit()) || request.getMonitorFastSubmit() == null)
                .monitorRightClick(Boolean.TRUE.equals(request.getMonitorRightClick()) || request.getMonitorRightClick() == null)
                .monitorPrintScreen(Boolean.TRUE.equals(request.getMonitorPrintScreen()) || request.getMonitorPrintScreen() == null)
                .monitorRapidQuestionSwitch(Boolean.TRUE.equals(request.getMonitorRapidQuestionSwitch()) || request.getMonitorRapidQuestionSwitch() == null)
                .monitorMultiMonitor(Boolean.TRUE.equals(request.getMonitorMultiMonitor()) || request.getMonitorMultiMonitor() == null)
                .requireCameraMic(Boolean.TRUE.equals(request.getRequireCameraMic()) || request.getRequireCameraMic() == null)
                .monitorNetworkInstability(Boolean.TRUE.equals(request.getMonitorNetworkInstability()) || request.getMonitorNetworkInstability() == null)
                .monitorSessionRecovery(Boolean.TRUE.equals(request.getMonitorSessionRecovery()) || request.getMonitorSessionRecovery() == null)
                .monitorQuestionTimingAnomaly(Boolean.TRUE.equals(request.getMonitorQuestionTimingAnomaly()) || request.getMonitorQuestionTimingAnomaly() == null)
                .monitorAnswerChangeBurst(Boolean.TRUE.equals(request.getMonitorAnswerChangeBurst()) || request.getMonitorAnswerChangeBurst() == null)
                .monitorClipboardBurst(Boolean.TRUE.equals(request.getMonitorClipboardBurst()) || request.getMonitorClipboardBurst() == null)
                .monitorFullscreenEvasion(Boolean.TRUE.equals(request.getMonitorFullscreenEvasion()) || request.getMonitorFullscreenEvasion() == null)
                .monitorAnswerSimilarity(Boolean.TRUE.equals(request.getMonitorAnswerSimilarity()) || request.getMonitorAnswerSimilarity() == null)
                .monitorIpFingerprintGraph(Boolean.TRUE.equals(request.getMonitorIpFingerprintGraph()) || request.getMonitorIpFingerprintGraph() == null)
                .enableAiProctoring(Boolean.TRUE.equals(request.getEnableAiProctoring()))
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .shuffleAnswers(Boolean.TRUE.equals(request.getShuffleAnswers()))
                .practice(false)
                .build();
        return toResponse(examRepository.save(exam));
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listExams(User currentUser) {
        boolean isTeacher = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.TEACHER || r.getName() == RoleName.ADMIN);

        if (isTeacher) {
            List<Exam> exams = examRepository.findByCreatedById(currentUser.getId());
            if (exams.isEmpty()) {
                return List.of();
            }
            List<Long> examIds = exams.stream().map(Exam::getId).toList();
            Map<Long, Long> participantByExam = participantCountsByExamIds(examIds);
            Map<Long, Long> questionCountByExam = questionCountsByExamIds(examIds);
            return exams.stream()
                    .map(e -> toResponse(e, participantByExam.getOrDefault(e.getId(), 0L), questionCountByExam.getOrDefault(e.getId(), 0L)))
                    .toList();
        }

        List<Exam> publishedExams = assignmentRepository.findDistinctPublishedExams().stream()
                .filter(exam -> canStudentAccessExam(currentUser, exam))
                .toList();
        List<Exam> practiceExams = examRepository.findByCreatedBy(currentUser);
        List<Exam> combined = new java.util.ArrayList<>(publishedExams);
        for (Exam exam : practiceExams) {
            if (combined.stream().noneMatch(existing -> existing.getId().equals(exam.getId()))) {
                combined.add(exam);
            }
        }
        if (combined.isEmpty()) {
            return List.of();
        }
        List<Long> examIds = combined.stream().map(Exam::getId).toList();
        Map<Long, Long> participantByExam = participantCountsByExamIds(examIds);
        Map<Long, Long> questionCountByExam = questionCountsByExamIds(examIds);
        return combined.stream()
                .map(e -> toResponse(e, participantByExam.getOrDefault(e.getId(), 0L), questionCountByExam.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listPublishedExamsByTeacher(User teacher) {
        if (teacher == null) return List.of();
        List<Exam> exams = assignmentRepository.findDistinctPublishedExamsByTeacher(teacher);
        if (exams.isEmpty()) return List.of();
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, Long> participantByExam = participantCountsByExamIds(examIds);
        Map<Long, Long> questionCountByExam = questionCountsByExamIds(examIds);
        return exams.stream()
                .map(e -> toResponse(e, participantByExam.getOrDefault(e.getId(), 0L), questionCountByExam.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listPublishedExamsByTeacherAndClassName(User teacher, String className) {
        if (teacher == null || className == null || className.isBlank()) {
            return List.of();
        }
        List<Exam> exams = assignmentRepository.findDistinctPublishedExamsByTeacherAndClassName(teacher, className.trim());
        if (exams.isEmpty()) return List.of();
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, Long> participantByExam = participantCountsByExamIds(examIds);
        Map<Long, Long> questionCountByExam = questionCountsByExamIds(examIds);
        return exams.stream()
                .map(e -> toResponse(e, participantByExam.getOrDefault(e.getId(), 0L), questionCountByExam.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> participantCountsByExamIds(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = examAttemptRepository.countDistinctStudentsGroupedByExamIds(examIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, Long> questionCountsByExamIds(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = questionRepository.countQuestionsGroupedByExamIds(examIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPracticeOptions() {
        long totalQuestions = questionRepository.count();
        int maxQuestions = (int) Math.min(Math.max(0, totalQuestions), 50);
        return Map.of("maxQuestions", maxQuestions);
    }

    @Transactional
    public ExamResponse createPracticeExamFromFile(User student, MultipartFile file, int durationMinutes,
            Integer questionCount) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp CSV hoặc XLSX chứa câu hỏi.");
        }
        int dur = Math.max(5, Math.min(240, durationMinutes));

        Exam practiceExam = Exam.builder()
                .title("Luyện Tập - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .code(generateUniqueExamCode())
                .description("Bài thi luyện tập từ tệp. (Hệ thống tự tạo)")
                .durationMinutes(dur)
                .isActive(true)
                .createdBy(student)
                .practice(true)
                .build();
        practiceExam = examRepository.save(practiceExam);

        int count = importXlsxService.importQuestions(practiceExam, file, questionCount);
        if (count <= 0) {
            examRepository.delete(practiceExam);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tệp không chứa câu hỏi hợp lệ. Vui lòng kiểm tra định dạng (CSV/XLSX) và thử lại.");
        }

        return toResponse(practiceExam);
    }

    @Transactional
    public ExamResponse generatePracticeExam(User student, PracticeExamRequest request) {
        int requestedCount = request == null || request.getQuestionCount() == null ? 20 : request.getQuestionCount();
        int durationMinutes = request == null || request.getDurationMinutes() == null ? 30 : request.getDurationMinutes();

        long availableCount = questionRepository.count();
        int questionCount = (int) Math.min(Math.max(0, requestedCount), Math.max(0, availableCount));
        if (questionCount <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ngân hàng câu hỏi trống. Giáo viên cần thêm câu hỏi (từ file hoặc nhập tay) trước khi sinh viên có thể luyện tập.");
        }

        Exam practiceExam = Exam.builder()
                .title("Luyện Tập - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .code(generateUniqueExamCode())
                .description("Bài thi luyện tập ngẫu nhiên. (Hệ thống tự tạo)")
                .durationMinutes(durationMinutes)
                .isActive(true)
                .createdBy(student)
                .practice(true)
                .build();
        practiceExam = examRepository.save(practiceExam);

        List<Question> randQs = questionRepository.findRandomQuestions(questionCount);
        for (Question q : randQs) {
            Question newQ = Question.builder()
                    .exam(practiceExam)
                    .content(q.getContent())
                    .type(q.getType())
                    .scoreWeight(q.getScoreWeight())
                    .options(q.getOptions())
                    .correctAnswer(q.getCorrectAnswer())
                    .difficulty(q.getDifficulty())
                    .metadata(q.getMetadata())
                    .attachments(q.getAttachments())
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
    public List<QuestionWrongStatsItem> getQuestionWrongStats(Long examId, User actor) {
        currentUserService.requireTeacherOrAdmin(actor);
        Exam exam = requireManageableExam(examId, actor);
        List<Answer> answers;
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            answers = answerRepository.findByExamSubmittedAttemptsInSession(
                    exam, exam.getStartTime(), exam.getEndTime());
        } else {
            answers = answerRepository.findByExamSubmittedAttempts(exam);
        }
        if (answers.isEmpty()) {
            return List.of();
        }
        Map<Long, java.util.List<Answer>> byQuestion = answers.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getQuestion().getId()));
        List<QuestionWrongStatsItem> items = new java.util.ArrayList<>();
        for (Map.Entry<Long, java.util.List<Answer>> e : byQuestion.entrySet()) {
            List<Answer> list = e.getValue();
            long wrongCount = list.stream().filter(a -> !Boolean.TRUE.equals(a.getCorrect())).count();
            long correctCount = list.size() - wrongCount;
            double wrongRate = list.isEmpty() ? 0 : (wrongCount * 100.0 / list.size());
            String content = list.isEmpty() ? "" : list.get(0).getQuestion().getContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 197) + "...";
            }
            items.add(QuestionWrongStatsItem.builder()
                    .questionId(e.getKey())
                    .questionContent(content != null ? content : "")
                    .totalAnswered(list.size())
                    .wrongCount((int) wrongCount)
                    .correctCount((int) correctCount)
                    .wrongRatePercent(Math.round(wrongRate * 10) / 10.0)
                    .rank(0)
                    .build());
        }
        items.sort((a, b) -> Integer.compare(b.getWrongCount(), a.getWrongCount()));
        List<QuestionWrongStatsItem> ranked = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            QuestionWrongStatsItem it = items.get(i);
            ranked.add(QuestionWrongStatsItem.builder()
                    .questionId(it.getQuestionId())
                    .questionContent(it.getQuestionContent())
                    .totalAnswered(it.getTotalAnswered())
                    .wrongCount(it.getWrongCount())
                    .correctCount(it.getCorrectCount())
                    .wrongRatePercent(it.getWrongRatePercent())
                    .rank(i + 1)
                    .build());
        }
        return ranked;
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
        validateExamJoinable(accessibleExam, LocalDateTime.now(VietNamTime.zone()));
        return toResponse(accessibleExam);
    }

    @Transactional
    public ExamResponse updateExam(Long examId, ExamRequest request, User actor) {
        validateExamRequest(request);

        Exam exam = requireManageableExam(examId, actor);
        ensureAssignedClassBelongsToTeacher(request.getClassName(), exam.getCreatedBy());

        LocalDateTime nowInExamZone = LocalDateTime.now(VietNamTime.zone());
        if (exam.getStartTime() != null && !nowInExamZone.isBefore(exam.getStartTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không thể chỉnh sửa đề thi đã bắt đầu. Chỉ cho phép sửa đề thi chưa đến thời gian bắt đầu.");
        }
        exam.setTitle(request.getTitle());
        if (exam.getCode() == null || exam.getCode().isBlank()) {
            exam.setCode(generateUniqueExamCode());
        }
        exam.setDescription(request.getDescription());
        exam.setClassName(normalizeClassName(request.getClassName()));
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setIsActive(request.getIsActive() == null ? exam.getIsActive() : request.getIsActive());
        if (request.getMonitorTabSwitch() != null) {
            exam.setMonitorTabSwitch(request.getMonitorTabSwitch());
        } else if (exam.getMonitorTabSwitch() == null) {
            exam.setMonitorTabSwitch(Boolean.TRUE);
        }
        if (request.getMonitorBlur() != null) {
            exam.setMonitorBlur(request.getMonitorBlur());
        } else if (exam.getMonitorBlur() == null) {
            exam.setMonitorBlur(Boolean.TRUE);
        }
        if (request.getMonitorExitFullscreen() != null) {
            exam.setMonitorExitFullscreen(request.getMonitorExitFullscreen());
        } else if (exam.getMonitorExitFullscreen() == null) {
            exam.setMonitorExitFullscreen(Boolean.TRUE);
        }
        if (request.getMonitorCopyPaste() != null) {
            exam.setMonitorCopyPaste(request.getMonitorCopyPaste());
        } else if (exam.getMonitorCopyPaste() == null) {
            exam.setMonitorCopyPaste(Boolean.TRUE);
        }
        if (request.getMonitorIdleTime() != null) {
            exam.setMonitorIdleTime(request.getMonitorIdleTime());
        } else if (exam.getMonitorIdleTime() == null) {
            exam.setMonitorIdleTime(Boolean.TRUE);
        }
        if (request.getMonitorDevtools() != null) {
            exam.setMonitorDevtools(request.getMonitorDevtools());
        } else if (exam.getMonitorDevtools() == null) {
            exam.setMonitorDevtools(Boolean.TRUE);
        }
        if (request.getMonitorDuplicateIp() != null) {
            exam.setMonitorDuplicateIp(request.getMonitorDuplicateIp());
        } else if (exam.getMonitorDuplicateIp() == null) {
            exam.setMonitorDuplicateIp(Boolean.TRUE);
        }
        if (request.getMonitorFastSubmit() != null) {
            exam.setMonitorFastSubmit(request.getMonitorFastSubmit());
        } else if (exam.getMonitorFastSubmit() == null) {
            exam.setMonitorFastSubmit(Boolean.TRUE);
        }
        if (request.getMonitorRightClick() != null) {
            exam.setMonitorRightClick(request.getMonitorRightClick());
        } else if (exam.getMonitorRightClick() == null) {
            exam.setMonitorRightClick(Boolean.TRUE);
        }
        if (request.getMonitorPrintScreen() != null) {
            exam.setMonitorPrintScreen(request.getMonitorPrintScreen());
        } else if (exam.getMonitorPrintScreen() == null) {
            exam.setMonitorPrintScreen(Boolean.TRUE);
        }
        if (request.getMonitorRapidQuestionSwitch() != null) {
            exam.setMonitorRapidQuestionSwitch(request.getMonitorRapidQuestionSwitch());
        } else if (exam.getMonitorRapidQuestionSwitch() == null) {
            exam.setMonitorRapidQuestionSwitch(Boolean.TRUE);
        }
        if (request.getMonitorMultiMonitor() != null) {
            exam.setMonitorMultiMonitor(request.getMonitorMultiMonitor());
        } else if (exam.getMonitorMultiMonitor() == null) {
            exam.setMonitorMultiMonitor(Boolean.TRUE);
        }
        if (request.getRequireCameraMic() != null) {
            exam.setRequireCameraMic(request.getRequireCameraMic());
        } else if (exam.getRequireCameraMic() == null) {
            exam.setRequireCameraMic(Boolean.TRUE);
        }
        if (request.getMonitorNetworkInstability() != null) {
            exam.setMonitorNetworkInstability(request.getMonitorNetworkInstability());
        } else if (exam.getMonitorNetworkInstability() == null) {
            exam.setMonitorNetworkInstability(Boolean.TRUE);
        }
        if (request.getMonitorSessionRecovery() != null) {
            exam.setMonitorSessionRecovery(request.getMonitorSessionRecovery());
        } else if (exam.getMonitorSessionRecovery() == null) {
            exam.setMonitorSessionRecovery(Boolean.TRUE);
        }
        if (request.getMonitorQuestionTimingAnomaly() != null) {
            exam.setMonitorQuestionTimingAnomaly(request.getMonitorQuestionTimingAnomaly());
        } else if (exam.getMonitorQuestionTimingAnomaly() == null) {
            exam.setMonitorQuestionTimingAnomaly(Boolean.TRUE);
        }
        if (request.getMonitorAnswerChangeBurst() != null) {
            exam.setMonitorAnswerChangeBurst(request.getMonitorAnswerChangeBurst());
        } else if (exam.getMonitorAnswerChangeBurst() == null) {
            exam.setMonitorAnswerChangeBurst(Boolean.TRUE);
        }
        if (request.getMonitorClipboardBurst() != null) {
            exam.setMonitorClipboardBurst(request.getMonitorClipboardBurst());
        } else if (exam.getMonitorClipboardBurst() == null) {
            exam.setMonitorClipboardBurst(Boolean.TRUE);
        }
        if (request.getMonitorFullscreenEvasion() != null) {
            exam.setMonitorFullscreenEvasion(request.getMonitorFullscreenEvasion());
        } else if (exam.getMonitorFullscreenEvasion() == null) {
            exam.setMonitorFullscreenEvasion(Boolean.TRUE);
        }
        if (request.getMonitorAnswerSimilarity() != null) {
            exam.setMonitorAnswerSimilarity(request.getMonitorAnswerSimilarity());
        } else if (exam.getMonitorAnswerSimilarity() == null) {
            exam.setMonitorAnswerSimilarity(Boolean.TRUE);
        }
        if (request.getMonitorIpFingerprintGraph() != null) {
            exam.setMonitorIpFingerprintGraph(request.getMonitorIpFingerprintGraph());
        } else if (exam.getMonitorIpFingerprintGraph() == null) {
            exam.setMonitorIpFingerprintGraph(Boolean.TRUE);
        }
        if (request.getEnableAiProctoring() != null) {
            exam.setEnableAiProctoring(request.getEnableAiProctoring());
        } else if (exam.getEnableAiProctoring() == null) {
            exam.setEnableAiProctoring(Boolean.FALSE);
        }
        if (request.getShuffleQuestions() != null) {
            exam.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getShuffleAnswers() != null) {
            exam.setShuffleAnswers(request.getShuffleAnswers());
        }
        return toResponse(examRepository.save(exam));
    }

    /**
     * Tạo đợt thi mới cho đề thi đã có (mở lại đề thi với thời gian khác).
     * Cập nhật Exam.startTime, endTime và tạo Assignment mới.
     */
    @Transactional
    public ExamResponse createNewSession(Long examId, NewSessionRequest request, User actor) {
        currentUserService.requireTeacherOrAdmin(actor);
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu và kết thúc là bắt buộc.");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        Exam exam = requireManageableExam(examId, actor);

        int duration = request.getDurationMinutes() != null && request.getDurationMinutes() > 0
                ? Math.max(5, Math.min(480, request.getDurationMinutes()))
                : exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 60;

        Assignment assignment = Assignment.builder()
                .exam(exam)
                .createdBy(actor)
                .title(exam.getTitle() + " (Đợt mới)")
                .openAt(request.getStartTime())
                .closeAt(request.getEndTime())
                .maxAttempts(1)
                .allowReviewAfterSubmit(true)
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();
        assignmentRepository.save(assignment);

        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(duration);
        exam.setIsActive(true);
        examRepository.save(exam);

        return toResponse(exam);
    }

    @Transactional
    public void deleteExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);

        try {
            riskSnapshotRepository.deleteByExam(exam);
            monitoringEventRepository.deleteByExam(exam);
            auditLogRepository.deleteByExam(exam);
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

    @Transactional
    public ExamResponse publishExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);
        exam.setIsActive(true);
        return toResponse(examRepository.save(exam));
    }

    @Transactional
    public ExamResponse unpublishExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);
        exam.setIsActive(false);
        return toResponse(examRepository.save(exam));
    }

    @Transactional
    public ExamResponse archiveExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);
        exam.setIsArchived(true);
        return toResponse(examRepository.save(exam));
    }

    @Transactional
    public ExamResponse unarchiveExam(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);
        exam.setIsArchived(false);
        return toResponse(examRepository.save(exam));
    }

    @Transactional
    public ExamResponse duplicateExam(Long examId, User actor) {
        Exam original = requireManageableExam(examId, actor);
        Exam copy = Exam.builder()
                .title("Bản sao — " + original.getTitle())
                .code(generateUniqueExamCode())
                .description(original.getDescription())
                .className(original.getClassName())
                .startTime(null)
                .endTime(null)
                .durationMinutes(original.getDurationMinutes())
                .isActive(false)
                .isArchived(false)
                .createdBy(actor)
                .monitorTabSwitch(original.getMonitorTabSwitch())
                .monitorBlur(original.getMonitorBlur())
                .monitorExitFullscreen(original.getMonitorExitFullscreen())
                .monitorCopyPaste(original.getMonitorCopyPaste())
                .monitorIdleTime(original.getMonitorIdleTime())
                .monitorDevtools(original.getMonitorDevtools())
                .monitorDuplicateIp(original.getMonitorDuplicateIp())
                .monitorFastSubmit(original.getMonitorFastSubmit())
                .monitorRightClick(original.getMonitorRightClick())
                .monitorPrintScreen(original.getMonitorPrintScreen())
                .monitorRapidQuestionSwitch(original.getMonitorRapidQuestionSwitch())
                .monitorMultiMonitor(original.getMonitorMultiMonitor())
                .requireCameraMic(original.getRequireCameraMic())
                .monitorNetworkInstability(original.getMonitorNetworkInstability())
                .monitorSessionRecovery(original.getMonitorSessionRecovery())
                .monitorQuestionTimingAnomaly(original.getMonitorQuestionTimingAnomaly())
                .monitorAnswerChangeBurst(original.getMonitorAnswerChangeBurst())
                .monitorClipboardBurst(original.getMonitorClipboardBurst())
                .monitorFullscreenEvasion(original.getMonitorFullscreenEvasion())
                .monitorAnswerSimilarity(original.getMonitorAnswerSimilarity())
                .monitorIpFingerprintGraph(original.getMonitorIpFingerprintGraph())
                .enableAiProctoring(original.getEnableAiProctoring())
                .shuffleQuestions(original.getShuffleQuestions())
                .shuffleAnswers(original.getShuffleAnswers())
                .practice(false)
                .build();
        examRepository.save(copy);
        // Copy questions
        List<Question> originalQs = questionRepository.findByExam(original);
        for (Question oq : originalQs) {
            Question nq = Question.builder()
                    .exam(copy)
                    .content(oq.getContent())
                    .type(oq.getType())
                    .scoreWeight(oq.getScoreWeight())
                    .options(oq.getOptions())
                    .correctAnswer(oq.getCorrectAnswer())
                    .difficulty(oq.getDifficulty())
                    .metadata(oq.getMetadata())
                    .attachments(oq.getAttachments())
                    .build();
            questionRepository.save(nq);
        }
        return toResponse(copy);
    }

    @Transactional
    public List<ExamResponse> bulkPublish(List<Long> examIds, User actor) {
        List<ExamResponse> results = new ArrayList<>();
        for (Long id : examIds) {
            try {
                results.add(publishExam(id, actor));
            } catch (Exception e) {
                log.warn("Failed to publish exam {} in bulk operation: {}", id, e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public List<ExamResponse> bulkArchive(List<Long> examIds, User actor) {
        List<ExamResponse> results = new ArrayList<>();
        for (Long id : examIds) {
            try {
                results.add(archiveExam(id, actor));
            } catch (Exception e) {
                log.warn("Failed to archive exam {} in bulk operation: {}", id, e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public void bulkDelete(List<Long> examIds, User actor) {
        for (Long id : examIds) {
            try {
                deleteExam(id, actor);
            } catch (Exception e) {
                log.warn("Failed to delete exam {} in bulk operation: {}", id, e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<WaitingStudentResponse> getWaitingStudents(Long examId, User actor) {
        Exam exam = requireManageableExam(examId, actor);
        List<ExamAttempt> attempts = examAttemptRepository.findByExamWithStudent(exam);

        List<WaitingStudentResponse> waitingStudents = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (attempt.getStudent() == null) continue;

            String statusLabel = switch (attempt.getStatus()) {
                case IN_PROGRESS -> "Đang thi";
                case PAUSED -> "Tạm dừng";
                case SUBMITTED, AUTO_SUBMITTED -> "Đã nộp";
                case STOPPED -> "Bị dừng";
            };

            waitingStudents.add(WaitingStudentResponse.builder()
                    .attemptId(attempt.getId())
                    .studentId(attempt.getStudent().getId())
                    .studentName(attempt.getStudent().getFullName() != null ?
                            attempt.getStudent().getFullName() : attempt.getStudent().getUsername())
                    .studentEmail(attempt.getStudent().getEmail())
                    .status(statusLabel)
                    .riskScore(attempt.getRiskScore())
                    .suspicious(attempt.getSuspicious())
                    .joinedAt(attempt.getStartedAt() != null ?
                            DateTimeUtils.toOffset(attempt.getStartedAt(), VietNamTime.zone()).toString() : null)
                    .build());
        }

        waitingStudents.sort((a, b) -> {
            if (a.getJoinedAt() == null) return 1;
            if (b.getJoinedAt() == null) return -1;
            return b.getJoinedAt().compareTo(a.getJoinedAt());
        });

        return waitingStudents;
    }

    public Exam requireExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Exam> findById(Long examId) {
        return examRepository.findById(examId);
    }

    @Transactional(readOnly = true)
    public Exam requireManageableExam(Long examId, User actor) {
        Exam exam = requireExam(examId);
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin) {
            return exam;
        }
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        if (isTeacher) {
            if (!exam.getCreatedBy().getId().equals(actor.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this exam");
            }
            return exam;
        }
        if (!exam.getCreatedBy().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this exam");
        }
        if (!isStudentPracticeExam(exam)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this exam");
        }
        return exam;
    }

    /**
     * Practice exams generated for students ({@code practice == true}) or legacy rows with the standard title prefix.
     */
    public boolean isStudentPracticeExam(Exam exam) {
        if (Boolean.TRUE.equals(exam.getPractice())) {
            return true;
        }
        return exam.getTitle() != null && exam.getTitle().startsWith(PRACTICE_EXAM_TITLE_PREFIX);
    }

    @Transactional(readOnly = true)
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

        // Own exam (e.g. practice created by student): avoid loading creator.roles when not needed
        if (exam.getCreatedBy().getId().equals(actor.getId())) {
            return exam;
        }

        boolean creatorIsTeacher = exam.getCreatedBy().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.TEACHER || role.getName() == RoleName.ADMIN);
        if (!creatorIsTeacher) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this exam");
        }
        enforceStudentClassAccess(exam, actor);
        return exam;
    }

    private void enforceStudentClassAccess(Exam exam, User actor) {
        String className = normalizeClassName(exam.getClassName());
        if (className == null) {
            return;
        }
        Optional<com.example.demo.domain.entity.ClassEntity> enrolledClass = classService.findStudentEnrolledClassForExam(actor, exam);
        if (enrolledClass.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp được phép vào bài thi này");
        }
    }

    private boolean canStudentAccessExam(User actor, Exam exam) {
        try {
            enforceStudentClassAccess(exam, actor);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    public void validateExamAvailability(Exam exam, LocalDateTime nowInExamZone) {
        if (Boolean.FALSE.equals(exam.getIsActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam is not active");
        }

        if (exam.getStartTime() != null && nowInExamZone.isBefore(exam.getStartTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has not started yet");
        }

        if (exam.getEndTime() != null && !nowInExamZone.isBefore(exam.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has ended");
        }
    }

    public void validateExamJoinable(Exam exam, LocalDateTime nowInExamZone) {
        if (Boolean.FALSE.equals(exam.getIsActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam is not active");
        }

        if (exam.getEndTime() != null && !nowInExamZone.isBefore(exam.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam has ended");
        }
    }

    private void validateExamRequest(ExamRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getStartTime().isBefore(request.getEndTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startTime must be strictly before endTime");
        }
    }

    private void ensureAssignedClassBelongsToTeacher(String className, User teacher) {
        String normalizedClassName = normalizeClassName(className);
        if (normalizedClassName == null || teacher == null) {
            return;
        }
        if (classService.findTeacherClassByName(teacher, normalizedClassName).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lớp được gán cho bài thi không hợp lệ");
        }
    }

    private String normalizeClassName(String className) {
        if (className == null) {
            return null;
        }
        String normalized = className.trim();
        return normalized.isEmpty() ? null : normalized;
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
        return toResponse(exam, examAttemptRepository.countDistinctStudentsByExamId(exam.getId()),
                questionRepository.countByExam(exam));
    }

    private ExamResponse toResponse(Exam exam, long participantCount) {
        return toResponse(exam, participantCount, questionRepository.countByExam(exam));
    }

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private ExamResponse toResponse(Exam exam, long participantCount, long questionCount) {
        return ExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .startTime(toOffset(exam.getStartTime()))
                .endTime(toOffset(exam.getEndTime()))
                .durationMinutes(exam.getDurationMinutes())
                .isActive(exam.getIsActive())
                .isArchived(Boolean.TRUE.equals(exam.getIsArchived()))
                .className(exam.getClassName())
                .createdBy(exam.getCreatedBy() == null ? null : exam.getCreatedBy().getUsername())
                .questionCount(questionCount)
                .participantCount(participantCount)
                .monitorTabSwitch(exam.getMonitorTabSwitch())
                .monitorBlur(exam.getMonitorBlur())
                .monitorExitFullscreen(exam.getMonitorExitFullscreen())
                .monitorCopyPaste(exam.getMonitorCopyPaste())
                .monitorIdleTime(exam.getMonitorIdleTime())
                .monitorDevtools(exam.getMonitorDevtools())
                .monitorDuplicateIp(exam.getMonitorDuplicateIp())
                .monitorFastSubmit(exam.getMonitorFastSubmit())
                .monitorRightClick(exam.getMonitorRightClick())
                .monitorPrintScreen(exam.getMonitorPrintScreen())
                .monitorRapidQuestionSwitch(exam.getMonitorRapidQuestionSwitch())
                .monitorMultiMonitor(exam.getMonitorMultiMonitor())
                .requireCameraMic(exam.getRequireCameraMic())
                .monitorNetworkInstability(exam.getMonitorNetworkInstability())
                .monitorSessionRecovery(exam.getMonitorSessionRecovery())
                .monitorQuestionTimingAnomaly(exam.getMonitorQuestionTimingAnomaly())
                .monitorAnswerChangeBurst(exam.getMonitorAnswerChangeBurst())
                .monitorClipboardBurst(exam.getMonitorClipboardBurst())
                .monitorFullscreenEvasion(exam.getMonitorFullscreenEvasion())
                .monitorAnswerSimilarity(exam.getMonitorAnswerSimilarity())
                .monitorIpFingerprintGraph(exam.getMonitorIpFingerprintGraph())
                .enableAiProctoring(exam.getEnableAiProctoring())
                .shuffleQuestions(exam.getShuffleQuestions())
                .shuffleAnswers(exam.getShuffleAnswers())
                .build();
    }
}
