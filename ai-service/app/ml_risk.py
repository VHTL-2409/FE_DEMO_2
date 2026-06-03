from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .schemas import MLRiskPredictionRequest, MLRiskPredictionResponse, MLRiskStatusResponse


ALGORITHM = "weighted_signal_risk_v1"
MODEL_VERSION = "ai-service-rule-risk-v1"
CONTRACT_VERSION = "ml-risk-v1"


@dataclass(frozen=True)
class WeightedFeature:
    name: str
    label: str
    weight: float
    group: str


SIGNAL_FEATURES = (
    WeightedFeature("tab_switch_count", "TAB_SWITCH", 1.0, "behavior"),
    WeightedFeature("window_blur_count", "WINDOW_BLUR", 0.8, "behavior"),
    WeightedFeature("fullscreen_exit_count", "EXIT_FULLSCREEN", 1.5, "behavior"),
    WeightedFeature("clipboard_attempts", "COPY_PASTE", 2.0, "behavior"),
    WeightedFeature("devtools_opened", "DEVTOOLS_OPEN", 3.0, "technical"),
    WeightedFeature("right_click_count", "RIGHT_CLICK", 0.8, "behavior"),
    WeightedFeature("print_screen_count", "PRINT_SCREEN", 2.0, "technical"),
    WeightedFeature("ip_changes", "IP_CHANGED", 3.0, "identity_network"),
    WeightedFeature("device_changes", "DEVICE_CHANGED", 4.0, "identity_network"),
    WeightedFeature("duplicate_ip_events", "DUPLICATE_IP", 4.0, "identity_network"),
    WeightedFeature("suspicious_signals", "AI_CAMERA_SIGNAL", 5.5, "camera_proctoring"),
    WeightedFeature("critical_signal_count", "CRITICAL_SIGNAL", 5.0, "severity"),
    WeightedFeature("high_signal_count", "HIGH_SIGNAL", 3.0, "severity"),
    WeightedFeature("medium_signal_count", "MEDIUM_SIGNAL", 1.5, "severity"),
)


