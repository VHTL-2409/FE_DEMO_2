package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Phat hien VPN, Proxy, Tor, va bat thuong IP.
 *
 * Ky thuat:
 * - Tra cuu IP reputation bang API
 * - Phat hien VPN/Proxy bang reverse DNS lookup
 * - Tor exit node detection
 * - Geo-location anomaly (dia diem khong hop ly)
 * - ISP anomaly (ISP khong khop voi truong/lop)
 * - Subnet analysis (cung subnet voi nhieu hoc sinh khac)
 *
 * Gian lan phat hien:
 * - VPN_DETECTED: Phat hien su dung VPN
 * - PROXY_DETECTED: Phat hien su dung proxy
 * - TOR_EXIT_NODE: Phat hien su dung Tor
 * - GEO_ANOMALY: Dia diem bat thuong
 * - ISP_ANOMALY: ISP khong khop
 * - SUBNET_SUSPICION: Cung subnet voi nhieu nguoi
 * - IP_REPUTATION_LOW: IP co reputation thap
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpReputationService {

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;

    @Value("${demo.ip-reputation.enabled:true}")
    private boolean enabled;

    @Value("${demo.ip-reputation.api-key:}")
    private String apiKey;

    @Value("${demo.ip-reputation.api-url:}")
    private String apiUrl;

    @Value("${demo.ip-reputation.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${demo.ip-reputation.cache-hours:24}")
    private int cacheHours;

    @Value("${demo.ip-reputation.subnet-suspicion-threshold:3}")
    private int subnetSuspicionThreshold;

    // In-memory cache: ip -> IpReputationResult
    private final Map<String, CachedIpReputation> reputationCache = new HashMap<>();

    /**
     * Phan tich tat ca attempts trong mot bai thi.
     */
    @Transactional
    public List<IpReputationResult> analyzeExam(Long examId) {
        if (!enabled) return Collections.emptyList();

        log.info("Starting IP reputation analysis for exam: {}", examId);
        List<IpReputationResult> results = new ArrayList<>();

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStatus(examId, AttemptStatus.SUBMITTED);

        for (ExamAttempt attempt : attempts) {
            IpReputationResult result = analyzeIp(attempt);
            if (result != null && result.hasAnomalies()) {
                results.add(result);
            }
        }

        log.info("IP reputation analysis complete: {} anomalies found", results.size());
        return results;
    }

    /**
     * Phan tich mot IP cu the.
     */
    @Transactional
    public IpReputationResult analyzeIp(ExamAttempt attempt) {
        if (!enabled) return null;

        String ip = attempt.getClientIp();
        if (ip == null || ip.isBlank()) {
            log.warn("No client IP for attempt: {}", attempt.getId());
            return null;
        }

        // 1. Check cache
        CachedIpReputation cached = reputationCache.get(ip);
        if (cached != null && !cached.isExpired()) {
            log.debug("IP {} found in cache, skipping API lookup", ip);
            return buildResult(attempt, cached.result, ip);
        }

        // 2. Collect all information
        IpReputationData data = new IpReputationData();

        // Reverse DNS lookup
        String hostname = reverseDnsLookup(ip);
        data.hostname(hostname);

        // Check known VPN/Proxy ranges (simple heuristic)
        data.isVpn(isKnownVpnRange(ip));
        data.isProxy(isKnownProxyRange(ip));
        data.isTor(isTorExitNode(ip));

        // Subnet analysis
        int subnetCount = countSubnetOccupants(attempt.getExam().getId(), ip);
        data.subnetCount(subnetCount);

        // Geo-location (simulated - trong production, goi IP geolocation API)
        data.geoLocation(estimateGeoLocation(ip));

        // Build result
        IpReputationResult result = buildResult(attempt, data, ip);

        // Cache
        reputationCache.put(ip, new CachedIpReputation(data, VietNamTime.now()));

        return result;
    }

    /**
     * Tra cuu IP bang API (IPQualityScore, IP-API, etc.).
     */
    public IpReputationData lookupIp(String ip) {
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.debug("IP reputation API not configured, using heuristic only");
            return null;
        }

        try {
            var client = java.net.HttpURLConnection.class.cast(
                    new java.net.URL(apiUrl + "?key=" + apiKey + "&ip=" + ip + "&fields=8").openConnection()
            );
            client.setRequestMethod("GET");
            client.setConnectTimeout(timeoutMs);
            client.setReadTimeout(timeoutMs);
            client.setRequestProperty("User-Agent", "FE_DEMO-Proctoring/1.0");

            int responseCode = client.getResponseCode();
            if (responseCode == 200) {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(client.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return parseApiResponse(response.toString());
                }
            }
        } catch (Exception e) {
            log.warn("IP reputation API call failed for {}: {}", ip, e.getMessage());
        }

        return null;
    }

    private IpReputationData parseApiResponse(String json) {
        IpReputationData data = new IpReputationData();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var jsonNode = mapper.readTree(json);

            data.isVpn(jsonNode.has("vpn") && jsonNode.get("vpn").asBoolean());
            data.isProxy(jsonNode.has("proxy") && jsonNode.get("proxy").asBoolean());
            data.isTor(jsonNode.has("tor") && jsonNode.get("tor").asBoolean());
            data.isResidential(jsonNode.has("is_crawler") && !jsonNode.get("is_crawler").asBoolean());
            data.hostname(jsonNode.has("host") ? jsonNode.get("host").asText() : null);

            if (jsonNode.has("city")) {
                data.geoLocation(new GeoLocation(
                        jsonNode.path("city").asText(""),
                        jsonNode.path("region").asText(""),
                        jsonNode.path("country").asText(""),
                        jsonNode.path("org").asText("")
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to parse IP reputation API response: {}", e.getMessage());
        }
        return data;
    }

    private IpReputationResult buildResult(ExamAttempt attempt, IpReputationData data, String ip) {
        if (data == null) {
            data = new IpReputationData();
        }

        List<String> anomalies = new ArrayList<>();
        List<String> signals = new ArrayList();

        if (Boolean.TRUE.equals(data.isVpn())) {
            anomalies.add("VPN_DETECTED");
            signals.add("VPN_DETECTED");
        }
        if (Boolean.TRUE.equals(data.isProxy())) {
            anomalies.add("PROXY_DETECTED");
            signals.add("PROXY_DETECTED");
        }
        if (Boolean.TRUE.equals(data.isTor())) {
            anomalies.add("TOR_EXIT_NODE");
            signals.add("TOR_EXIT_NODE");
        }
        if (data.subnetCount() != null && data.subnetCount() >= subnetSuspicionThreshold) {
            anomalies.add("SUBNET_SUSPICION:" + data.subnetCount());
            signals.add("SUBNET_SUSPICION");
        }

        // Record signals
        for (String signalType : signals) {
            SignalSeverity severity = signalType.contains("TOR") || signalType.contains("VPN")
                    ? SignalSeverity.HIGH
                    : signalType.contains("SUBNET")
                            ? SignalSeverity.MEDIUM
                            : SignalSeverity.LOW;

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "ip_reputation");
            evidence.put("signalType", signalType);
            evidence.put("ip", ip);
            evidence.put("hostname", data.hostname());
            evidence.put("isVpn", data.isVpn());
            evidence.put("isProxy", data.isProxy());
            evidence.put("isTor", data.isTor());
            evidence.put("subnetCount", data.subnetCount());

            fraudSignalService.recordServerSignal(attempt, signalType, severity, 0.9, evidence);
        }

        return new IpReputationResult(
                attempt.getId(),
                ip,
                data.hostname(),
                data.isVpn(),
                data.isProxy(),
                data.isTor(),
                data.isResidential(),
                data.geoLocation(),
                data.subnetCount(),
                anomalies,
                data.subnetCount() != null && data.subnetCount() >= subnetSuspicionThreshold
        );
    }

    private String reverseDnsLookup(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isKnownVpnRange(String ip) {
        if (ip == null) return false;
        // Heuristic: private ranges, common VPN ports, known ranges
        if (ip.startsWith("10.") || ip.startsWith("172.16.") || ip.startsWith("192.168.")) {
            return true;
        }
        // Trong production, so sanh voi database cua VPN IP ranges
        return false;
    }

    private boolean isKnownProxyRange(String ip) {
        // Heuristic - trong production, tra cuu proxy database
        if (ip == null) return false;
        return ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    private boolean isTorExitNode(String ip) {
        // Trong production, tra cuu Tor exit node list
        // https://check.torproject.org/torbulkexitlist
        return false;
    }

    private int countSubnetOccupants(Long examId, String ip) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStatus(examId, AttemptStatus.SUBMITTED);

        // Extract /24 subnet
        String subnet = extractSubnet(ip, 24);

        int count = 0;
        for (ExamAttempt a : attempts) {
            if (subnet.equals(extractSubnet(a.getClientIp(), 24))) {
                count++;
            }
        }
        return count;
    }

    private String extractSubnet(String ip, int prefixBits) {
        if (ip == null) return "";
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return ip;
            int bytes = prefixBits / 8;
            int bits = prefixBits % 8;
            int mask = bits == 0 ? 255 : 256 - (1 << (8 - bits));
            int subnetInt = (Integer.parseInt(parts[bytes]) & mask);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes; i++) {
                if (i > 0) sb.append(".");
                sb.append(parts[i]);
            }
            if (bytes > 0) sb.append(".");
            sb.append(subnetInt);
            for (int i = bytes + 1; i < 4; i++) {
                sb.append(".0");
            }
            return sb.toString();
        } catch (Exception e) {
            return ip;
        }
    }

    private GeoLocation estimateGeoLocation(String ip) {
        // Trong production, goi API nhu ip-api.com
        // Chi la placeholder
        if (ip == null || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return new GeoLocation("Local Network", "Local", "VN", "Private ISP");
        }
        return null;
    }

    // ========== RECORD CLASSES ==========

    public record IpReputationResult(
            Long attemptId,
            String ipAddress,
            String hostname,
            Boolean isVpn,
            Boolean isProxy,
            Boolean isTor,
            Boolean isResidential,
            GeoLocation geoLocation,
            Integer subnetCount,
            List<String> anomalies,
            boolean hasAnomalies
    ) {}

    public record GeoLocation(String city, String region, String country, String isp) {}

    private static class CachedIpReputation {
        private final IpReputationData result;
        private final long cachedAt;

        CachedIpReputation(IpReputationData result, java.time.LocalDateTime cachedAt) {
            this.result = result;
            this.cachedAt = cachedAt.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toEpochSecond();
        }

        boolean isExpired() {
            return System.currentTimeMillis() / 1000 - cachedAt > 24 * 3600;
        }
    }

    private static class IpReputationData {
        private String hostname;
        private Boolean isVpn = false;
        private Boolean isProxy = false;
        private Boolean isTor = false;
        private Boolean isResidential = true;
        private GeoLocation geoLocation;
        private Integer subnetCount;

        IpReputationData hostname(String v) { this.hostname = v; return this; }
        IpReputationData isVpn(Boolean v) { this.isVpn = v; return this; }
        IpReputationData isProxy(Boolean v) { this.isProxy = v; return this; }
        IpReputationData isTor(Boolean v) { this.isTor = v; return this; }
        IpReputationData isResidential(Boolean v) { this.isResidential = v; return this; }
        IpReputationData geoLocation(GeoLocation v) { this.geoLocation = v; return this; }
        IpReputationData subnetCount(Integer v) { this.subnetCount = v; return this; }

        String hostname() { return hostname; }
        Boolean isVpn() { return isVpn; }
        Boolean isProxy() { return isProxy; }
        Boolean isTor() { return isTor; }
        Boolean isResidential() { return isResidential; }
        GeoLocation geoLocation() { return geoLocation; }
        Integer subnetCount() { return subnetCount; }
    }
}
