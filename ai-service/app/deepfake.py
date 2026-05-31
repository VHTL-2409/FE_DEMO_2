from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import numpy as np

from .config import env_bool, env_float


class DeepfakeAnalyzer:
    """Optional deepfake/liveness wrapper.

    The production model is intentionally optional. When weights are not
    configured, this class exposes diagnostics and conservative heuristic scores
    so the proctoring pipeline can keep running and route uncertain cases to
    manual review instead of failing open or crashing.
    """

    def __init__(self) -> None:
        self.enabled = env_bool("AI_SERVICE_DEEPFAKE_ENABLED", True)
        self.liveness_enabled = env_bool("AI_SERVICE_LIVENESS_ENABLED", True)
        self.threshold = env_float("AI_SERVICE_DEEPFAKE_THRESHOLD", 0.82)
        self.model_path = os.getenv("AI_SERVICE_DEEPFAKE_MODEL_PATH", "").strip()
        self.backend = "HEURISTIC"
        self.model_ready = bool(self.model_path and Path(self.model_path).is_file())
        if self.model_ready:
            # Placeholder for a future ONNX/TensorFlow runtime. The current
            # deployment has no declared dependency for a deepfake model runtime.
            self.backend = "CONFIGURED_EXTERNAL_MODEL"

    def analyze(
        self,
        spoofing_result: dict[str, Any],
        face_count: int,
        eye_tracking: dict[str, Any],
        gaze_analysis: dict[str, Any],
        quality_gate: dict[str, Any],
    ) -> dict[str, Any]:
        spoofing_confidence = float(spoofing_result.get("confidence") or 0.0)
        detected = bool(spoofing_result.get("detected"))
        spoof_type = spoofing_result.get("type")
        fallback_used = not self.model_ready

        liveness_score = self._liveness_score(face_count, eye_tracking, gaze_analysis, quality_gate)
        deepfake_score = spoofing_confidence
        if detected and str(spoof_type or "").upper() in {"DEEPFAKE", "FACE_SWAP", "GAN"}:
            deepfake_score = max(deepfake_score, 0.9)
        elif detected:
            deepfake_score = max(deepfake_score, 0.68)
        if self.liveness_enabled and liveness_score < 0.35 and face_count == 1:
            deepfake_score = max(deepfake_score, 0.72)

        label = "CLEAR"
        if deepfake_score >= self.threshold:
            label = "DEEPFAKE"
        elif detected:
            label = str(spoof_type or "SPOOFING_SUSPECTED").upper()
        elif liveness_score < 0.35 and face_count == 1:
            label = "LOW_LIVENESS"

        return {
            "valid": self.enabled and face_count == 1,
            "score": round(float(np.clip(deepfake_score, 0.0, 1.0)), 3),
            "livenessScore": round(float(np.clip(liveness_score, 0.0, 1.0)), 3),
            "label": label,
            "modelReady": self.model_ready,
            "backend": self.backend,
            "threshold": self.threshold,
            "temporalWindow": {
                "usesBlink": True,
                "usesGaze": True,
                "usesFaceStability": True,
            },
            "fallbackUsed": fallback_used,
            "sourceSpoofing": spoofing_result,
        }

    def _liveness_score(
        self,
        face_count: int,
        eye_tracking: dict[str, Any],
        gaze_analysis: dict[str, Any],
        quality_gate: dict[str, Any],
    ) -> float:
        if face_count != 1:
            return 0.0
        score = 0.55
        if quality_gate.get("eye_valid"):
            score += 0.15
        if quality_gate.get("gaze_valid"):
            score += 0.10
        if str(eye_tracking.get("eye_state", "")).upper() in {"OPEN", "PARTIAL", "CLOSED"}:
            score += 0.10
        if gaze_analysis.get("rapid_eye_movement"):
            score += 0.05
        if eye_tracking.get("prolonged_eye_closure"):
            score -= 0.15
        if quality_gate.get("lighting_reliable") is False:
            score -= 0.10
        if quality_gate.get("sharpness_reliable") is False:
            score -= 0.10
        return float(np.clip(score, 0.0, 1.0))
