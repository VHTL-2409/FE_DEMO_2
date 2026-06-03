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
            case "AI_DEEPFAKE", "MODEL_SPOOF" -> "DEEPFAKE";
            case "AI_PRINTED_PHOTO", "PHOTO_ATTACK", "PRINT_ATTACK", "PAPER_PHOTO" -> "PRINTED_PHOTO";
            case "AI_SCREEN_REPLAY", "REPLAY_ATTACK", "VIDEO_REPLAY", "VIDEO_ATTACK" -> "SCREEN_REPLAY";
            case "AI_SCREEN_DISPLAY", "DISPLAY_ATTACK", "SCREEN_ATTACK" -> "SCREEN_DISPLAY";
            case "AI_FLAT_IMAGE", "FLAT_FACE", "FLAT_TEXTURE" -> "FLAT_IMAGE";
            case "AI_LOW_LIVENESS", "LOW_LIVE_SCORE" -> "LOW_LIVENESS";
            case "AI_NO_MIC", "NO_MICROPHONE", "MIC_OFF", "MIC_DISABLED", "MICROPHONE_OFF" -> "NO_MIC";
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
