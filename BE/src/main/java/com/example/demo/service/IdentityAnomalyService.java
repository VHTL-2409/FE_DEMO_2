package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.service.FraudSignalService;
import com.example.demo.service.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phát hiện bất thường về danh tính: thay đổi device fingerprint, nhiều thiết bị cùng phiên, và thay đổi IP.
 *
 * <p>Các signal được tạo ra:</p>
 * <ul>
 *   <li>{@code DEVICE_FINGERPRINT_CHANGED} — cùng một attempt nhưng khác thiết bị/trình duyệt</li>
 *   <li>{@code MULTIPLE_DEVICE_SESSION} — cùng học sinh + bài thi, có attempt thứ hai đang active với fingerprint khác</li>
 *   <li>{@code IP_CHANGED} — cùng một attempt nhưng IP thay đổi giữa chừng</li>
 * </ul>
 *
 * <p>Giai đoạn 1: signals được ghi nhận nhưng attempts KHÔNG bị tự động dừng.</p>
 */
@Service
@RequiredArgsConstructor
public class IdentityAnomalyService {

    @Value("${demo.identity-anomaly.ip-change-dedup-seconds:60}")
    private long ipChangeDedupSeconds;

    @Value("${demo.identity-anomaly.device-change-dedup-seconds:300}")
    private long deviceChangeDedupSeconds;

    @Value("${demo.identity-anomaly.multi-device-dedup-seconds:300}")
    private long multiDeviceDedupSeconds;

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;
    private final RealtimeNotificationService realtimeNotificationService;

    /**
     * Được gọi khi phiên giám sát bắt đầu (batch / heartbeat đầu tiên).
     * Thiết lập fingerprint/IP ban đầu và kiểm tra multi-device sessions.
     * @return danh sách các FraudSignal mới được tạo (có thể rỗng)
     */
    @Transactional
    public List<FraudSignal> onProctoringStart(ExamAttempt attempt, String normalizedFingerprint, String clientIp) {
        List<FraudSignal> newSignals = new ArrayList<>();
        boolean isFirstFingerprint = attempt.getOriginalDeviceFingerprint() == null
                || attempt.getOriginalDeviceFingerprint().isBlank();
        boolean isFirstIp = attempt.getInitialClientIp() == null
                || attempt.getInitialClientIp().isBlank();

        // Debug logging
        System.out.println("[DEBUG] onProctoringStart: attemptId=" + attempt.getId()
            + ", studentId=" + attempt.getStudent().getId()
            + ", examId=" + attempt.getExam().getId()
            + ", currentStatus=" + attempt.getStatus()
            + ", originalFingerprint=" + (attempt.getOriginalDeviceFingerprint() != null ? attempt.getOriginalDeviceFingerprint().substring(0, Math.min(16, attempt.getOriginalDeviceFingerprint().length())) + "..." : "null")
            + ", isFirstFingerprint=" + isFirstFingerprint
            + ", normalizedFingerprint=" + (normalizedFingerprint != null ? normalizedFingerprint.substring(0, Math.min(16, normalizedFingerprint.length())) + "..." : "null")
            + ", isFirstIp=" + isFirstIp
            + ", clientIp=" + clientIp);

        if (isFirstFingerprint && normalizedFingerprint != null && !normalizedFingerprint.isBlank()) {
            attempt.setOriginalDeviceFingerprint(normalizedFingerprint);
            attempt.setDeviceFingerprint(normalizedFingerprint);
            examAttemptRepository.save(attempt);
            System.out.println("[DEBUG] onProctoringStart: SET fingerprint for attemptId=" + attempt.getId());
        }

        if (isFirstIp && clientIp != null && !clientIp.isBlank()) {
            attempt.setInitialClientIp(clientIp);
            attempt.setCurrentClientIp(clientIp);
            examAttemptRepository.save(attempt);
            System.out.println("[DEBUG] onProctoringStart: SET IP for attemptId=" + attempt.getId() + ", ip=" + clientIp);
        }

        // Luôn kiểm tra multi-device session sau khi thiết lập fingerprint.
        // Điều này phát hiện trường hợp một attempt mới bắt đầu trong khi một attempt active
        // khác cho cùng học sinh + bài thi tồn tại với fingerprint khác.
        if (normalizedFingerprint != null && !normalizedFingerprint.isBlank()) {
            System.out.println("[DEBUG] onProctoringStart: calling checkMultiDeviceSession for attemptId=" + attempt.getId());
            newSignals.addAll(checkMultiDeviceSession(attempt, normalizedFingerprint));
            System.out.println("[DEBUG] onProctoringStart: checkMultiDeviceSession returned " + newSignals.size() + " signals for attemptId=" + attempt.getId());
        }
        return newSignals;
    }

