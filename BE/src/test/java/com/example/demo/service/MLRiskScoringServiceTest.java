package com.example.demo.service;

import com.example.demo.api.dto.fraud.MLRiskScoreResponse;
import com.example.demo.api.dto.fraud.MLModelStatusResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MLRiskScoringServiceTest {

    private FraudSignalRepository fraudSignalRepository;
    private ExamAttemptRepository examAttemptRepository;
    private ExamRepository examRepository;
    private MLRiskScoringService service;

    @BeforeEach
    void setUp() {
        fraudSignalRepository = mock(FraudSignalRepository.class);
        examAttemptRepository = mock(ExamAttemptRepository.class);
        examRepository = mock(ExamRepository.class);
        service = new MLRiskScoringService(fraudSignalRepository, examAttemptRepository, examRepository);
        ReflectionTestUtils.setField(service, "mlEnabled", false);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.7);
        ReflectionTestUtils.setField(service, "mlWeight", 0.6);
        ReflectionTestUtils.setField(service, "minConfidence", 0.5);
        ReflectionTestUtils.setField(service, "mlTimeoutMs", 1);
        ReflectionTestUtils.setField(service, "mlServiceUrl", "http://127.0.0.1:1");
    }

    @Test
    void analyzeRiskDoesNotFailWhenAttemptStartTimeIsMissing() {
        Exam exam = Exam.builder().id(31L).title("Math").build();
        User student = User.builder().id(41L).username("student-41").build();
        ExamAttempt attempt = ExamAttempt.builder()
                .id(51L)
                .exam(exam)
                .student(student)
                .status(AttemptStatus.SUBMITTED)
                .score(8.0)
                .riskScore(0)
                .suspicious(false)
                .submittedAt(VietNamTime.now())
                .build();
        FraudSignal signal = FraudSignal.builder()
                .attempt(attempt)
                .student(student)
                .signalType("TAB_SWITCH")
                .category("SCREEN_LEAVE")
                .severity(SignalSeverity.MEDIUM)
                .confidence(0.8)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(examAttemptRepository.findByIdWithExamAndUsers(51L)).thenReturn(Optional.of(attempt));
        when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(signal));

        MLRiskScoreResponse response = service.analyzeRisk(51L);

        assertThat(response.getAttemptId()).isEqualTo(51L);
        assertThat(response.getRuleBasedScore()).isGreaterThan(0);
        assertThat(response.getModelType()).isEqualTo("RULE_BASED");
        assertThat(response.getScoringStatus()).isEqualTo("DISABLED");
        assertThat(response.getScoringSource()).isEqualTo("RULE_BASED");
        assertThat(response.getAlgorithm()).isEqualTo("RuleBased");
        assertThat(response.getCombinedScore()).isEqualTo(response.getRuleBasedScore());
        assertThat(response.getMlAnalysis()).containsEntry("mode", "RULE_BASED_ONLY");
    }

    @Test
    void mlResponseParsingAcceptsSnakeCaseFromAiService() {
        Map<String, Object> response = Map.of(
                "ml_score", 72.5,
                "ml_confidence", 0.82,
                "risk_level", "HIGH_RISK",
                "fraud_probability", 0.725
        );

        Optional<Double> score = ReflectionTestUtils.invokeMethod(
                service,
                "numberFrom",
                response,
                new String[]{"mlScore", "ml_score", "score"}
        );
        Optional<Double> confidence = ReflectionTestUtils.invokeMethod(
                service,
                "numberFrom",
                response,
                new String[]{"confidence", "mlConfidence", "ml_confidence"}
        );
        Optional<String> level = ReflectionTestUtils.invokeMethod(
                service,
                "stringFrom",
                response,
                new String[]{"riskLevel", "risk_level"}
        );

        assertThat(score).contains(72.5);
        assertThat(confidence).contains(0.82);
        assertThat(level).contains("HIGH_RISK");
    }

    @Test
    void enabledMlFallsBackToStatisticalSourceWhenExternalServiceIsUnavailable() {
        ReflectionTestUtils.setField(service, "mlEnabled", true);
        Exam exam = Exam.builder().id(32L).title("Physics").build();
        User student = User.builder().id(42L).username("student-42").build();
        ExamAttempt attempt = ExamAttempt.builder()
                .id(52L)
                .exam(exam)
                .student(student)
                .status(AttemptStatus.SUBMITTED)
                .startedAt(VietNamTime.now().minusMinutes(30))
                .submittedAt(VietNamTime.now())
                .riskScore(0)
                .suspicious(false)
                .build();
        FraudSignal signal = FraudSignal.builder()
                .attempt(attempt)
                .student(student)
                .signalType("DEVTOOLS_OPEN")
                .category("TECHNICAL")
                .severity(SignalSeverity.HIGH)
                .confidence(0.9)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(examAttemptRepository.findByIdWithExamAndUsers(52L)).thenReturn(Optional.of(attempt));
        when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(signal));

        MLRiskScoreResponse response = service.analyzeRisk(52L);

        assertThat(response.getModelType()).isEqualTo("ML_HYBRID");
        assertThat(response.getScoringStatus()).isEqualTo("FALLBACK_STATISTICAL");
        assertThat(response.getScoringSource()).isEqualTo("STATISTICAL_FALLBACK");
        assertThat(response.getAlgorithm()).isEqualTo("WeightedSignalStatisticalFallback");
        assertThat(response.getMlAnalysis()).containsEntry("mode", "STATISTICAL_FALLBACK");
    }

    @Test
    void enabledMlCallsExternalAiServiceWithBackendContractAndParsesResponse() throws Exception {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestApiKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ml/risk/predict", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestApiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, """
                    {
                      "mlScore": 82.5,
                      "confidence": 0.91,
                      "riskLevel": "HIGH_RISK",
                      "fraudProbability": 0.825,
                      "modelVersion": "ai-service-rule-risk-v1",
                      "diagnostics": {
                        "algorithm": "weighted_signal_risk_v1"
                      }
                    }
                    """);
        });
        server.start();

        try {
            ReflectionTestUtils.setField(service, "mlEnabled", true);
            ReflectionTestUtils.setField(service, "mlServiceUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/");
            ReflectionTestUtils.setField(service, "aiServiceApiKey", "contract-key");
            Exam exam = Exam.builder().id(33L).title("Chemistry").build();
            User student = User.builder().id(43L).username("student-43").build();
            ExamAttempt attempt = ExamAttempt.builder()
                    .id(53L)
                    .exam(exam)
                    .student(student)
                    .status(AttemptStatus.SUBMITTED)
                    .startedAt(VietNamTime.now().minusMinutes(25))
                    .submittedAt(VietNamTime.now())
                    .riskScore(0)
                    .suspicious(false)
                    .build();
            FraudSignal signal = FraudSignal.builder()
                    .attempt(attempt)
                    .student(student)
                    .signalType("TAB_SWITCH")
                    .category("SCREEN_LEAVE")
                    .severity(SignalSeverity.HIGH)
                    .confidence(0.9)
                    .createdAt(LocalDateTime.now().minusMinutes(4))
                    .build();

            when(examAttemptRepository.findByIdWithExamAndUsers(53L)).thenReturn(Optional.of(attempt));
            when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(signal));

            MLRiskScoreResponse response = service.analyzeRisk(53L);

            assertThat(requestMethod).hasValue("POST");
            assertThat(requestPath).hasValue("/ml/risk/predict");
            assertThat(requestApiKey).hasValue("contract-key");
            assertThat(requestBody.get())
                    .contains("\"attemptId\":53")
                    .contains("\"studentId\":43")
                    .contains("\"examId\":33")
                    .contains("\"signals\"")
                    .contains("\"tabSwitchCount\":1")
                    .contains("\"highSignalCount\":1")
                    .contains("\"totalSignalCount\":1");

            assertThat(response.getModelType()).isEqualTo("ML_HYBRID");
            assertThat(response.getScoringStatus()).isEqualTo("READY");
            assertThat(response.getScoringSource()).isEqualTo("EXTERNAL_ML");
            assertThat(response.getAlgorithm()).isEqualTo("weighted_signal_risk_v1");
            assertThat(response.getMlScore()).isEqualTo(82.5);
            assertThat(response.getMlConfidence()).isEqualTo(0.91);
            assertThat(response.getMlRiskLevel()).isEqualTo("HIGH_RISK");
            assertThat(response.getFraudProbability()).isEqualTo(0.825);
            assertThat(response.getCombinedScore()).isEqualTo(52);
            assertThat(response.getMlAnalysis())
                    .containsEntry("mode", "ML_HYBRID")
                    .containsEntry("source", "EXTERNAL_ML")
                    .containsEntry("status", "READY");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void modelStatusDoesNotReportReadyWhenExternalServiceIsUnavailable() {
        ReflectionTestUtils.setField(service, "mlEnabled", true);

        MLModelStatusResponse status = service.getModelStatus();

        assertThat(status.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(status.getWarnings()).contains("External ML risk service is enabled but unavailable. Attempts will use statistical fallback.");
    }

    private static void sendJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
