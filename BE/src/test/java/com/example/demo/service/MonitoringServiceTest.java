package com.example.demo.service;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.FraudWarning;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.FraudWarningReviewStatus;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamEventRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringServiceTest {

    private FraudSignalRepository fraudSignalRepository;
    private FraudWarningRepository fraudWarningRepository;
    private FraudWarningService fraudWarningService;
    private ExamAttemptRepository examAttemptRepository;
    private MonitoringService service;

    @BeforeEach
    void setUp() {
        fraudSignalRepository = mock(FraudSignalRepository.class);
        fraudWarningRepository = mock(FraudWarningRepository.class);
        fraudWarningService = mock(FraudWarningService.class);
        examAttemptRepository = mock(ExamAttemptRepository.class);
        service = new MonitoringService(
                examAttemptRepository,
                mock(MonitoringEventRepository.class),
                mock(ExamEventRepository.class),
                fraudSignalRepository,
                fraudWarningRepository,
                mock(RiskScoreLogRepository.class),
                mock(ExamEventService.class),
                fraudWarningService,
                mock(RealtimeNotificationService.class),
                mock(AuditLogService.class),
                mock(AuditLogRepository.class),
                mock(RiskScoringService.class)
        );
    }

    @Test
    void updateDeviceStatusStoresReadinessForWaitingAttempt() {
        User student = User.builder()
                .id(30L)
                .username("student")
                .roles(Set.of())
                .build();
        User teacher = teacher(1L);
        ExamAttempt attempt = ExamAttempt.builder()
                .id(40L)
                .exam(Exam.builder().id(20L).createdBy(teacher).build())
                .student(student)
                .status(AttemptStatus.WAITING)
                .build();
        when(examAttemptRepository.findByIdWithExamAndUsers(40L)).thenReturn(Optional.of(attempt));

        service.updateDeviceStatus(40L, true, true, student);

        org.junit.jupiter.api.Assertions.assertTrue(attempt.getCameraOn());
        org.junit.jupiter.api.Assertions.assertTrue(attempt.getMicOn());
        verify(examAttemptRepository).save(attempt);
    }

    @Test
    void dismissCameraAlertReviewsExistingCameraWarning() {
        User teacher = teacher(1L);
        ExamAttempt attempt = attemptOwnedBy(teacher);
        FraudWarning warning = FraudWarning.builder()
                .id(9L)
                .attempt(attempt)
                .exam(attempt.getExam())
                .category(FraudWarningCategory.CAMERA_PROCTORING)
                .warningType("MULTIPLE_FACES")
                .severity(SignalSeverity.HIGH)
                .confidence(0.9)
                .build();
        when(fraudWarningRepository.findById(9L)).thenReturn(Optional.of(warning));

        service.dismissCameraAlert(9L, teacher);

        verify(fraudWarningService).reviewWarning(
                eq(9L),
                eq(FraudWarningReviewStatus.DISMISSED),
                eq("Camera alert dismissed from proctor dashboard"),
                eq(teacher)
        );
    }

    @Test
    void acknowledgeCameraAlertCreatesWarningFromRawSignalThenReviewsIt() {
        User teacher = teacher(2L);
        ExamAttempt attempt = attemptOwnedBy(teacher);
        FraudSignal signal = FraudSignal.builder()
                .id(10L)
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType("MULTIPLE_FACES")
                .severity(SignalSeverity.HIGH)
                .confidence(0.88)
                .build();
        FraudWarning recorded = FraudWarning.builder()
                .id(11L)
                .attempt(attempt)
                .exam(attempt.getExam())
                .category(FraudWarningCategory.CAMERA_PROCTORING)
                .warningType("MULTIPLE_FACES")
                .build();
        when(fraudWarningRepository.findById(10L)).thenReturn(Optional.empty());
        when(fraudSignalRepository.findById(10L)).thenReturn(Optional.of(signal));
        when(fraudWarningService.recordFromFraudSignal(signal)).thenReturn(Optional.of(recorded));

        service.acknowledgeCameraAlert(10L, teacher);

        verify(fraudWarningService).recordFromFraudSignal(signal);
        verify(fraudWarningService).reviewWarning(
                eq(11L),
                eq(FraudWarningReviewStatus.CONFIRMED),
                eq("Camera alert acknowledged from proctor dashboard"),
                eq(teacher)
        );
    }

    private User teacher(Long id) {
        return User.builder()
                .id(id)
                .username("teacher-" + id)
                .roles(Set.of(Role.builder().name(RoleName.TEACHER).build()))
                .build();
    }

    private ExamAttempt attemptOwnedBy(User teacher) {
        Exam exam = Exam.builder()
                .id(20L)
                .title("Math")
                .createdBy(teacher)
                .build();
        User student = User.builder()
                .id(30L)
                .username("student")
                .build();
        return ExamAttempt.builder()
                .id(40L)
                .exam(exam)
                .student(student)
                .build();
    }
}
