package com.example.demo.service;

import com.example.demo.api.dto.fraud.ComprehensiveAnalysisResponse;
import com.example.demo.api.dto.fraud.StatisticalAnalysisResponse;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudAnalysisServiceTest {

    private ExamRepository examRepository;
    private ExamAttemptRepository examAttemptRepository;
    private FraudSignalRepository fraudSignalRepository;
    private AnswerSimilarityService answerSimilarityService;
    private FraudWarningService fraudWarningService;
    private QuestionRepository questionRepository;
    private FraudAnalysisService service;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examAttemptRepository = mock(ExamAttemptRepository.class);
        fraudSignalRepository = mock(FraudSignalRepository.class);
        answerSimilarityService = mock(AnswerSimilarityService.class);
        fraudWarningService = mock(FraudWarningService.class);
        questionRepository = mock(QuestionRepository.class);
        service = new FraudAnalysisService(
                examRepository,
                examAttemptRepository,
                fraudSignalRepository,
                answerSimilarityService,
                fraudWarningService,
                questionRepository
        );
        ReflectionTestUtils.setField(service, "tabSwitchMediumThreshold", 5L);
        ReflectionTestUtils.setField(service, "tabSwitchHighThreshold", 10L);
        ReflectionTestUtils.setField(service, "heartbeatMediumThreshold", 3L);
        ReflectionTestUtils.setField(service, "heartbeatHighThreshold", 10L);
    }

    @Test
    void statisticalAnalysisHandlesExamWithoutSubmittedAttempts() {
        Exam exam = Exam.builder().id(10L).title("Math").build();
        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExam(exam)).thenReturn(List.of());

        StatisticalAnalysisResponse response = service.analyzeStatisticalByExam(10L);

        assertThat(response.getTotalAttempts()).isZero();
        assertThat(response.getScoreStats().getCount()).isZero();
        assertThat(response.getStatisticalResults()).isEmpty();
    }

    @Test
    void comprehensiveAnalysisIncludesStatisticalAndBehaviorSections() {
        Exam exam = Exam.builder().id(11L).title("Physics").build();
        when(examRepository.findById(11L)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExam(exam)).thenReturn(List.of());
        when(answerSimilarityService.findSuspiciousPairs(any(Exam.class))).thenReturn(List.of());

        ComprehensiveAnalysisResponse response = service.analyzeComprehensiveByExam(11L);

        assertThat(response.getStatistical()).isNotNull();
        assertThat(response.getBehavior()).isNotNull();
        assertThat(response.getIpReputation()).isNotNull();
        assertThat(response.getPlagiarism()).isNotNull();
        assertThat(response.getTiming()).isNotNull();
    }

    @Test
    void statisticalAnalysisUsesLongDedupWindowForGeneratedWarnings() {
        Exam exam = Exam.builder().id(12L).title("Chemistry").build();
        List<ExamAttempt> attempts = List.of(
                submittedAttempt(1L, exam, "student-1", 100.0),
                submittedAttempt(2L, exam, "student-2", 50.0),
                submittedAttempt(3L, exam, "student-3", 50.0),
                submittedAttempt(4L, exam, "student-4", 50.0),
                submittedAttempt(5L, exam, "student-5", 50.0)
        );
        when(examRepository.findById(12L)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExam(exam)).thenReturn(attempts);
        when(questionRepository.countByExam(exam)).thenReturn(10L);

        StatisticalAnalysisResponse response = service.analyzeStatisticalByExam(12L);

        assertThat(response.getStatisticalResults()).hasSize(1);
        ArgumentCaptor<Duration> dedupWindow = ArgumentCaptor.forClass(Duration.class);
        verify(fraudWarningService).recordWarningWithDedupWindow(
                eq(exam),
                eq(attempts.get(0)),
                eq(FraudWarningCategory.POST_EXAM_STATISTICAL),
                eq("SCORE_OUTLIER"),
                eq(SignalSeverity.MEDIUM),
                anyDouble(),
                anyString(),
                any(),
                eq("statistical_analysis"),
                anyList(),
                dedupWindow.capture()
        );
        assertThat(dedupWindow.getValue()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void behaviorAnalysisUsesLongDedupWindowForGeneratedWarnings() {
        Exam exam = Exam.builder().id(13L).title("Biology").build();
        ExamAttempt attempt = submittedAttempt(6L, exam, "student-6", 80.0);
        when(examRepository.findById(13L)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExam(exam)).thenReturn(List.of(attempt));
        when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(
                signal(attempt, "TAB_SWITCH"),
                signal(attempt, "TAB_SWITCH"),
                signal(attempt, "TAB_SWITCH"),
                signal(attempt, "TAB_SWITCH"),
                signal(attempt, "TAB_SWITCH")
        ));

        service.analyzeBehaviorByExam(13L);

        ArgumentCaptor<Duration> dedupWindow = ArgumentCaptor.forClass(Duration.class);
        verify(fraudWarningService).recordWarningWithDedupWindow(
                eq(exam),
                eq(attempt),
                eq(FraudWarningCategory.SESSION_INTEGRITY),
                eq("TAB_SWITCH"),
                eq(SignalSeverity.MEDIUM),
                anyDouble(),
                anyString(),
                any(),
                eq("behavior_analysis"),
                anyList(),
                dedupWindow.capture()
        );
        assertThat(dedupWindow.getValue()).isEqualTo(Duration.ofDays(30));
    }

    private ExamAttempt submittedAttempt(Long id, Exam exam, String username, double score) {
        return ExamAttempt.builder()
                .id(id)
                .exam(exam)
                .student(User.builder().id(id).username(username).build())
                .status(AttemptStatus.SUBMITTED)
                .score(score)
                .riskScore(0)
                .suspicious(false)
                .build();
    }

    private FraudSignal signal(ExamAttempt attempt, String type) {
        return FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType(type)
                .severity(SignalSeverity.MEDIUM)
                .confidence(0.8)
                .build();
    }
}
