package com.example.demo.service;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // If behind a reverse proxy, prefer X-Forwarded-For (first hop).
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            String trimmed = realIp.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }

        return request.getRemoteAddr();
    }
}

