package com.example.demo.service;

import com.example.demo.config.RiskSignalScoreProperties;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.FraudSignalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FraudSignalServiceTest {

    @Test
    void lowLivenessHasVisualSpoofingDescriptor() {
        FraudSignalService service = new FraudSignalService(
                mock(FraudSignalRepository.class),
                new ObjectMapper(),
                new RiskSignalScoreProperties()
        );

        FraudSignalService.SignalDescriptor descriptor = service.descriptorFor("LOW_LIVENESS");

        assertEquals("LOW_LIVENESS", descriptor.signalType());
        assertEquals("AI_SPOOFING", descriptor.category());
        assertEquals(SignalSeverity.HIGH, descriptor.severity());
        assertTrue(descriptor.riskImpact() >= 20);
    }

    @Test
    void spoofingAliasesNormalizeToCanonicalSignalTypes() {
        assertEquals("PRINTED_PHOTO", FraudSignalTypeNormalizer.canonical("photo_attack"));
        assertEquals("SCREEN_REPLAY", FraudSignalTypeNormalizer.canonical("video_replay"));
        assertEquals("SCREEN_DISPLAY", FraudSignalTypeNormalizer.canonical("display_attack"));
        assertEquals("FLAT_IMAGE", FraudSignalTypeNormalizer.canonical("flat_face"));
        assertEquals("DEEPFAKE", FraudSignalTypeNormalizer.canonical("model_spoof"));
        assertEquals("LOW_LIVENESS", FraudSignalTypeNormalizer.canonical("low_live_score"));
    }
}
