package com.example.demo.service;

import com.example.demo.api.dto.admin.AdminDashboardStatsResponse;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
}
