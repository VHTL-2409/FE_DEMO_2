package com.example.demo.service;

import com.example.demo.api.dto.admin.AdminDashboardStatsResponse;
import com.example.demo.domain.entity.ImportJobStatus;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.ImportJobRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ImportJobRepository importJobRepository;
    private final EmailService emailService;
    private final AiAssistService aiAssistService;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        List<Object[]> raw = examAttemptRepository.countAttemptsGroupedByDaySince(from.atStartOfDay());
        Map<String, Long> byDay = raw.stream().collect(Collectors.toMap(
                row -> String.valueOf(row[0]).trim(),
                row -> ((Number) row[1]).longValue(),
                Long::sum));

        List<AdminDashboardStatsResponse.DailyAttemptCount> series = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            String key = d.format(DAY_FMT);
            series.add(AdminDashboardStatsResponse.DailyAttemptCount.builder()
                    .date(key)
                    .count(byDay.getOrDefault(key, 0L))
                    .build());
        }

        return AdminDashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalTeachers(userRepository.countUsersWithRole(RoleName.TEACHER))
                .totalStudents(userRepository.countUsersWithRole(RoleName.STUDENT))
                .totalAdmins(userRepository.countUsersWithRole(RoleName.ADMIN))
                .totalExams(examRepository.count())
                .totalQuestions(questionRepository.count())
                .totalAttempts(examAttemptRepository.count())
                .attemptsLast7Days(series)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOpsSummary() {
        boolean emailConfigured = emailService.isMailConfigured();
        Map<String, Object> aiHealth = aiAssistService.healthSummary();
        long inactiveExams = examRepository.countByIsActiveFalse();
        long pendingImports = importJobRepository.countByStatus(ImportJobStatus.PROCESSING);
        long failedImports = importJobRepository.countByStatus(ImportJobStatus.FAILED);
        long emailVerificationPending = userRepository.countByEmailVerifiedFalse();

        List<Map<String, Object>> healthChecks = List.of(
                healthCheck("api", "API backend", "UP", "Core API đang phản hồi bình thường."),
                healthCheck("email", "Email", emailConfigured ? "READY" : "ATTENTION",
                        emailConfigured
                                ? "Email xác minh và quên mật khẩu đã sẵn sàng."
                                : "Chưa cấu hình SMTP đầy đủ."),
                healthCheck("ai", "AI service",
                        String.valueOf(aiHealth.getOrDefault("status", "UNKNOWN")),
                        String.valueOf(aiHealth.getOrDefault("message", "Chưa có dữ liệu AI service.")))
        );

        List<Map<String, Object>> moderationQueue = List.of(
                queueItem("pending-imports", "Import jobs đang xử lý", pendingImports, pendingImports > 0 ? "HIGH" : "LOW", "/teacher/exams"),
                queueItem("failed-imports", "Import lỗi cần rà lại", failedImports, failedImports > 0 ? "HIGH" : "LOW", "/teacher/exams"),
                queueItem("inactive-exams", "Đề đang ở trạng thái nháp/tắt", inactiveExams, inactiveExams > 0 ? "MEDIUM" : "LOW", "/admin/exams"),
                queueItem("unverified-users", "Tài khoản chưa xác minh email", emailVerificationPending, emailVerificationPending > 0 ? "MEDIUM" : "LOW", "/admin/students")
        );

        List<Map<String, Object>> bulkActions = List.of(
                actionItem("users", "Rà tài khoản học sinh", "Xem và xử lý các tài khoản cần hỗ trợ hoặc chưa xác minh.", "/admin/students", "group"),
                actionItem("teachers", "Kiểm tra giảng viên", "Đi nhanh tới danh sách giáo viên để rà soát hồ sơ và quyền.", "/admin/teachers", "co_present"),
                actionItem("exams", "Duyệt trạng thái đề", "Xử lý các đề thi đang draft/tắt hoặc cần bật lại.", "/admin/exams", "quiz")
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now().toString());
        response.put("moderationQueueSize", pendingImports + failedImports + inactiveExams + emailVerificationPending);
        response.put("healthChecks", healthChecks);
        response.put("moderationQueue", moderationQueue);
        response.put("bulkActions", bulkActions);
        return response;
    }

    private Map<String, Object> healthCheck(String id, String label, String status, String detail) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("label", label);
        item.put("status", status);
        item.put("detail", detail);
        return item;
    }

    private Map<String, Object> queueItem(String id, String label, long count, String severity, String route) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("label", label);
        item.put("count", count);
        item.put("severity", severity);
        item.put("route", route);
        return item;
    }

    private Map<String, Object> actionItem(String id, String label, String description, String route, String icon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("label", label);
        item.put("description", description);
        item.put("route", route);
        item.put("icon", icon);
        return item;
    }
}
