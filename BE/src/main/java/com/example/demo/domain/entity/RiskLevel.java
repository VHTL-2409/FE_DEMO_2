package com.example.demo.domain.entity;

public enum RiskLevel {
    CLEAN,
    SUSPICIOUS,
    HIGH_RISK,
    CRITICAL;

    public boolean isSuspicious() {
        return this != CLEAN;
    }
}
