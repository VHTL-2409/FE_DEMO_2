from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import numpy as np

from .config import env_bool, env_float

try:
    import onnxruntime as ort
except Exception:
    ort = None

try:
    import cv2
except Exception:
    cv2 = None


DEEPFAKE_TYPES = {"DEEPFAKE", "FACE_SWAP", "GAN", "MODEL_SPOOF"}
PRINT_ATTACK_TYPES = {"PRINTED_PHOTO", "PHOTO_ATTACK", "PRINT_ATTACK", "PAPER_PHOTO"}
REPLAY_ATTACK_TYPES = {"SCREEN_REPLAY", "REPLAY_ATTACK", "VIDEO_REPLAY", "VIDEO_ATTACK"}
SCREEN_ATTACK_TYPES = {"SCREEN_DISPLAY", "DISPLAY_ATTACK", "SCREEN_ATTACK"}
FLAT_ATTACK_TYPES = {"FLAT_IMAGE", "FLAT_FACE", "FLAT_TEXTURE"}

DEFAULT_MODEL_FILENAME = "antispoof_minifasnet.onnx"
MINIFASNET_INPUT_SIZE = 80
MINIFASNET_CROP_SCALE = 2.7
MINIFASNET_CLASS_LIVE = 0
MINIFASNET_CLASS_PRINT = 1
MINIFASNET_CLASS_REPLAY = 2


