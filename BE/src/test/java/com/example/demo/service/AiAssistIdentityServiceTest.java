package com.example.demo.service;

import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.api.dto.ai.IdentityVerifyRequest;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.StudentIdentityCheck;
import com.example.demo.domain.entity.User;
import com.example.demo.realtime.TeacherAlertGateway;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.StudentIdentityCheckRepository;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.service.ProctorEvidenceImageService.StoredEvidenceImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistIdentityServiceTest {

    private ExamAttemptRepository examAttemptRepository;
    private StudentIdentityCheckRepository studentIdentityCheckRepository;
    private ProctorEvidenceImageService evidenceImageService;
    private TeacherAlertGateway teacherAlertGateway;
    private FraudSignalService fraudSignalService;
    private FraudWarningService fraudWarningService;
    private AiAssistService service;

    @BeforeEach
    void setUp() {
        examAttemptRepository = mock(ExamAttemptRepository.class);
        studentIdentityCheckRepository = mock(StudentIdentityCheckRepository.class);
        evidenceImageService = mock(ProctorEvidenceImageService.class);
        teacherAlertGateway = mock(TeacherAlertGateway.class);
        fraudSignalService = mock(FraudSignalService.class);
        fraudWarningService = mock(FraudWarningService.class);
        service = new AiAssistService(
                examAttemptRepository,
                fraudSignalService,
                mock(FraudSignalRepository.class),
                mock(RiskScoringService.class),
                teacherAlertGateway,
                fraudWarningService,
                evidenceImageService,
                mock(StudentProfileRepository.class),
                studentIdentityCheckRepository,
                mock(SubmissionService.class),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:1");
        ReflectionTestUtils.setField(service, "timeoutMs", 20_000);
        ReflectionTestUtils.setField(service, "cameraTimeoutMs", 4_000);
        ReflectionTestUtils.setField(service, "cameraFrameMaxBase64Chars", 2_500_000);
        ReflectionTestUtils.setField(service, "cameraSignalDedupSeconds", 30L);
        ReflectionTestUtils.setField(service, "behaviorSignalDedupSeconds", 30L);
    }

    @Test
    void verifyIdentityRejectsExamWithIdentityDisabled() {
        User student = student();
        ExamAttempt attempt = attempt(71L, exam(false, true), student, AttemptStatus.WAITING);
        when(examAttemptRepository.findByIdWithExamAndUsers(71L)).thenReturn(Optional.of(attempt));

        IdentityVerifyRequest request = new IdentityVerifyRequest();
        request.setAttemptId(71L);
        request.setStudentId(student.getId());
        request.setDocumentImageBase64("data:image/png;base64,AAAA");
        request.setSelfieImageBase64("data:image/png;base64,BBBB");

        assertThatThrownBy(() -> service.verifyIdentity(request, student))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void recheckIdentityRejectsExamWithPeriodicIdentityDisabled() {
        User student = student();
        ExamAttempt attempt = attempt(72L, exam(true, false), student, AttemptStatus.IN_PROGRESS);
        when(examAttemptRepository.findByIdWithExamAndUsers(72L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.recheckIdentity(frameRequest(72L, student.getId()), student))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Periodic identity recheck is disabled");
    }

    @Test
    void recheckIdentityPersistsFrameEvidenceReference() {
        User student = student();
        ExamAttempt attempt = attempt(73L, exam(true, true), student, AttemptStatus.IN_PROGRESS);
        when(examAttemptRepository.findByIdWithExamAndUsers(73L)).thenReturn(Optional.of(attempt));
        when(evidenceImageService.storeFrameImage(eq(73L), anyString(), eq("IDENTITY_RECHECK"), anyString()))
                .thenReturn(new StoredEvidenceImage(
                        "/api/v1/proctor/evidence/identity-recheck.png",
                        "identity-recheck.png",
                        "identity-recheck.png",
                        "image/png",
                        12L,
                        LocalDateTime.now()
                ));
        when(studentIdentityCheckRepository.save(any(StudentIdentityCheck.class))).thenAnswer(invocation -> {
            StudentIdentityCheck check = invocation.getArgument(0);
            check.setId(900L);
            return check;
        });

        Map<String, Object> response = service.recheckIdentity(frameRequest(73L, student.getId()), student);

        assertThat(response.get("identityCheckId")).isEqualTo(900L);
        assertThat(response.get("reviewStatus")).isEqualTo("AUTO_VERIFIED");
        assertThat(response.get("evidenceRefs")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> evidenceRefs = (Map<String, Object>) response.get("evidenceRefs");
        assertThat(evidenceRefs).containsKeys("frame", "frameId", "capturedAt", "source");
    }

    @Test
    void recheckIdentityUsesSelfieReferenceAndCreatesMismatchWarningWithoutSignalArray() throws Exception {
        User student = student();
        ExamAttempt attempt = attempt(731L, exam(true, true), student, AttemptStatus.IN_PROGRESS);
        StudentIdentityCheck initialCheck = identityCheck(990L, attempt, student, "VERIFIED", "AUTO_VERIFIED");
        initialCheck.setCheckType("INITIAL");
        initialCheck.setEvidenceRefsJson("""
                {"selfie":{"fileName":"identity-selfie.png"}}
                """);

        when(examAttemptRepository.findByIdWithExamAndUsers(731L)).thenReturn(Optional.of(attempt));
        when(studentIdentityCheckRepository.findByAttemptIdOrderByCreatedAtDesc(731L))
                .thenReturn(java.util.List.of(initialCheck));
        when(evidenceImageService.loadEvidenceImageDataUrl(731L, "identity-selfie.png"))
                .thenReturn("data:image/png;base64,SELFIE");
        when(fraudSignalService.descriptorFor("IDENTITY_FACE_MISMATCH"))
                .thenReturn(new FraudSignalService.SignalDescriptor(
                        "IDENTITY_FACE_MISMATCH",
                        "VISUAL_IDENTITY",
                        "Khuon mat khong khop giay to",
                        22,
                        SignalSeverity.HIGH,
                        0.84d
                ));
        when(evidenceImageService.storeFrameImage(eq(731L), anyString(), eq("IDENTITY_RECHECK"), anyString()))
                .thenReturn(new StoredEvidenceImage(
                        "/api/v1/proctor/evidence/identity-recheck.png",
                        "identity-recheck.png",
                        "identity-recheck.png",
                        "image/png",
                        12L,
                        LocalDateTime.now()
                ));
        when(studentIdentityCheckRepository.save(any(StudentIdentityCheck.class))).thenAnswer(invocation -> {
            StudentIdentityCheck check = invocation.getArgument(0);
            check.setId(991L);
            return check;
        });

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/identity/recheck", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(requestBody).contains("\"reference_face_base64\":\"data:image/png;base64,SELFIE\"");
            assertThat(requestBody).contains("\"referenceSource\":\"IDENTITY_SELFIE\"");

            byte[] body = """
                    {
                      "status":"DONE",
                      "verificationStatus":"NEEDS_REVIEW",
                      "confidence":0.41,
                      "faceMatch":{"available":true,"matched":false,"confidence":0.33,"reason":"face drift"},
                      "signals":[]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ReflectionTestUtils.setField(service, "enabled", true);
            ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            Map<String, Object> response = service.recheckIdentity(frameRequest(731L, student.getId()), student);

            assertThat(response.get("identityCheckId")).isEqualTo(991L);
            verify(fraudWarningService).recordWarningWithDedupWindow(
                    eq(attempt.getExam()),
                    eq(attempt),
                    eq(FraudWarningCategory.VISUAL_IDENTITY),
                    eq("IDENTITY_FACE_MISMATCH"),
                    eq(SignalSeverity.HIGH),
                    eq(0.33d),
                    eq("Khuon mat khong khop giay to"),
                    any(),
                    eq("identity_recheck"),
                    eq(java.util.List.of(attempt.getId())),
                    any()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void identityHistoryReturnsStoredChecksForAuthorizedTeacher() {
        User teacher = teacher(31L);
        User student = student();
        Exam exam = exam(true, true);
        exam.setCreatedBy(teacher);
        ExamAttempt attempt = attempt(74L, exam, student, AttemptStatus.IN_PROGRESS);
        StudentIdentityCheck first = identityCheck(901L, attempt, student, "VERIFIED", "AUTO_VERIFIED");
        StudentIdentityCheck second = identityCheck(902L, attempt, student, "NEEDS_REVIEW", "NEEDS_REVIEW");
        when(examAttemptRepository.findByIdWithExamAndUsers(74L)).thenReturn(Optional.of(attempt));
        when(studentIdentityCheckRepository.findByAttemptIdOrderByCreatedAtDesc(74L))
                .thenReturn(java.util.List.of(second, first));

        var history = service.getIdentityCheckHistory(74L, teacher);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("identityCheckId")).isEqualTo(902L);
        assertThat(history.get(0).get("verificationStatus")).isEqualTo("NEEDS_REVIEW");
    }

    @Test
    void identityHistoryRejectsTeacherThatDoesNotOwnExam() {
        User owner = teacher(31L);
        User otherTeacher = teacher(32L);
        User student = student();
        Exam exam = exam(true, true);
        exam.setCreatedBy(owner);
        ExamAttempt attempt = attempt(75L, exam, student, AttemptStatus.IN_PROGRESS);
        when(examAttemptRepository.findByIdWithExamAndUsers(75L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.getIdentityCheckHistory(75L, otherTeacher))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not allowed");
    }

    @Test
    void adminCanReviewIdentityCheck() {
        User admin = admin();
        User student = student();
        ExamAttempt attempt = attempt(76L, exam(true, true), student, AttemptStatus.IN_PROGRESS);
        StudentIdentityCheck check = identityCheck(903L, attempt, student, "NEEDS_REVIEW", "NEEDS_REVIEW");
        when(studentIdentityCheckRepository.findById(903L)).thenReturn(Optional.of(check));
        when(studentIdentityCheckRepository.save(any(StudentIdentityCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> reviewed = service.reviewIdentityCheck(903L, "VERIFIED", "manual ok", admin);

        assertThat(reviewed.get("verificationStatus")).isEqualTo("VERIFIED");
        assertThat(reviewed.get("reviewStatus")).isEqualTo("CONFIRMED");
        assertThat(reviewed.get("reviewedById")).isEqualTo(admin.getId());
    }

    @Test
    void verifyIdentityPublishesTeacherAlertWhenReviewRequired() {
        User student = student();
        Exam exam = exam(true, true);
        exam.setId(82L);
        ExamAttempt attempt = attempt(77L, exam, student, AttemptStatus.WAITING);
        when(examAttemptRepository.findByIdWithExamAndUsers(77L)).thenReturn(Optional.of(attempt));
        when(studentIdentityCheckRepository.save(any(StudentIdentityCheck.class))).thenAnswer(invocation -> {
            StudentIdentityCheck check = invocation.getArgument(0);
            check.setId(904L);
            return check;
        });
        IdentityVerifyRequest request = new IdentityVerifyRequest();
        request.setAttemptId(77L);
        request.setStudentId(student.getId());
        request.setDocumentImageBase64("data:image/png;base64,AAAA");
        request.setSelfieImageBase64("data:image/png;base64,BBBB");

        Map<String, Object> response = service.verifyIdentity(request, student);

        assertThat(response.get("verificationStatus")).isEqualTo("NEEDS_REVIEW");
        verify(teacherAlertGateway).publishIdentityReviewRequired(
                eq(82L),
                eq(77L),
                eq(student.getUsername()),
                eq(student.getUsername()),
                eq(904L),
                eq("NEEDS_REVIEW"),
                eq("NEEDS_REVIEW"),
                anyString(),
                any(),
                eq("MEDIUM"),
                any()
        );
    }

    @Test
    void verifyIdentityStoresDocumentFaceCropEvidenceWhenAiReturnsCrop() throws Exception {
        User student = student();
        Exam exam = exam(true, true);
        ExamAttempt attempt = attempt(79L, exam, student, AttemptStatus.WAITING);
        when(examAttemptRepository.findByIdWithExamAndUsers(79L)).thenReturn(Optional.of(attempt));
        when(evidenceImageService.storeFrameImage(eq(79L), anyString(), anyString(), anyString()))
                .thenReturn(new StoredEvidenceImage(
                        "/evidence-images/attempt-79/evidence.jpg",
                        "evidence.jpg",
                        "evidence.jpg",
                        "image/jpeg",
                        10L,
                        LocalDateTime.now()
                ));
        when(studentIdentityCheckRepository.save(any(StudentIdentityCheck.class))).thenAnswer(invocation -> {
            StudentIdentityCheck check = invocation.getArgument(0);
            check.setId(905L);
            return check;
        });

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/identity/verify", exchange -> {
            byte[] body = """
                    {
                      "status":"DONE",
                      "verificationStatus":"NEEDS_REVIEW",
                      "confidence":0.7,
                      "documentOcr":{"text":"ok","fields":{}},
                      "faceMatch":{"available":true,"matched":false,"confidence":0.4},
                      "documentFaceCrop":{"available":true,"imageBase64":"AAAA","box":[1,2,3,4],"confidence":0.8,"quality":0.7,"method":"TEST"},
                      "liveness":{"passed":true,"score":0.8},
                      "signals":[],
                      "reviewReason":"manual review"
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ReflectionTestUtils.setField(service, "enabled", true);
            ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            IdentityVerifyRequest request = new IdentityVerifyRequest();
            request.setAttemptId(79L);
            request.setStudentId(student.getId());
            request.setDocumentImageBase64("data:image/png;base64,AAAA");
            request.setSelfieImageBase64("data:image/png;base64,BBBB");

            Map<String, Object> response = service.verifyIdentity(request, student);

            assertThat(response.get("identityCheckId")).isEqualTo(905L);
            verify(evidenceImageService).storeFrameImage(eq(79L), eq("identity-document-face"), eq("IDENTITY_DOCUMENT_FACE"), eq("AAAA"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void analyzeFrameFallsBackWithoutRecordingSignalsWhenAiServiceRateLimits() throws Exception {
        User student = student();
        Exam exam = exam(true, true);
        exam.setId(83L);
        exam.setEnableAiProctoring(true);
        ExamAttempt attempt = attempt(78L, exam, student, AttemptStatus.IN_PROGRESS);
        attempt.setCameraOn(true);
        when(examAttemptRepository.findByIdWithExamAndUsers(78L)).thenReturn(Optional.of(attempt));

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/proctor/analyze/frame", exchange -> {
            byte[] body = "{\"error\":\"RATE_LIMIT_EXCEEDED\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(429, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ReflectionTestUtils.setField(service, "enabled", true);
            ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            FrameAnalysisRequest request = frameRequest(78L, student.getId());
            request.setFrameId("rate-limited-frame");

            Map<String, Object> response = service.analyzeFrame(request);

            assertThat(response.get("status")).isEqualTo("AI_BUSY");
            assertThat(response.get("message")).asString().doesNotContain("RATE_LIMIT_EXCEEDED");
            assertThat(response.get("backendAnalysisReceived")).isEqualTo(false);
            assertThat(response.get("accepted")).isEqualTo(true);
            assertThat(response.get("frameId")).isEqualTo("rate-limited-frame");
            verify(teacherAlertGateway, times(2)).publishCameraFrame(eq(83L), eq(78L), any());
            verify(fraudSignalService, never()).recordServerSignal(any(), any(), any(), anyDouble(), any());
        } finally {
            server.stop(0);
        }
    }

    private Exam exam(boolean requireIdentity, boolean periodicIdentity) {
        return Exam.builder()
                .id(81L)
                .title("Identity exam")
                .requireIdentityVerification(requireIdentity)
                .inExamIdentityCheckEnabled(periodicIdentity)
                .practice(false)
                .build();
    }

    private ExamAttempt attempt(Long id, Exam exam, User student, AttemptStatus status) {
        return ExamAttempt.builder()
                .id(id)
                .exam(exam)
                .student(student)
                .status(status)
                .score(0.0)
                .riskScore(0)
                .suspicious(false)
                .build();
    }

    private User student() {
        return User.builder()
                .id(91L)
                .username("student-91")
                .roles(Set.of(Role.builder().name(RoleName.STUDENT).build()))
                .build();
    }

    private User teacher(Long id) {
        return User.builder()
                .id(id)
                .username("teacher-" + id)
                .roles(Set.of(Role.builder().name(RoleName.TEACHER).build()))
                .build();
    }

    private User admin() {
        return User.builder()
                .id(1L)
                .username("admin")
                .roles(Set.of(Role.builder().name(RoleName.ADMIN).build()))
                .build();
    }


    private StudentIdentityCheck identityCheck(Long id, ExamAttempt attempt, User student, String status, String reviewStatus) {
        return StudentIdentityCheck.builder()
                .id(id)
                .attempt(attempt)
                .student(student)
                .status(status)
                .confidence(0.72d)
                .documentType("STUDENT_ID")
                .checkType("INITIAL")
                .source("WAITING_ROOM")
                .ocrFieldsJson("{}")
                .matchedFieldsJson("{}")
                .mismatchedFieldsJson("{}")
                .faceMatchJson("{}")
                .livenessJson("{}")
                .evidenceRefsJson("{}")
                .reviewStatus(reviewStatus)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private FrameAnalysisRequest frameRequest(Long attemptId, Long studentId) {
        FrameAnalysisRequest request = new FrameAnalysisRequest();
        request.setAttemptId(attemptId);
        request.setStudentId(studentId);
        request.setFrameId("identity-frame-1");
        request.setImageBase64("data:image/png;base64,AAAA");
        request.setCapturedAt(VietNamTime.now().toString());
        return request;
    }
}
