package com.example.demo.domain.entity;

public enum SignalSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public int baseScore() {
        return switch (this) {
            case LOW -> 10;
            case MEDIUM -> 25;
            case HIGH -> 40;
            case CRITICAL -> 80;
        };
    }
}