class MLRiskEngine:
    """Deterministic risk baseline with explicit diagnostics.

    This is intentionally labeled as a heuristic baseline, not a trained model.
    It keeps the backend contract stable while leaving a clear replacement point
    for a real model later.
    """

    def status(self) -> MLRiskStatusResponse:
        return MLRiskStatusResponse(
            available=True,
            algorithm=ALGORITHM,
            version=MODEL_VERSION,
            trained_model_loaded=False,
            status="HEURISTIC_READY",
            message="ML risk uses a deterministic weighted-signal heuristic; no trained model is loaded.",
            diagnostics={
                "inputContractVersion": CONTRACT_VERSION,
                "scoringSource": "HEURISTIC_BASELINE",
                "supportsCamelCase": True,
                "supportsSnakeCase": True,
            },
        )

    def predict(self, req: MLRiskPredictionRequest) -> MLRiskPredictionResponse:
        contributions: list[dict[str, Any]] = []
        signals = req.signals

        for feature in SIGNAL_FEATURES:
            value = self._non_negative_number(getattr(signals, feature.name, 0))
            if value <= 0:
                continue
            contributions.append(self._contribution(feature.label, feature.group, value, feature.weight))

        signal_density = min(max(self._non_negative_number(signals.signals_per_minute), 0.0), 10.0)
        if signal_density > 0:
            contributions.append(self._contribution("SIGNALS_PER_MINUTE", "temporal", signal_density, 1.2))

        context = req.context or {}
        if self._truthy(context, "isSharedIp", "is_shared_ip"):
            contributions.append(self._contribution("SHARED_IP", "identity_network", 1.0, 6.0))
        if self._truthy(context, "isSharedDevice", "is_shared_device"):
            contributions.append(self._contribution("SHARED_DEVICE", "identity_network", 1.0, 6.0))
        if self._truthy(context, "isVpn", "is_vpn") or self._truthy(context, "isProxy", "is_proxy"):
            contributions.append(self._contribution("VPN_OR_PROXY", "identity_network", 1.0, 5.0))

        temporal = req.temporal or {}
        if self._truthy(temporal, "impossiblyFastAnswers", "impossibly_fast_answers"):
            contributions.append(self._contribution("IMPOSSIBLY_FAST_ANSWERS", "temporal", 1.0, 8.0))
        if self._truthy(temporal, "suspiciousPacing", "suspicious_pacing"):
            contributions.append(self._contribution("SUSPICIOUS_PACING", "temporal", 1.0, 4.0))

        behavior = req.behavior or {}
        if self._truthy(behavior, "typingPatternMismatch", "typing_pattern_mismatch"):
            contributions.append(self._contribution("TYPING_PATTERN_MISMATCH", "behavior", 1.0, 4.0))
        if self._truthy(behavior, "mouseSignatureAnomaly", "mouse_signature_anomaly"):
            contributions.append(self._contribution("MOUSE_SIGNATURE_ANOMALY", "behavior", 1.0, 4.0))

        score = round(max(0.0, min(100.0, sum(item["contribution"] for item in contributions))), 2)
        total_signal_count = max(int(self._non_negative_number(signals.total_signal_count)), 0)
        confidence, confidence_factors, warnings = self._confidence(total_signal_count, signals, contributions)

        diagnostics = {
            "algorithm": ALGORITHM,
            "modelVersion": MODEL_VERSION,
            "attemptId": req.attempt_id,
            "studentId": req.student_id,
            "examId": req.exam_id,
            "totalSignalCount": total_signal_count,
            "inputContractVersion": CONTRACT_VERSION,
            "trainedModelLoaded": False,
            "scoringSource": "HEURISTIC_BASELINE",
            "featureContributions": sorted(contributions, key=lambda item: item["contribution"], reverse=True),
            "confidenceFactors": confidence_factors,
            "warnings": warnings,
        }

        return MLRiskPredictionResponse(
            ml_score=score,
            confidence=confidence,
            risk_level=self._risk_level(score),
            fraud_probability=round(score / 100.0, 3),
            model_version=MODEL_VERSION,
            diagnostics=diagnostics,
        )

    def _confidence(
        self,
        total_signal_count: int,
        signals: Any,
        contributions: list[dict[str, Any]],
    ) -> tuple[float, list[str], list[str]]:
        confidence = 0.45
        factors: list[str] = ["heuristic_baseline"]
        warnings: list[str] = ["NO_TRAINED_MODEL_LOADED"]

        if total_signal_count <= 0:
            warnings.append("NO_SIGNALS_PROVIDED")
        elif total_signal_count < 5:
            confidence += 0.05
            warnings.append("LOW_SIGNAL_COUNT")
            factors.append("low_signal_count")
        elif total_signal_count < 15:
            confidence += 0.20
            factors.append("moderate_signal_count")
        else:
            confidence += 0.30
            factors.append("high_signal_count")

        if signals.critical_signal_count or signals.high_signal_count:
            confidence += 0.10
            factors.append("high_severity_signal_present")
        if signals.signals_per_minute > 0:
            confidence += 0.05
            factors.append("signal_density_available")
        if len(contributions) >= 3:
            confidence += 0.05
            factors.append("multiple_feature_groups")

        return round(max(0.1, min(0.95, confidence)), 3), factors, warnings

    def _risk_level(self, score: float) -> str:
        if score >= 81:
            return "CRITICAL"
        if score >= 61:
            return "HIGH_RISK"
        if score >= 40:
            return "SUSPICIOUS"
        return "CLEAN"

    def _contribution(self, name: str, group: str, value: float, weight: float) -> dict[str, Any]:
        return {
            "name": name,
            "group": group,
            "value": round(value, 3),
            "weight": weight,
            "contribution": round(value * weight, 3),
        }

    def _truthy(self, source: dict[str, Any], *keys: str) -> bool:
        for key in keys:
            value = source.get(key)
            if isinstance(value, bool):
                return value
            if value is not None and str(value).strip().lower() in {"1", "true", "yes", "on"}:
                return True
        return False

    def _non_negative_number(self, value: Any) -> float:
        try:
            return max(float(value or 0), 0.0)
        except (TypeError, ValueError):
            return 0.0
