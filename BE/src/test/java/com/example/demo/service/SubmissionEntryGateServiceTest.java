package com.example.demo.service;

import com.example.demo.api.dto.submission.AttemptEntryStatusResponse;
import com.example.demo.api.dto.submission.StartAttemptResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ClassEntity;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.StudentIdentityCheck;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.AssignmentRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.StudentIdentityCheckRepository;
import com.example.demo.service.helper.SubmissionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionEntryGateServiceTest {

    private ExamAttemptRepository examAttemptRepository;
    private AssignmentRepository assignmentRepository;
    private StudentIdentityCheckRepository studentIdentityCheckRepository;
    private ExamService examService;
    private ClassService classService;
    private SubmissionService service;

    @BeforeEach
    void setUp() {
        examAttemptRepository = mock(ExamAttemptRepository.class);
        assignmentRepository = mock(AssignmentRepository.class);
        studentIdentityCheckRepository = mock(StudentIdentityCheckRepository.class);
        examService = mock(ExamService.class);
        classService = mock(ClassService.class);
        service = new SubmissionService(
                examAttemptRepository,
                assignmentRepository,
                mock(QuestionRepository.class),
                mock(AnswerRepository.class),
                mock(SubmissionHelper.class),
                examService,
                mock(RealtimeNotificationService.class),
                mock(DuplicateIpDetectionService.class),
                mock(AuditLogService.class),
                mock(MonitoringService.class),
                mock(QuestionService.class),
                mock(FraudSignalRepository.class),
                mock(FraudWarningRepository.class),
                studentIdentityCheckRepository,
                classService
        );
    }

    @Test
    void prepareAttemptRejectsStudentOutsideAssignedClass() {
        User teacher = user(1L, RoleName.TEACHER);
        User student = user(2L, RoleName.STUDENT);
        ClassEntity assignedClass = classEntity(10L, "12A1", teacher);
        Exam exam = classExam(20L, teacher, assignedClass);
        when(classService.findStudentEnrolledClassForExam(student, exam)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.prepareAttempt(exam, student, "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not enrolled");

        verify(examAttemptRepository, never()).save(any(ExamAttempt.class));
    }

    @Test
    void prepareAttemptForEnrolledStudentReturnsWaitingGateStateWithClassMetadata() {
        User teacher = user(1L, RoleName.TEACHER);
        User student = user(2L, RoleName.STUDENT);
        ClassEntity assignedClass = classEntity(10L, "12A1", teacher);
        Exam exam = classExam(20L, teacher, assignedClass);
        when(classService.findStudentEnrolledClassForExam(student, exam)).thenReturn(Optional.of(assignedClass));
        when(assignmentRepository.findPublishedByExamOrderByCreatedAtDesc(exam)).thenReturn(List.of());
        when(studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt attempt = invocation.getArgument(0);
            attempt.setId(100L);
            return attempt;
        });

        StartAttemptResponse response = service.prepareAttempt(exam, student, "127.0.0.1");

        assertThat(response.getStatus()).isEqualTo(AttemptStatus.WAITING.name());
        assertThat(response.getClassId()).isEqualTo(10L);
        assertThat(response.getClassName()).isEqualTo("12A1");
        assertThat(response.getClassMembershipRequired()).isTrue();
        assertThat(response.getClassMembershipValid()).isTrue();
        assertThat(response.getEntryBlockedReasons()).contains("IDENTITY_NOT_VERIFIED");
    }

    @Test
    void needsReviewIdentityIsAcceptedWhenPolicyAllowsWarning() {
        User teacher = user(1L, RoleName.TEACHER);
        User student = user(2L, RoleName.STUDENT);
        Exam exam = identityExam(20L, teacher, "ALLOW_WITH_WARNING");
        ExamAttempt attempt = readyAttempt(100L, exam, student);
        when(examAttemptRepository.findByIdWithExamAndUsers(100L)).thenReturn(Optional.of(attempt));
        when(studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(identityCheck(200L, attempt, student, "NEEDS_REVIEW")));

        AttemptEntryStatusResponse response = service.getEntryStatus(100L, student);

        assertThat(response.getIdentityStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(response.getIdentityReviewPolicy()).isEqualTo("ALLOW_WITH_WARNING");
        assertThat(response.getBlockedReasons()).doesNotContain("IDENTITY_NOT_VERIFIED");
        assertThat(response.getCanStart()).isTrue();
    }

    @Test
    void needsReviewIdentityStillBlocksWhenPolicyRequiresVerifiedOnly() {
        User teacher = user(1L, RoleName.TEACHER);
        User student = user(2L, RoleName.STUDENT);
        Exam exam = identityExam(20L, teacher, "STRICT_VERIFIED_ONLY");
        ExamAttempt attempt = readyAttempt(100L, exam, student);
        when(examAttemptRepository.findByIdWithExamAndUsers(100L)).thenReturn(Optional.of(attempt));
        when(studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(identityCheck(200L, attempt, student, "NEEDS_REVIEW")));

        AttemptEntryStatusResponse response = service.getEntryStatus(100L, student);

        assertThat(response.getIdentityStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(response.getIdentityReviewPolicy()).isEqualTo("STRICT_VERIFIED_ONLY");
        assertThat(response.getBlockedReasons()).contains("IDENTITY_NOT_VERIFIED");
        assertThat(response.getCanStart()).isFalse();
    }

    private Exam classExam(Long id, User teacher, ClassEntity assignedClass) {
        return Exam.builder()
                .id(id)
                .title("Class exam")
                .durationMinutes(45)
                .isActive(true)
                .createdBy(teacher)
                .classEntity(assignedClass)
                .className(assignedClass.getName())
                .requireRulesAgreement(false)
                .requireCameraMic(false)
                .requireIdentityVerification(true)
                .identityReviewPolicy("ALLOW_WITH_WARNING")
                .identityCheckIntervalSeconds(60)
                .practice(false)
                .build();
    }

    private Exam identityExam(Long id, User teacher, String reviewPolicy) {
        return Exam.builder()
                .id(id)
                .title("Identity exam")
                .durationMinutes(45)
                .isActive(true)
                .createdBy(teacher)
                .requireRulesAgreement(false)
                .requireCameraMic(false)
                .requireIdentityVerification(true)
                .identityReviewPolicy(reviewPolicy)
                .identityCheckIntervalSeconds(60)
                .practice(false)
                .build();
    }

    private ExamAttempt readyAttempt(Long id, Exam exam, User student) {
        return ExamAttempt.builder()
                .id(id)
                .exam(exam)
                .student(student)
                .status(AttemptStatus.WAITING)
                .cameraOn(true)
                .micOn(true)
                .rulesAgreedAt(LocalDateTime.now())
                .build();
    }

    private StudentIdentityCheck identityCheck(Long id, ExamAttempt attempt, User student, String status) {
        return StudentIdentityCheck.builder()
                .id(id)
                .attempt(attempt)
                .student(student)
                .status(status)
                .confidence(0.8)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ClassEntity classEntity(Long id, String name, User teacher) {
        return ClassEntity.builder()
                .id(id)
                .name(name)
                .teacher(teacher)
                .build();
    }

    private User user(Long id, RoleName roleName) {
        return User.builder()
                .id(id)
                .username("user-" + id)
                .email("user-" + id + "@example.test")
                .roles(Set.of(Role.builder().name(roleName).build()))
                .build();
    }
}
