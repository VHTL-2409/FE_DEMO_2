from __future__ import annotations

import base64
import io
from typing import Any

import numpy as np
from PIL import Image

try:
    import cv2
except Exception:  # pragma: no cover - optional dependency guard
    cv2 = None


class ProctorAnalyzer:
    def __init__(self) -> None:
        self._face_cascade = None
        if cv2 is not None:
            cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
            self._face_cascade = cv2.CascadeClassifier(cascade_path)

    def cv_ready(self) -> bool:
        return cv2 is not None and self._face_cascade is not None and not self._face_cascade.empty()

    def analyze_frame(self, image_base64: str, metadata: dict[str, Any] | None = None) -> dict[str, Any]:
        image = self._decode_image(image_base64)
        rgb_array = np.array(image)
        gray = np.array(image.convert("L"))
        face_count = self._detect_faces(gray)
        average_brightness = float(gray.mean()) if gray.size else 0.0
        variance = float(gray.var()) if gray.size else 0.0

        signals = []
        if self.cv_ready() and face_count == 0:
            signals.append(self._signal("FACE_NOT_DETECTED", "MEDIUM", 0.68, {"faceCount": 0}))
        elif self.cv_ready() and face_count > 1:
            signals.append(self._signal("MULTIPLE_FACES", "HIGH", 0.93, {"faceCount": face_count}))

        if average_brightness < 45:
            signals.append(self._signal("LOW_LIGHTING", "LOW", 0.58, {"averageBrightness": round(average_brightness, 2)}))

        if variance < 80:
            signals.append(self._signal("BLURRY_FRAME", "LOW", 0.55, {"variance": round(variance, 2)}))

        return {
            "status": "DONE",
            "face_count": int(face_count),
            "average_brightness": round(average_brightness, 2),
            "signals": signals,
            "diagnostics": {
                "cv_ready": self.cv_ready(),
                "image_width": int(rgb_array.shape[1]) if rgb_array.ndim >= 2 else 0,
                "image_height": int(rgb_array.shape[0]) if rgb_array.ndim >= 2 else 0,
                "metadata": metadata or {},
            },
        }

    def analyze_behavior(
        self,
        paste_length: int = 0,
        tab_switch_count: int = 0,
        idle_seconds: int = 0,
        typing_intervals: list[int] | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        intervals = [value for value in (typing_intervals or []) if value is not None and value >= 0]
        signals = []

        if paste_length >= 50:
            signals.append(self._signal("COPY_PASTE_DETECTED", "MEDIUM", 0.82, {"pasteLength": paste_length}))
        if tab_switch_count >= 3:
            signals.append(self._signal("TAB_SWITCH_CLUSTER", "MEDIUM", 0.78, {"tabSwitchCount": tab_switch_count}))
        if idle_seconds >= 120:
            signals.append(self._signal("IDLE_EXCESSIVE", "LOW", 0.61, {"idleSeconds": idle_seconds}))

        diagnostics = {"metadata": metadata or {}}
        if intervals:
            avg_interval = sum(intervals) / len(intervals)
            diagnostics["avgTypingIntervalMs"] = round(avg_interval, 2)
            if avg_interval < 40:
                signals.append(self._signal("TYPING_ANOMALY", "MEDIUM", 0.66, {"avgIntervalMs": round(avg_interval, 2)}))

        return {
            "status": "DONE",
            "signals": signals,
            "diagnostics": diagnostics,
        }

    def _detect_faces(self, gray_image: np.ndarray) -> int:
        if not self.cv_ready():
            return 0
        faces = self._face_cascade.detectMultiScale(gray_image, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))
        return len(faces)

    def _decode_image(self, image_base64: str) -> Image.Image:
        raw = image_base64.split(",", 1)[1] if "," in image_base64 else image_base64
        image_bytes = base64.b64decode(raw)
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")

    def _signal(self, signal_type: str, severity: str, confidence: float, evidence: dict[str, Any]) -> dict[str, Any]:
        return {
            "signal_type": signal_type,
            "severity": severity,
            "confidence": confidence,
            "evidence": evidence,
        }
