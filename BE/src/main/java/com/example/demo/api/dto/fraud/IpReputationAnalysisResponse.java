package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class IpReputationAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalAttempts;
    private int uniqueIpCount;
    private List<IpResultItem> ipResults;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class IpResultItem {
        private String ipAddress;
        private Boolean isVpn;
        private Boolean isProxy;
        private Boolean isTor;
        private String hostname;
        private Map<String, String> geoLocation;
        private Integer subnetCount;
        private Integer attemptCount;
        private Integer studentCount;
        private List<String> studentUsernames;
        private String riskLevel;
    }
}