    /**
     * Được gọi trên mỗi batch/heartbeat. Kiểm tra fingerprint/IP thay đổi trong cùng một attempt.
     * @return danh sách các FraudSignal mới được tạo (có thể rỗng)
     */
    @Transactional
    public List<FraudSignal> onProctoringHeartbeat(ExamAttempt attempt, String normalizedFingerprint, String clientIp) {
        List<FraudSignal> newSignals = new ArrayList<>();
        FraudSignal fpSignal = detectFingerprintChange(attempt, normalizedFingerprint);
        if (fpSignal != null) newSignals.add(fpSignal);

        FraudSignal ipSignal = detectIpChange(attempt, clientIp);
        if (ipSignal != null) newSignals.add(ipSignal);

        newSignals.addAll(checkMultiDeviceSession(attempt, normalizedFingerprint));
        return newSignals;
    }

    /**
     * Phát hiện thay đổi fingerprint trong cùng một attempt. Trả về signal nếu phát hiện, null nếu không.
     */
    private FraudSignal detectFingerprintChange(ExamAttempt attempt, String newFingerprint) {
        if (newFingerprint == null || newFingerprint.isBlank()) {
            return null;
        }
        String original = attempt.getOriginalDeviceFingerprint();
        if (original == null || original.isBlank()) {
            return null;
        }
        if (original.equals(newFingerprint)) {
            return null;
        }

        if (isRecentIdentitySignal(attempt, "DEVICE_FINGERPRINT_CHANGED", deviceChangeDedupSeconds)) {
            return null;
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "identity_anomaly");
        evidence.put("detectionType", "FINGERPRINT_CHANGE");
        evidence.put("attemptId", attempt.getId());
        evidence.put("studentId", attempt.getStudent().getId());
        evidence.put("examId", attempt.getExam().getId());
        evidence.put("originalFingerprint", original);
        evidence.put("newFingerprint", newFingerprint);
        evidence.put("changeType", "within_attempt");

        FraudSignal signal = fraudSignalService.recordServerSignal(
                attempt,
                "DEVICE_FINGERPRINT_CHANGED",
                SignalSeverity.HIGH,
                0.95,
                evidence
        );

        realtimeNotificationService.notifyFraudSignalRecorded(
                attempt, signal, null, attempt.getRiskScore() != null ? attempt.getRiskScore() : 0,
                attempt.getRiskLevel(), Map.of("identityScore", 20)
        );

        return signal;
    }

    /**
     * Phát hiện thay đổi IP trong cùng một attempt. Trả về signal nếu phát hiện, null nếu không.
     */
    private FraudSignal detectIpChange(ExamAttempt attempt, String newIp) {
        if (newIp == null || newIp.isBlank()) {
            return null;
        }
        String initialIp = attempt.getInitialClientIp();
        if (initialIp == null || initialIp.isBlank()) {
            return null;
        }
        if (initialIp.equals(newIp)) {
            return null;
        }

        if (isRecentIdentitySignal(attempt, "IP_CHANGED", ipChangeDedupSeconds)) {
            return null;
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "identity_anomaly");
        evidence.put("detectionType", "IP_CHANGE");
        evidence.put("attemptId", attempt.getId());
        evidence.put("studentId", attempt.getStudent().getId());
        evidence.put("examId", attempt.getExam().getId());
        evidence.put("initialIp", initialIp);
        evidence.put("newIp", newIp);
        evidence.put("changeType", "within_attempt");

        FraudSignal signal = fraudSignalService.recordServerSignal(
                attempt,
                "IP_CHANGED",
                SignalSeverity.MEDIUM,
                0.88,
                evidence
        );

        attempt.setCurrentClientIp(newIp);
        examAttemptRepository.save(attempt);

        realtimeNotificationService.notifyFraudSignalRecorded(
                attempt, signal, null, attempt.getRiskScore() != null ? attempt.getRiskScore() : 0,
                attempt.getRiskLevel(), Map.of("identityScore", 15)
        );

        return signal;
    }