def _resolve_default_model_path() -> str:
    candidates = [
        os.getenv("AI_SERVICE_ANTISPOOF_MODEL_PATH", "").strip(),
        os.getenv("AI_SERVICE_LIVENESS_MODEL_PATH", "").strip(),
        os.getenv("AI_SERVICE_DEEPFAKE_MODEL_PATH", "").strip(),
    ]
    for candidate in candidates:
        if candidate:
            return candidate
    base_dir = Path(__file__).resolve().parent
    return str(base_dir / "models" / DEFAULT_MODEL_FILENAME)


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
        self.low_liveness_threshold = env_float("AI_SERVICE_LOW_LIVENESS_THRESHOLD", 0.65)
        self.spoof_threshold = env_float("AI_SERVICE_ANTISPOOF_THRESHOLD", 0.78)
        self.model_path = _resolve_default_model_path()
        self.backend = "HEURISTIC"
        self.session = None
        self.input_name = None
        self.model_ready = False
        self.input_shape = None
        if self.model_path and Path(self.model_path).is_file() and ort is not None:
            try:
                self.session = ort.InferenceSession(self.model_path, providers=["CPUExecutionProvider"])
                self.input_name = self.session.get_inputs()[0].name
                self.input_shape = self.session.get_inputs()[0].shape
                self.model_ready = True
                self.backend = "ONNX"
            except Exception:
                self.session = None
                self.input_name = None
                self.model_ready = False

    def analyze(
        self,
        spoofing_result: dict[str, Any],
        face_count: int,
        eye_tracking: dict[str, Any],
        gaze_analysis: dict[str, Any],
        quality_gate: dict[str, Any],
        temporal_liveness: dict[str, Any] | None = None,
        face_crop: np.ndarray | None = None,
        face_bbox: tuple[int, int, int, int] | None = None,
    ) -> dict[str, Any]:
        spoofing_confidence = float(spoofing_result.get("confidence") or 0.0)
        detected = bool(spoofing_result.get("detected"))
        spoof_type = spoofing_result.get("type")
        fallback_used = not self.model_ready

        temporal_liveness = dict(temporal_liveness or {})
        model_result = self._run_model(face_crop, face_bbox)
        model_spoof_score = model_result.get("spoofScore")
        model_predicted_class = model_result.get("predictedClass")
        model_live_prob = model_result.get("liveProb")
        if model_spoof_score is not None:
            spoofing_confidence = max(spoofing_confidence, float(model_spoof_score))
            if model_spoof_score >= self.spoof_threshold:
                detected = True
                if model_predicted_class == MINIFASNET_CLASS_PRINT:
                    spoof_type = "PRINTED_PHOTO"
                elif model_predicted_class == MINIFASNET_CLASS_REPLAY:
                    spoof_type = "SCREEN_REPLAY"
                else:
                    spoof_type = spoof_type or "MODEL_SPOOF"
            elif model_live_prob is not None and model_live_prob < 0.4:
                spoofing_confidence = max(spoofing_confidence, 0.65)

        liveness_score = self._liveness_score(face_count, eye_tracking, gaze_analysis, quality_gate, temporal_liveness)
        deepfake_score = spoofing_confidence
        normalized_spoof_type = self._normalize_spoof_type(spoof_type)
        deepfake_like = normalized_spoof_type in DEEPFAKE_TYPES
        if detected and deepfake_like:
            deepfake_score = max(deepfake_score, 0.9)
        elif detected:
            deepfake_score = max(deepfake_score, 0.68)
        if self.liveness_enabled and liveness_score < self.low_liveness_threshold and face_count == 1:
            deepfake_score = max(deepfake_score, 0.72)

        label = "CLEAR"
        liveness_status = "LIVE"
        needs_review = False
        if not bool(quality_gate.get("lighting_reliable", True)) or not bool(quality_gate.get("sharpness_reliable", True)):
            liveness_status = "UNCERTAIN"
            needs_review = True
        if deepfake_like and deepfake_score >= self.threshold:
            label = "DEEPFAKE"
            liveness_status = "SPOOF"
            needs_review = True
        elif detected:
            label = normalized_spoof_type or "SPOOFING_SUSPECTED"
            liveness_status = "SPOOF"
            needs_review = True
        elif liveness_score < self.low_liveness_threshold and face_count == 1:
            label = "LOW_LIVENESS"
            if liveness_status != "UNCERTAIN":
                liveness_status = "SPOOF"
            needs_review = True

        return {
            "valid": self.enabled and face_count == 1,
            "score": round(float(np.clip(deepfake_score, 0.0, 1.0)), 3),
            "livenessScore": round(float(np.clip(liveness_score, 0.0, 1.0)), 3),
            "livenessStatus": liveness_status,
            "label": label,
            "spoofType": normalized_spoof_type,
            "rawSpoofType": spoof_type,
            "modelReady": self.model_ready,
            "backend": self.backend,
            "threshold": self.threshold,
            "lowLivenessThreshold": self.low_liveness_threshold,
            "spoofThreshold": self.spoof_threshold,
            "needsReview": needs_review,
            "temporalWindow": {
                "usesBlink": True,
                "usesGaze": True,
                "usesFaceStability": True,
                "sampleCount": temporal_liveness.get("sampleCount", 0),
                "windowMs": temporal_liveness.get("windowMs", 0),
            },
            "evidence": temporal_liveness,
            "model": model_result,
            "fallbackUsed": fallback_used,
            "sourceSpoofing": spoofing_result,
            "modelLiveProb": model_result.get("liveProb"),
            "modelPrintProb": model_result.get("printProb"),
            "modelReplayProb": model_result.get("replayProb"),
            "modelPredictedClass": model_result.get("predictedClass"),
        }

    def _normalize_spoof_type(self, spoof_type: Any) -> str:
        value = str(spoof_type or "").strip().upper()
        if not value:
            return ""
        if value in DEEPFAKE_TYPES:
            return "DEEPFAKE" if value == "MODEL_SPOOF" else value
        if value in PRINT_ATTACK_TYPES:
            return "PRINTED_PHOTO"
        if value in REPLAY_ATTACK_TYPES:
            return "SCREEN_REPLAY"
        if value in SCREEN_ATTACK_TYPES:
            return "SCREEN_DISPLAY"
        if value in FLAT_ATTACK_TYPES:
            return "FLAT_IMAGE"
        return value

    def _preprocess_minifasnet(
        self, rgb_array: np.ndarray, bbox: tuple[int, int, int, int]
    ) -> np.ndarray | None:
        """Preprocess face crop for MiniFASNetV2.

        Input: RGB numpy array (H, W, 3), face bbox (x, y, w, h).
        Output: (1, 3, 80, 80) float32 BGR normalized to [0, 1].
        """
        if cv2 is None:
            return None
        x, y, w, h = bbox
        h_img, w_img = rgb_array.shape[:2]
        s = min((h_img - 1) / max(h, 1), (w_img - 1) / max(w, 1), MINIFASNET_CROP_SCALE)
        new_w, new_h = w * s, h * s
        cx, cy = x + w / 2, y + h / 2
        x1 = max(0, int(cx - new_w / 2))
        y1 = max(0, int(cy - new_h / 2))
        x2 = min(w_img, int(cx + new_w / 2))
        y2 = min(h_img, int(cy + new_h / 2))
        crop = rgb_array[y1:y2, x1:x2]
        if crop.size == 0:
            crop = rgb_array
        resized = cv2.resize(crop, (MINIFASNET_INPUT_SIZE, MINIFASNET_INPUT_SIZE))
        bgr = cv2.cvtColor(resized, cv2.COLOR_RGB2BGR)
        blob = bgr.astype(np.float32) / 255.0
        blob = np.transpose(blob, (2, 0, 1))[None, ...]
        return blob

    def _run_model(
        self,
        face_crop: np.ndarray | None,
        face_bbox: tuple[int, int, int, int] | None = None,
    ) -> dict[str, Any]:
        if not self.model_ready or self.session is None or not self.input_name:
            return {"ready": self.model_ready, "used": False}

        if face_crop is None:
            return {"ready": True, "used": False, "reason": "no_face_crop"}

        try:
            if self.backend == "ONNX" and self.input_shape is not None:
                h_in, w_in = self.input_shape[2], self.input_shape[3]
                if h_in == w_in == MINIFASNET_INPUT_SIZE:
                    if face_bbox is None:
                        return {"ready": True, "used": False, "reason": "no_bbox_for_minifasnet"}
                    tensor = self._preprocess_minifasnet(face_crop, face_bbox)
                    if tensor is None:
                        return {"ready": True, "used": False, "reason": "cv2_unavailable"}
                else:
                    image = face_crop.astype("float32")
                    h, w = image.shape[:2]
                    target_h, target_w = h_in, w_in
                    y_idx = np.linspace(0, max(h - 1, 0), target_h).astype(int)
                    x_idx = np.linspace(0, max(w - 1, 0), target_w).astype(int)
                    resized = image[y_idx][:, x_idx]
                    tensor = np.transpose(resized / 255.0, (2, 0, 1))[None, ...].astype("float32")

            output = self.session.run(None, {self.input_name: tensor})[0]
            values = np.asarray(output).reshape(-1)

            if values.size == 3:
                probs = np.exp(values) / np.exp(values).sum()
                live_prob = float(probs[MINIFASNET_CLASS_LIVE])
                print_prob = float(probs[MINIFASNET_CLASS_PRINT])
                replay_prob = float(probs[MINIFASNET_CLASS_REPLAY])
                spoof_prob = max(print_prob, replay_prob)
                predicted_class = int(np.argmax(probs))
                spoof_score = float(spoof_prob)
            elif values.size > 1:
                live_prob = float(values[0]) if values[0] >= 0 else 0.0
                spoof_score = max(0.0, min(1.0, float(values[1])))
                print_prob = spoof_score * 0.6
                replay_prob = spoof_score * 0.4
                live_prob = 1.0 - spoof_score
                predicted_class = MINIFASNET_CLASS_LIVE if live_prob > spoof_score else MINIFASNET_CLASS_PRINT
            else:
                spoof_score = float(values[0]) if values.size == 1 else 0.0
                live_prob = 1.0 - spoof_score
                print_prob = spoof_score * 0.5
                replay_prob = spoof_score * 0.5
                predicted_class = MINIFASNET_CLASS_LIVE if live_prob > spoof_score else MINIFASNET_CLASS_PRINT

            return {
                "ready": True,
                "used": True,
                "spoofScore": round(spoof_score, 4),
                "liveProb": round(live_prob, 4),
                "printProb": round(print_prob, 4),
                "replayProb": round(replay_prob, 4),
                "predictedClass": predicted_class,
                "modelType": "MiniFASNetV2",
            }
        except Exception as exc:
            return {"ready": True, "used": False, "error": str(exc)}

    def _liveness_score(
        self,
        face_count: int,
        eye_tracking: dict[str, Any],
        gaze_analysis: dict[str, Any],
        quality_gate: dict[str, Any],
        temporal_liveness: dict[str, Any],
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
        if int(temporal_liveness.get("blinkCount") or 0) > 0:
            score += 0.12
        if float(temporal_liveness.get("faceMotionScore") or 0.0) >= 0.015:
            score += 0.10
        if float(temporal_liveness.get("pupilMotionScore") or 0.0) >= 0.015:
            score += 0.08
        if temporal_liveness.get("staticSequence"):
            score -= 0.28
        if eye_tracking.get("prolonged_eye_closure"):
            score -= 0.15
        if quality_gate.get("lighting_reliable") is False:
            score -= 0.10
        if quality_gate.get("sharpness_reliable") is False:
            score -= 0.10
        return float(np.clip(score, 0.0, 1.0))
