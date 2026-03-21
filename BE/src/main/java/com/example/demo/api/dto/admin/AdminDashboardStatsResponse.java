package com.example.demo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private long totalUsers;
    private long totalTeachers;
    private long totalStudents;
    private long totalAdmins;
    private long totalExams;
    private long totalQuestions;
    private long totalAttempts;
    private List<DailyAttemptCount> attemptsLast7Days;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttemptCount {
        private String date;
        private long count;
    }
}