    /**
     * Phát hiện multi-device session: cùng học sinh + cùng bài thi có nhiều active attempts
     * với fingerprint khác nhau. Trả về danh sách các signal mới (có thể rỗng).
     */
    private List<FraudSignal> checkMultiDeviceSession(ExamAttempt attempt, String currentFingerprint) {
        List<FraudSignal> newSignals = new ArrayList<>();
        if (currentFingerprint == null || currentFingerprint.isBlank()) {
            System.out.println("[DEBUG] checkMultiDeviceSession: null/blank fingerprint, skipping for attemptId=" + attempt.getId());
            return newSignals;
        }

        List<ExamAttempt> activeCounterparts = examAttemptRepository
                .findActiveCounterpartsForMultiDeviceDetection(
                        attempt.getExam().getId(),
                        attempt.getStudent().getId(),
                        attempt.getId()
                );

        System.out.println("[DEBUG] checkMultiDeviceSession: attemptId=" + attempt.getId()
            + ", currentFingerprint=" + currentFingerprint.substring(0, Math.min(16, currentFingerprint.length())) + "..."
            + ", found " + activeCounterparts.size() + " active counterparts");

        for (ExamAttempt counterpart : activeCounterparts) {
            String counterpartFp = counterpart.getDeviceFingerprint();
            if (counterpartFp == null || counterpartFp.isBlank()) {
                counterpartFp = counterpart.getOriginalDeviceFingerprint();
            }
            if (counterpartFp == null || counterpartFp.isBlank()) {
                System.out.println("[DEBUG] checkMultiDeviceSession: counterpart attemptId=" + counterpart.getId() + " has no fingerprint, skipping");
                continue;
            }

            System.out.println("[DEBUG] checkMultiDeviceSession: comparing currentFp=" + currentFingerprint.substring(0, Math.min(16, currentFingerprint.length())) + "..."
                + " with counterpartFp=" + counterpartFp.substring(0, Math.min(16, counterpartFp.length())) + "..."
                + ", equals=" + counterpartFp.equals(currentFingerprint));

            if (counterpartFp.equals(currentFingerprint)) {
                System.out.println("[DEBUG] checkMultiDeviceSession: fingerprints match, skipping");
                continue;
            }

            String pairSignature = buildPairSignature(attempt.getId(), counterpart.getId());
            if (isRecentIdentitySignalForPair(attempt, "MULTIPLE_DEVICE_SESSION", pairSignature, multiDeviceDedupSeconds)) {
                System.out.println("[DEBUG] checkMultiDeviceSession: recent signal exists for pair, skipping");
                continue;
            }

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "identity_anomaly");
            evidence.put("detectionType", "MULTI_DEVICE_SESSION");
            evidence.put("attemptId", attempt.getId());
            evidence.put("otherAttemptId", counterpart.getId());
            evidence.put("studentId", attempt.getStudent().getId());
            evidence.put("examId", attempt.getExam().getId());
            evidence.put("thisFingerprint", currentFingerprint);
            evidence.put("otherFingerprint", counterpartFp);
            evidence.put("thisIp", attempt.getCurrentClientIp());
            evidence.put("otherIp", counterpart.getCurrentClientIp());

            FraudSignal signal = fraudSignalService.recordServerSignal(
                    attempt,
                    "MULTIPLE_DEVICE_SESSION",
                    SignalSeverity.HIGH,
                    0.95,
                    evidence
            );
            newSignals.add(signal);

            Map<String, Object> peerEvidence = new LinkedHashMap<>();
            peerEvidence.put("source", "identity_anomaly");
            peerEvidence.put("detectionType", "MULTI_DEVICE_SESSION");
            peerEvidence.put("attemptId", counterpart.getId());
            peerEvidence.put("otherAttemptId", attempt.getId());
            peerEvidence.put("studentId", counterpart.getStudent().getId());
            peerEvidence.put("examId", counterpart.getExam().getId());
            peerEvidence.put("thisFingerprint", counterpartFp);
            peerEvidence.put("otherFingerprint", currentFingerprint);
            peerEvidence.put("thisIp", counterpart.getCurrentClientIp());
            peerEvidence.put("otherIp", attempt.getCurrentClientIp());

            FraudSignal peerSignal = fraudSignalService.recordServerSignal(
                    counterpart,
                    "MULTIPLE_DEVICE_SESSION",
                    SignalSeverity.HIGH,
                    0.95,
                    peerEvidence
            );

            realtimeNotificationService.notifyFraudSignalRecorded(
                    attempt, signal, null, attempt.getRiskScore() != null ? attempt.getRiskScore() : 0,
                    attempt.getRiskLevel(), Map.of("identityScore", 25)
            );
            realtimeNotificationService.notifyFraudSignalRecorded(
                    counterpart, peerSignal, null, counterpart.getRiskScore() != null ? counterpart.getRiskScore() : 0,
                    counterpart.getRiskLevel(), Map.of("identityScore", 25)
            );
        }
        return newSignals;
    }

    private boolean isRecentIdentitySignal(ExamAttempt attempt, String signalType, long dedupSeconds) {
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(Math.max(dedupSeconds, 0));
        return fraudSignalRepository.countByAttemptAndSignalTypeSince(attempt, signalType, cutoff) > 0;
    }

    private boolean isRecentIdentitySignalForPair(ExamAttempt attempt, String signalType,
            String pairSignature, long dedupSeconds) {
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(Math.max(dedupSeconds, 0));
        return fraudSignalRepository
                .existsByAttemptAndSignalTypeAndCreatedAtAfterWithDetails(attempt.getId(), signalType, cutoff, pairSignature);
    }

    private String buildPairSignature(Long firstId, Long secondId) {
        Long min = Math.min(firstId, secondId);
        Long max = Math.max(firstId, secondId);
        return "pair=" + min + "-" + max;
    }
}
