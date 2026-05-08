package com.example.demo.service;

import java.util.Locale;

final class FraudSignalTypeNormalizer {

    private FraudSignalTypeNormalizer() {
    }

    static String canonical(String signalType) {
        String normalized = normalize(signalType);
        return switch (normalized) {
            case "BLUR" -> "WINDOW_BLUR";
            case "FULLSCREEN_EVASION", "FULLSCREEN_VIOLATION" -> "EXIT_FULLSCREEN";
            case "COPY", "PASTE", "COPY_ATTEMPT", "CUT_ATTEMPT", "PASTE_ATTEMPT", "LONG_PASTE",
                    "COPY_PASTE", "CLIPBOARD_ABUSE", "CLIPBOARD_BURST" -> "COPY_PASTE";
            case "DEVTOOLS", "DEVTOOLS_DETECTED", "INSPECT_ATTEMPT" -> "DEVTOOLS_OPEN";
            case "IP_ANOMALY" -> "IP_CHANGED";
            case "AI_MULTIPLE_FACES" -> "MULTIPLE_FACES";
            case "AI_FACE_MISSING" -> "FACE_NOT_DETECTED";
            case "AI_LOOKING_AWAY" -> "GAZE_OFF_SCREEN";
            default -> normalized;
        };
    }

    static String normalize(String signalType) {
        if (signalType == null || signalType.isBlank()) {
            return "UNKNOWN_SIGNAL";
        }
        return signalType.trim().toUpperCase(Locale.ROOT);
    }
}
