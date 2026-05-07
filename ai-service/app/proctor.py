from __future__ import annotations

import base64
import io
import logging
import os
import time
from datetime import datetime
from itertools import combinations
from pathlib import Path
from typing import Any
from urllib.request import Request as UrlRequest, urlopen

import numpy as np
from PIL import Image

try:
    import cv2
    DNN_AVAILABLE = hasattr(cv2, 'dnn')
    # Additional check for advanced OpenCV features
    FACE_LANDMARK_AVAILABLE = hasattr(cv2, 'face')
except Exception:
    cv2 = None
    DNN_AVAILABLE = False
    FACE_LANDMARK_AVAILABLE = False


logger = logging.getLogger(__name__)

FACE_MODEL_DIR = Path(__file__).resolve().parent / "models"
FACE_DNN_PROTO_NAME = "deploy.prototxt.txt"
FACE_DNN_MODEL_NAME = "res10_300x300_ssd_iter_140000.caffemodel"
FACE_DNN_PROTO_URL = "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt"
FACE_DNN_MODEL_URL = "https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel"


class ProctorAnalyzer:
    """Advanced Proctor Analyzer with Deep Learning-based Face Detection,
    Eye Tracking, and Spoofing Detection."""

    def __init__(self) -> None:
        self._face_cascade = None
        self._eye_cascade = None
        self._smile_cascade = None
        self._profile_cascade = None
        self._dnn_net = None
        self._dnn_model_type = None
        self._dnn_confidence_threshold = self._env_float("AI_SERVICE_FACE_DETECTION_THRESHOLD", 0.55)
        self._face_detector_backend = "UNAVAILABLE"
        self._face_detector_error = None
        self._face_model_auto_download = self._env_bool("AI_SERVICE_FACE_MODEL_AUTO_DOWNLOAD", True)
        self._face_model_download_timeout = int(self._env_float("AI_SERVICE_FACE_MODEL_DOWNLOAD_TIMEOUT_SECONDS", 12))
        self._eye_landmarks = None
        self._face_landmark_detector = None
        self._spoofing_model = None
        self._spoofing_dl = False
        self._tracking_state: dict[str, dict[str, Any]] = {}
        self._tracking_state_ttl_seconds = 180
        self._max_tracking_states = 256
        self._gaze_required_frames = 3
        self._gaze_required_ms = 2500
        self._eye_closure_required_frames = 2
        self._eye_closure_required_ms = 2000

        if cv2 is not None:
            self._init_haar_cascades()
            self._init_dnn_face_detection()
            self._init_eye_landmark_detector()
            self._init_spoofing_detector()
        else:
            self._face_detector_error = "OpenCV is not available"

    def _env_bool(self, name: str, default: bool) -> bool:
        raw = os.getenv(name)
        if raw is None:
            return default
        return raw.strip().lower() in {"1", "true", "yes", "on"}

    def _env_float(self, name: str, default: float) -> float:
        raw = os.getenv(name)
        if raw is None or raw.strip() == "":
            return default
        try:
            return float(raw)
        except ValueError:
            return default

    def _init_haar_cascades(self) -> None:
        """Initialize Haar cascade classifiers."""
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self._face_cascade = cv2.CascadeClassifier(cascade_path)

        eye_path = cv2.data.haarcascades + "haarcascade_eye.xml"
        self._eye_cascade = cv2.CascadeClassifier(eye_path)

        smile_path = cv2.data.haarcascades + "haarcascade_smile.xml"
        self._smile_cascade = cv2.CascadeClassifier(smile_path)

        profile_path = cv2.data.haarcascades + "haarcascade_profileface.xml"
        self._profile_cascade = cv2.CascadeClassifier(profile_path)

    def _init_dnn_face_detection(self) -> None:
        """Initialize DNN-based face detection."""
        if not DNN_AVAILABLE:
            self._face_detector_backend = "UNAVAILABLE"
            self._face_detector_error = "OpenCV DNN is not available"
            return

        model_dir = FACE_MODEL_DIR
        model_dir.mkdir(parents=True, exist_ok=True)
        proto_path = model_dir / FACE_DNN_PROTO_NAME
        model_path = model_dir / FACE_DNN_MODEL_NAME

        if self._face_model_auto_download:
            self._ensure_face_model_files(proto_path, model_path)

        try:
            if self._is_valid_file(proto_path) and self._is_valid_file(model_path):
                self._dnn_net = cv2.dnn.readNetFromCaffe(str(proto_path), str(model_path))
                self._dnn_model_type = "CAFFE"
                self._configure_dnn_net()
                self._face_detector_backend = "DNN_CAFFE"
                self._face_detector_error = None
                return

            tf_proto = model_dir / "opencv_face_detector.pbtxt"
            tf_model = model_dir / "opencv_face_detector_uint8.pb"
            if self._is_valid_file(tf_proto) and self._is_valid_file(tf_model):
                self._dnn_net = cv2.dnn.readNetFromTensorflow(str(tf_model), str(tf_proto))
                self._dnn_model_type = "TENSORFLOW"
                self._configure_dnn_net()
                self._face_detector_backend = "DNN_TENSORFLOW"
                self._face_detector_error = None
                return
            self._face_detector_backend = "HAAR_FALLBACK"
            self._face_detector_error = (
                f"Missing face detector model files in {model_dir}. "
                f"Expected {FACE_DNN_PROTO_NAME} and {FACE_DNN_MODEL_NAME}."
            )
        except Exception as exc:
            self._dnn_net = None
            self._dnn_model_type = None
            self._face_detector_backend = "HAAR_FALLBACK"
            self._face_detector_error = str(exc)
            logger.warning("Failed to initialize DNN face detector: %s", exc)

    def _configure_dnn_net(self) -> None:
        if self._dnn_net is None:
            return
        try:
            backend = getattr(cv2.dnn, "DNN_BACKEND_OPENCV", None)
            target = getattr(cv2.dnn, "DNN_TARGET_CPU", None)
            if backend is not None:
                self._dnn_net.setPreferableBackend(backend)
            if target is not None:
                self._dnn_net.setPreferableTarget(target)
        except Exception as exc:
            logger.debug("Unable to configure DNN backend/target: %s", exc)

    def _ensure_face_model_files(self, proto_path: Path, model_path: Path) -> None:
        downloads = [
            (FACE_DNN_PROTO_URL, proto_path, 1_000),
            (FACE_DNN_MODEL_URL, model_path, 1_000_000),
        ]
        for url, path, min_bytes in downloads:
            if self._is_valid_file(path, min_bytes=min_bytes):
                continue
            self._download_binary(url, path)

    def _download_binary(self, url: str, target_path: Path) -> None:
        target_path.parent.mkdir(parents=True, exist_ok=True)
        temp_path = target_path.with_suffix(target_path.suffix + ".download")
        try:
            request = UrlRequest(url, headers={"User-Agent": "Mozilla/5.0"})
            with urlopen(request, timeout=self._face_model_download_timeout) as response, open(temp_path, "wb") as out_file:
                while True:
                    chunk = response.read(1024 * 64)
                    if not chunk:
                        break
                    out_file.write(chunk)
            if not temp_path.exists() or temp_path.stat().st_size == 0:
                raise ValueError("download returned an empty file")
            temp_path.replace(target_path)
            logger.info("Downloaded face detector model: %s", target_path.name)
        except Exception as exc:
            try:
                if temp_path.exists():
                    temp_path.unlink()
            except Exception:
                pass
            logger.warning("Unable to download %s: %s", target_path.name, exc)

    def _is_valid_file(self, path: Path, min_bytes: int = 1) -> bool:
        try:
            return path.exists() and path.is_file() and path.stat().st_size >= min_bytes
        except Exception:
            return False
        self._dnn_net = None

    def _init_eye_landmark_detector(self) -> None:
        """Initialize eye landmark detector (if available)."""
        # Try to use dlib or OpenCV's face module if available
        if FACE_LANDMARK_AVAILABLE:
            try:
                # OpenCV's face module provides face landmark detection
                self._face_landmark_detector = cv2.face.createFacemarkLBF()
                self._eye_landmarks = True
            except Exception:
                self._eye_landmarks = False
        else:
            self._eye_landmarks = False

    def _init_spoofing_detector(self) -> None:
        """Initialize deep learning-based spoofing detector."""
        # Try to load pre-trained spoofing model if available
        base_dir = os.path.dirname(__file__)
        spoofing_model_path = os.path.join(base_dir, "models", "antispoofing_model.json")
        spoofing_weights_path = os.path.join(base_dir, "models", "antispoofing_weights.h5")

        if os.path.exists(spoofing_model_path) and os.path.exists(spoofing_weights_path):
            try:
                # Load deep learning model for anti-spoofing
                self._spoofing_model = cv2.dnn.readNetFromTensorflow(spoofing_weights_path, spoofing_model_path)
                self._spoofing_dl = True
            except Exception:
                self._spoofing_model = None
                self._spoofing_dl = False
        else:
            self._spoofing_model = None
            self._spoofing_dl = False

    def cv_ready(self) -> bool:
        return cv2 is not None and self._face_cascade is not None and not self._face_cascade.empty()

    def dnn_ready(self) -> bool:
        return self._dnn_net is not None

    def is_ready(self) -> bool:
        return self.cv_ready() or self.dnn_ready()

    def face_detector_status(self) -> dict[str, Any]:
        return {
            "ready": self.is_ready(),
            "backend": getattr(self, "_face_detector_backend", "UNAVAILABLE"),
            "dnnReady": self.dnn_ready(),
            "cvReady": self.cv_ready(),
            "modelType": getattr(self, "_dnn_model_type", None),
            "error": getattr(self, "_face_detector_error", None),
            "autoDownload": getattr(self, "_face_model_auto_download", False),
            "modelDir": str(FACE_MODEL_DIR),
        }

    def eye_landmark_ready(self) -> bool:
        return self._eye_landmarks and self._face_landmark_detector is not None

    def spoofing_dl_ready(self) -> bool:
        return self._spoofing_dl and self._spoofing_model is not None

    def analyze_frame(self, image_base64: str, metadata: dict[str, Any] | None = None) -> dict[str, Any]:
        """Comprehensive frame analysis including face detection, eye tracking, and spoofing detection."""
        metadata = dict(metadata or {})
        camera_state = self._resolve_camera_state(metadata)
        if camera_state["camera_off"]:
            return self._build_camera_off_response(metadata, camera_state)
        sample_time = self._resolve_sample_time(metadata)
        tracking_key = self._resolve_tracking_key(metadata)

        image = self._decode_image(image_base64)
        rgb_array = np.array(image)
        gray = np.array(image.convert("L"))

        # Face detection
        face_count, face_locations, detection_method = self._detect_face_locations(rgb_array, gray)

        # Eye tracking with landmark detection
        eye_tracking = self._track_eyes(rgb_array, gray, face_locations)

        # Brightness and quality analysis
        average_brightness = float(gray.mean()) if gray.size else 0.0
        variance = float(gray.var()) if gray.size else 0.0

        # Advanced spoofing detection with deep learning
        spoofing_result = self._detect_spoofing_deep(rgb_array, gray, face_count, face_locations)

        # Occlusion detection
        occlusion_result = self._detect_occlusion(gray, face_locations, eye_tracking)

        # Face position analysis
        position_analysis = self._analyze_face_position(face_locations, gray.shape)

        # Gaze analysis
        gaze_analysis = self._analyze_gaze(eye_tracking, face_locations, gray.shape)
        temporal_tracking = self._update_tracking_state(
            tracking_key,
            sample_time,
            face_count,
            eye_tracking,
            gaze_analysis,
        )
        eye_tracking.update(temporal_tracking.get("eye_tracking", {}))
        gaze_analysis.update(temporal_tracking.get("gaze_analysis", {}))

        signals = []

        # === FACE DETECTION SIGNALS ===
        if (self.dnn_ready() or self.cv_ready()) and face_count == 0:
            signals.append(self._signal(
                "FACE_NOT_DETECTED", "HIGH", 0.92,
                {"faceCount": 0, "reason": "No face found in frame",
                 "recommendation": "Ensure face is clearly visible and well-lit",
                 "detectionMethod": detection_method}
            ))

        elif (self.dnn_ready() or self.cv_ready()) and face_count > 1:
            signals.append(self._signal(
                "MULTIPLE_FACES", "CRITICAL", 0.98,
                {"faceCount": face_count, "reason": f"{face_count} faces detected",
                 "recommendation": "Only one person should be in frame",
                 "detectionMethod": detection_method}
            ))

        elif (self.dnn_ready() or self.cv_ready()) and face_count == 1:
            face_size = face_locations[0][2] * face_locations[0][3] if face_locations else 0
            frame_size = gray.shape[0] * gray.shape[1]
            face_ratio = face_size / frame_size if frame_size > 0 else 0

            if face_ratio < 0.03:
                signals.append(self._signal("FACE_TOO_FAR", "MEDIUM", 0.80,
                    {"faceRatio": round(face_ratio * 100, 2),
                     "reason": "Face is too small", "recommendation": "Move closer to camera"}))
            elif face_ratio > 0.5:
                signals.append(self._signal("FACE_TOO_CLOSE", "LOW", 0.72,
                    {"faceRatio": round(face_ratio * 100, 2),
                     "reason": "Face is too close", "recommendation": "Move back to show shoulders"}))

        # === SPOOFING DETECTION ===
        if spoofing_result["detected"]:
            signals.append(self._signal(
                "FACE_SPOOFING_SUSPECTED", "CRITICAL", spoofing_result["confidence"],
                {"spoofingType": spoofing_result["type"],
                 "reason": f"Possible {spoofing_result['type']} detected",
                 "dlEnhanced": spoofing_result.get("dl_enhanced", False),
                 "recommendation": "Verify identity with manual review"}
            ))

        # === OCCLUSION DETECTION ===
        if occlusion_result["mask_detected"]:
            signals.append(self._signal("FACE_OBSTRUCTED_MASK", "HIGH", 0.92,
                {"reason": "Face mask detected", "recommendation": "Remove mask for verification"}))

        if occlusion_result["sunglasses_detected"]:
            signals.append(self._signal("EYES_OBSTRUCTED", "MEDIUM", 0.80,
                {"reason": "Sunglasses or reflective glasses detected",
                 "recommendation": "Remove sunglasses for eye tracking"}))

        if occlusion_result["partial_face"]:
            signals.append(self._signal("PARTIAL_FACE_VISIBLE", "MEDIUM", 0.78,
                {"reason": "Face partially visible or turned away",
                 "coverage": occlusion_result.get("coverage_percent", 0),
                 "recommendation": "Position face to show full front view"}))

        # === EYE TRACKING SIGNALS ===
        if eye_tracking["eye_count"] == 0 and face_count == 1:
            signals.append(self._signal("EYES_NOT_DETECTED", "MEDIUM", 0.75,
                {"reason": "Eyes not detectable", "recommendation": "Face the camera directly"}))

        # Eye blink anomaly
        if eye_tracking.get("blink_anomaly"):
            signals.append(self._signal("EYE_BLINK_ANOMALY", "MEDIUM", 0.72,
                {"blinkRate": eye_tracking.get("blink_rate", 0),
                 "reason": "Abnormal blinking pattern detected",
                 "recommendation": "Normal blinking patterns expected"}))

        # Eye closure detection
        if eye_tracking.get("prolonged_eye_closure"):
            signals.append(self._signal("EYES_CLOSED_PROLONGED", "LOW", 0.65,
                {"closureDuration": eye_tracking.get("closure_duration", 0),
                 "closureDurationMs": eye_tracking.get("closure_duration_ms", 0),
                 "reason": "Eyes closed for extended period",
                 "recommendation": "Keep eyes open during exam"}))

        # === GAZE TRACKING SIGNALS ===
        if gaze_analysis["gaze_off_screen"]:
            signals.append(self._signal("GAZE_OFF_SCREEN", "HIGH", gaze_analysis["gaze_confidence"],
                {"gazeDirection": gaze_analysis.get("primary_direction", "UNKNOWN"),
                 "duration": gaze_analysis.get("off_duration", 0),
                 "durationMs": gaze_analysis.get("off_duration_ms", 0),
                 "reason": "Looking away from screen",
                 "recommendation": "Focus on the exam content"}))

        if gaze_analysis["rapid_eye_movement"]:
            signals.append(self._signal("RAPID_EYE_MOVEMENT", "MEDIUM", 0.68,
                {"movementCount": gaze_analysis.get("movement_count", 0),
                 "reason": "Rapid eye movements detected",
                 "recommendation": "Steady gaze expected"}))

        # === LIGHTING SIGNALS ===
        if average_brightness < 40:
            signals.append(self._signal("VERY_LOW_LIGHTING", "HIGH", 0.85,
                {"averageBrightness": round(average_brightness, 2),
                 "reason": "Lighting is too dark",
                 "recommendation": "Improve lighting conditions"}))
        elif average_brightness < 60:
            signals.append(self._signal("LOW_LIGHTING", "MEDIUM", 0.68,
                {"averageBrightness": round(average_brightness, 2),
                 "reason": "Lighting could be improved",
                 "recommendation": "Add more light source"}))
        elif average_brightness > 240:
            signals.append(self._signal("OVEREXPOSED_FRAME", "LOW", 0.60,
                {"averageBrightness": round(average_brightness, 2),
                 "reason": "Image is too bright",
                 "recommendation": "Avoid direct light source behind you"}))

        # === BLUR/QUALITY SIGNALS ===
        if variance < 50:
            signals.append(self._signal("VERY_BLURRY_FRAME", "HIGH", 0.88,
                {"variance": round(variance, 2),
                 "reason": "Frame is very blurry",
                 "recommendation": "Ensure camera is stable and focused"}))
        elif variance < 80:
            signals.append(self._signal("BLURRY_FRAME", "LOW", 0.60,
                {"variance": round(variance, 2),
                 "reason": "Frame has some blur",
                 "recommendation": "Stabilize camera"}))

        # === POSITION SIGNALS ===
        if position_analysis["face_turned"]:
            signals.append(self._signal("FACE_TURNED_AWAY", "MEDIUM", 0.78,
                {"turnAngle": position_analysis.get("turn_angle", 0),
                 "reason": "Face is not facing camera directly",
                 "recommendation": "Face the camera directly"}))

        if position_analysis["face_not_centered"]:
            signals.append(self._signal("FACE_NOT_CENTERED", "LOW", 0.58,
                {"centerOffset": position_analysis.get("center_offset", {}),
                 "reason": "Face is not centered in frame",
                 "recommendation": "Center your face in the camera"}))

        return {
            "status": "DONE",
            "face_count": int(face_count),
            "eye_count": int(eye_tracking["eye_count"]),
            "face_detected": face_count > 0,
            "multiple_faces": face_count > 1,
            "face_quality": self._assess_face_quality(face_count, eye_tracking["eye_count"], average_brightness, variance),
            "average_brightness": round(average_brightness, 2),
            "frame_quality": self._assess_frame_quality(average_brightness, variance),
            "signals": signals,
            "warnings": self._generate_warnings(signals),
            # Eye tracking fields
            "eye_state": eye_tracking.get("eye_state", "UNKNOWN"),
            "eye_aspect_ratio": eye_tracking.get("eye_aspect_ratio", 0),
            "blink_rate": eye_tracking.get("blink_rate", 0),
            "eye_tracking_confidence": eye_tracking.get("tracking_confidence", 0.0),
            "closure_duration_ms": eye_tracking.get("closure_duration_ms", 0),
            # Gaze analysis fields
            "gaze_direction": gaze_analysis.get("primary_direction", "CENTER"),
            "gaze_off_screen": gaze_analysis.get("gaze_off_screen", False),
            "attention_score": gaze_analysis.get("attention_score", 1.0),
            "gaze_confidence": gaze_analysis.get("gaze_confidence", 0.0),
            "off_screen_duration_ms": gaze_analysis.get("off_duration_ms", 0),
            "visual_overlay": self._build_visual_overlay(rgb_array, face_locations, eye_tracking, gaze_analysis),
            "diagnostics": {
                "cv_ready": self.cv_ready(),
                "dnn_ready": self.dnn_ready(),
                "eye_landmark_ready": self.eye_landmark_ready(),
                "spoofing_dl_ready": self.spoofing_dl_ready(),
                "detection_method": detection_method,
                "face_detector": self.face_detector_status(),
                "image_width": int(rgb_array.shape[1]) if rgb_array.ndim >= 2 else 0,
                "image_height": int(rgb_array.shape[0]) if rgb_array.ndim >= 2 else 0,
                "face_locations": [list(loc) for loc in face_locations] if face_locations else [],
                "spoofing_check": spoofing_result,
                "occlusion_check": occlusion_result,
                "position_analysis": position_analysis,
                "eye_tracking": eye_tracking,
                "gaze_analysis": gaze_analysis,
                "metadata": metadata or {},
            },
        }

    def _build_visual_overlay(
        self,
        rgb_array: np.ndarray,
        face_locations: list,
        eye_tracking: dict,
        gaze_analysis: dict,
    ) -> dict[str, Any]:
        image_height = int(rgb_array.shape[0]) if rgb_array.ndim >= 2 else 0
        image_width = int(rgb_array.shape[1]) if rgb_array.ndim >= 2 else 0

        face_boxes = []
        for face in face_locations or []:
            normalized = self._normalize_overlay_box(face)
            if normalized is not None:
                face_boxes.append(normalized)

        eye_boxes = []
        pupil_points = []
        for side_key in ("left_eye", "right_eye"):
            eye_detail = eye_tracking.get(side_key)
            if not isinstance(eye_detail, dict):
                continue

            eye_box = self._normalize_overlay_box(eye_detail.get("box"))
            if eye_box is not None:
                eye_box["side"] = side_key.replace("_eye", "")
                eye_boxes.append(eye_box)

            pupil_point = self._normalize_overlay_point(eye_detail.get("pupil_center"))
            if pupil_point is not None:
                pupil_point["side"] = side_key.replace("_eye", "")
                pupil_points.append(pupil_point)

        if not pupil_points:
            for index, point in enumerate(eye_tracking.get("landmarks") or []):
                normalized_point = self._normalize_overlay_point(point)
                if normalized_point is not None:
                    normalized_point["side"] = f"eye_{index + 1}"
                    pupil_points.append(normalized_point)

        face_count = len(face_boxes)
        eye_count = int(eye_tracking.get("eye_count") or len(eye_boxes) or 0)
        eye_state = str(eye_tracking.get("eye_state", "UNKNOWN")).upper()
        prolonged_eye_closure = bool(eye_tracking.get("prolonged_eye_closure"))
        gaze_off_screen = bool(gaze_analysis.get("gaze_off_screen"))

        status = "TRACKING"
        label = "Đang theo dõi"
        tone = "success"
        if face_count == 0:
            status = "NO_FACE"
            label = "Không thấy mặt"
            tone = "danger"
        elif face_count > 1:
            status = "MULTIPLE_FACES"
            label = "Nhiều khuôn mặt"
            tone = "danger"
        elif eye_count == 0 or not eye_boxes:
            status = "NO_EYES"
            label = "Không thấy mắt"
            tone = "warning"
        elif gaze_off_screen:
            status = "GAZE_AWAY"
            label = "Nhìn lệch"
            tone = "danger"
        elif prolonged_eye_closure or eye_state == "CLOSED":
            status = "EYES_CLOSED"
            label = "Mắt nhắm"
            tone = "warning"

        return {
            "imageWidth": image_width,
            "imageHeight": image_height,
            "faceBoxes": face_boxes,
            "eyeBoxes": eye_boxes,
            "pupilPoints": pupil_points,
            "status": status,
            "label": label,
            "tone": tone,
        }

    def _normalize_overlay_box(self, box: Any) -> dict[str, Any] | None:
        if isinstance(box, dict):
            x = box.get("x", box.get("left"))
            y = box.get("y", box.get("top"))
            width = box.get("width", box.get("w"))
            height = box.get("height", box.get("h"))
        elif isinstance(box, (list, tuple)) and len(box) >= 4:
            x, y, width, height = box[:4]
        else:
            return None

        try:
            x_i = int(round(float(x)))
            y_i = int(round(float(y)))
            width_i = int(round(float(width)))
            height_i = int(round(float(height)))
        except Exception:
            return None

        if width_i <= 0 or height_i <= 0:
            return None

        return {
            "x": x_i,
            "y": y_i,
            "width": width_i,
            "height": height_i,
        }

    def _normalize_overlay_point(self, point: Any) -> dict[str, Any] | None:
        if isinstance(point, dict):
            x = point.get("x", point.get("left"))
            y = point.get("y", point.get("top"))
        elif isinstance(point, (list, tuple)) and len(point) >= 2:
            x, y = point[:2]
        else:
            return None

        try:
            return {
                "x": int(round(float(x))),
                "y": int(round(float(y))),
            }
        except Exception:
            return None

    # === Advanced Eye Tracking ===
    def _track_eyes(self, rgb_array: np.ndarray, gray: np.ndarray, face_locations: list) -> dict:
        """Track eyes with OpenCV-only heuristics.

        Haar cascades give eye boxes, then each eye ROI is inspected for a dark
        pupil-like blob. Gaze signals are generated later by the temporal
        tracker, so this method only reports per-frame measurements.
        """
        result = {
            "eye_count": 0,
            "left_eye": None,
            "right_eye": None,
            "eye_state": "UNKNOWN",  # OPEN, CLOSED, PARTIAL, UNKNOWN
            "blink_anomaly": False,
            "blink_rate": 0,
            "prolonged_eye_closure": False,
            "closure_duration": 0,
            "closure_duration_ms": 0,
            "eye_aspect_ratio": 0,
            "tracking_confidence": 0.0,
            "raw_eye_count": 0,
            "landmarks": [],
            "pupil_positions": [],
        }

        if not face_locations or len(face_locations) != 1 or not self.cv_ready():
            return result

        (x, y, w, h) = face_locations[0]
        frame_h, frame_w = gray.shape[:2]
        x1, y1 = max(0, x), max(0, y)
        x2, y2 = min(frame_w, x + w), min(frame_h, y + h)
        face_w, face_h = x2 - x1, y2 - y1
        if face_w <= 0 or face_h <= 0:
            return result

        upper_h = max(1, int(face_h * 0.58))
        upper_roi = gray[y1:y1 + upper_h, x1:x2]
        candidates = self._detect_eye_candidates(upper_roi, x1, y1, face_w, face_h)
        merged_candidates = self._merge_eye_candidates(candidates)
        selected_eyes = self._select_eye_pair(merged_candidates)
        result["raw_eye_count"] = len(merged_candidates)

        eye_details = [self._analyze_eye_roi(gray, box) for box in selected_eyes]
        eye_details = [detail for detail in eye_details if detail is not None]
        if not eye_details:
            return result

        eye_details.sort(key=lambda detail: detail["center"][0])
        result["eye_count"] = len(eye_details)
        result["landmarks"] = [detail["pupil_center"] for detail in eye_details]
        result["pupil_positions"] = [
            detail["normalized_pupil"] for detail in eye_details
            if detail.get("pupil_confidence", 0.0) >= 0.15
        ]

        if len(eye_details) >= 1:
            result["left_eye"] = eye_details[0]
        if len(eye_details) >= 2:
            result["right_eye"] = eye_details[1]

        aspect_values = [detail.get("aspect_ratio", 0.0) for detail in eye_details]
        result["eye_aspect_ratio"] = round(float(np.mean(aspect_values)), 3) if aspect_values else 0
        result["tracking_confidence"] = self._resolve_eye_tracking_confidence(eye_details)
        result["eye_state"] = self._resolve_eye_state(eye_details)

        return result

    def _detect_eye_candidates(
        self,
        upper_roi: np.ndarray,
        offset_x: int,
        offset_y: int,
        face_w: int,
        face_h: int,
    ) -> list[tuple[int, int, int, int]]:
        if upper_roi.size == 0 or self._eye_cascade is None or self._eye_cascade.empty():
            return []

        variants = [upper_roi]
        try:
            variants.append(cv2.equalizeHist(upper_roi))
        except Exception:
            pass
        try:
            variants.append(cv2.bilateralFilter(upper_roi, 5, 40, 40))
        except Exception:
            pass

        candidates: list[tuple[int, int, int, int]] = []
        for variant in variants:
            for ex, ey, ew, eh in self._detect_with_cascade(variant, self._eye_cascade):
                if ew <= 0 or eh <= 0:
                    continue
                rel_w = ew / max(face_w, 1)
                rel_h = eh / max(face_h, 1)
                aspect = eh / max(ew, 1)
                center_y = ey + eh / 2
                if rel_w < 0.08 or rel_w > 0.42:
                    continue
                if rel_h < 0.04 or rel_h > 0.28:
                    continue
                if aspect < 0.16 or aspect > 1.25:
                    continue
                if center_y < face_h * 0.10 or center_y > face_h * 0.62:
                    continue
                candidates.append((offset_x + int(ex), offset_y + int(ey), int(ew), int(eh)))
        return candidates

    def _merge_eye_candidates(self, eyes: list[tuple[int, int, int, int]]) -> list[tuple[int, int, int, int]]:
        merged: list[tuple[int, int, int, int]] = []
        for eye in sorted(eyes, key=lambda item: item[2] * item[3], reverse=True):
            if eye[2] <= 0 or eye[3] <= 0:
                continue
            match_index = next(
                (
                    index for index, existing in enumerate(merged)
                    if self._rect_iou(eye, existing) >= 0.35
                    or self._rect_center_close(eye, existing)
                ),
                None,
            )
            if match_index is None:
                merged.append(eye)
            else:
                merged[match_index] = self._union_rect(merged[match_index], eye)
        return merged

    def _select_eye_pair(self, candidates: list[tuple[int, int, int, int]]) -> list[tuple[int, int, int, int]]:
        if len(candidates) <= 2:
            return sorted(candidates, key=lambda item: item[0])

        best_pair: tuple[tuple[int, int, int, int], tuple[int, int, int, int]] | None = None
        best_score = float("-inf")
        for left, right in combinations(candidates, 2):
            lx, ly, lw, lh = left
            rx, ry, rw, rh = right
            lcx, lcy = lx + lw / 2, ly + lh / 2
            rcx, rcy = rx + rw / 2, ry + rh / 2
            horizontal_sep = abs(rcx - lcx)
            vertical_delta = abs(rcy - lcy)
            size_score = lw * lh + rw * rh
            score = size_score + horizontal_sep * 8 - vertical_delta * 12
            if score > best_score:
                best_score = score
                best_pair = (left, right)

        return sorted(list(best_pair), key=lambda item: item[0]) if best_pair else []

    def _analyze_eye_roi(self, gray: np.ndarray, eye_box: tuple[int, int, int, int]) -> dict[str, Any] | None:
        ex, ey, ew, eh = eye_box
        frame_h, frame_w = gray.shape[:2]
        x1, y1 = max(0, ex), max(0, ey)
        x2, y2 = min(frame_w, ex + ew), min(frame_h, ey + eh)
        if x2 <= x1 or y2 <= y1:
            return None

        roi = gray[y1:y2, x1:x2]
        if roi.size == 0:
            return None

        box_w, box_h = x2 - x1, y2 - y1
        box_area = max(box_w * box_h, 1)
        aspect_ratio = box_h / max(box_w, 1)
        center_x, center_y = box_w / 2, box_h / 2
        pupil_x, pupil_y = center_x, center_y
        pupil_confidence = 0.0

        try:
            enhanced = cv2.equalizeHist(roi)
            blurred = cv2.GaussianBlur(enhanced, (5, 5), 0)
            _, threshold = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            margin_x = max(1, int(box_w * 0.08))
            margin_y = max(1, int(box_h * 0.08))
            mask = np.zeros_like(threshold)
            mask[margin_y:box_h - margin_y or box_h, margin_x:box_w - margin_x or box_w] = 255
            threshold = cv2.bitwise_and(threshold, mask)
            kernel = np.ones((3, 3), np.uint8)
            threshold = cv2.morphologyEx(threshold, cv2.MORPH_OPEN, kernel)
            threshold = cv2.morphologyEx(threshold, cv2.MORPH_CLOSE, kernel)

            contours_raw = cv2.findContours(threshold, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            contours = contours_raw[0] if len(contours_raw) == 2 else contours_raw[1]
            best: tuple[float, float, float] | None = None
            for contour in contours:
                area = float(cv2.contourArea(contour))
                area_ratio = area / box_area
                if area_ratio < 0.008 or area_ratio > 0.42:
                    continue
                bx, by, bw, bh = cv2.boundingRect(contour)
                if bw <= 0 or bh <= 0:
                    continue
                shape_ratio = bw / max(bh, 1)
                if shape_ratio < 0.25 or shape_ratio > 5.0:
                    continue
                moments = cv2.moments(contour)
                if moments["m00"] == 0:
                    cx, cy = bx + bw / 2, by + bh / 2
                else:
                    cx, cy = moments["m10"] / moments["m00"], moments["m01"] / moments["m00"]
                center_penalty = abs(cx - center_x) / max(box_w, 1) + abs(cy - center_y) / max(box_h, 1)
                score = area_ratio * 3.0 + max(0.0, 1.0 - center_penalty)
                if best is None or score > best[0]:
                    best = (score, cx, cy)

            if best is not None:
                _, pupil_x, pupil_y = best
                contrast = float(np.std(roi)) / 64.0
                pupil_confidence = max(0.15, min(0.95, best[0] * 0.45 + min(0.35, contrast)))
            else:
                _, _, min_loc, _ = cv2.minMaxLoc(blurred)
                pupil_x, pupil_y = float(min_loc[0]), float(min_loc[1])
                contrast = float(np.std(roi)) / 80.0
                pupil_confidence = max(0.05, min(0.35, contrast))
        except Exception:
            pass

        norm_x = max(0.0, min(1.0, pupil_x / max(box_w, 1)))
        norm_y = max(0.0, min(1.0, pupil_y / max(box_h, 1)))
        openness_score = max(0.0, min(1.0, (aspect_ratio - 0.16) / 0.42))
        if openness_score < 0.18 and pupil_confidence < 0.35:
            eye_state = "CLOSED"
        elif openness_score < 0.35 or pupil_confidence < 0.22:
            eye_state = "PARTIAL"
        else:
            eye_state = "OPEN"

        return {
            "box": [int(x1), int(y1), int(box_w), int(box_h)],
            "center": [int(x1 + box_w / 2), int(y1 + box_h / 2)],
            "pupil_center": [int(x1 + pupil_x), int(y1 + pupil_y)],
            "normalized_pupil": {"x": round(norm_x, 3), "y": round(norm_y, 3)},
            "pupil_confidence": round(float(pupil_confidence), 3),
            "aspect_ratio": round(float(aspect_ratio), 3),
            "openness_score": round(float(openness_score), 3),
            "eye_state": eye_state,
        }

    def _resolve_eye_state(self, eye_details: list[dict[str, Any]]) -> str:
        if not eye_details:
            return "UNKNOWN"
        if len(eye_details) == 1:
            state = eye_details[0].get("eye_state", "UNKNOWN")
            return "PARTIAL" if state == "OPEN" else state

        openness = [float(detail.get("openness_score", 0.0)) for detail in eye_details]
        pupil_confidence = [float(detail.get("pupil_confidence", 0.0)) for detail in eye_details]
        average_open = float(np.mean(openness)) if openness else 0.0
        average_confidence = float(np.mean(pupil_confidence)) if pupil_confidence else 0.0
        if average_open < 0.18 and average_confidence < 0.35:
            return "CLOSED"
        if average_open < 0.35 or average_confidence < 0.25:
            return "PARTIAL"
        return "OPEN"

    def _resolve_eye_tracking_confidence(self, eye_details: list[dict[str, Any]]) -> float:
        if not eye_details:
            return 0.0
        pupil_confidence = [float(detail.get("pupil_confidence", 0.0)) for detail in eye_details]
        open_scores = [float(detail.get("openness_score", 0.0)) for detail in eye_details]
        eye_count_score = min(1.0, len(eye_details) / 2.0)
        score = 0.45 * float(np.mean(pupil_confidence)) + 0.35 * float(np.mean(open_scores)) + 0.20 * eye_count_score
        return round(max(0.0, min(1.0, score)), 3)

    # === Deep Learning-based Spoofing Detection ===
    def _detect_spoofing_deep(self, rgb_array: np.ndarray, gray: np.ndarray,
                              face_count: int, face_locations: list) -> dict:
        """Deep learning-based anti-spoofing detection."""
        result = {
            "detected": False,
            "type": None,
            "confidence": 0.0,
            "dl_enhanced": False,
            "method": "statistical"
        }

        if face_count == 0:
            return result

        try:
            # Method 1: DL-based spoofing detection (if model available)
            if self.spoofing_dl_ready() and face_locations:
                dl_result = self._dl_spoofing_detection(rgb_array, face_locations[0])
                if dl_result["detected"]:
                    return dl_result

            # Method 2: Enhanced statistical spoofing detection
            statistical_result = self._enhanced_statistical_spoofing(rgb_array, gray, face_locations)
            if statistical_result["detected"]:
                return statistical_result

            # Method 3: Texture-based spoofing detection
            texture_result = self._texture_based_spoofing(gray, face_locations)
            if texture_result["detected"]:
                return texture_result

            # Method 4: Color-based spoofing detection
            color_result = self._color_based_spoofing(rgb_array, face_locations)
            if color_result["detected"]:
                return color_result

        except Exception:
            pass

        return result

    def _dl_spoofing_detection(self, rgb_array: np.ndarray, face_loc: tuple) -> dict:
        """Use deep learning model for spoofing detection."""
        x, y, w, h = face_loc
        face_roi = rgb_array[y:y+h, x:x+w]

        if face_roi.size == 0:
            return {"detected": False, "type": None, "confidence": 0.0, "dl_enhanced": True}

        try:
            blob = cv2.dnn.blobFromImage(cv2.resize(face_roi, (64, 64)), 1.0, (64, 64), [104, 117, 123])
            self._spoofing_model.setInput(blob)
            predictions = self._spoofing_model.forward()

            # Assuming binary classification: [real, fake]
            fake_prob = float(predictions[0][1]) if predictions.shape[1] > 1 else 0.0

            if fake_prob > 0.7:
                return {
                    "detected": True,
                    "type": "DEEPFAKE",
                    "confidence": round(fake_prob, 3),
                    "dl_enhanced": True,
                    "method": "cnn"
                }
            elif fake_prob > 0.5:
                return {
                    "detected": True,
                    "type": "LIKELY_FAKE",
                    "confidence": round(fake_prob, 3),
                    "dl_enhanced": True,
                    "method": "cnn"
                }

        except Exception:
            pass

        return {"detected": False, "type": None, "confidence": 0.0, "dl_enhanced": True}

    def _enhanced_statistical_spoofing(self, rgb_array: np.ndarray, gray: np.ndarray,
                                       face_locations: list) -> dict:
        """Enhanced statistical spoofing detection."""
        laplacian = cv2.Laplacian(gray, cv2.CV_64F).var()

        # Very low Laplacian variance indicates flat image (printed photo)
        if laplacian < 40:
            return {
                "detected": True,
                "type": "PRINTED_PHOTO",
                "confidence": 0.88,
                "dl_enhanced": False,
                "method": "laplacian",
                "details": {"laplacian_variance": round(laplacian, 2)}
            }

        # FFT-based screen detection
        fft_result = self._fft_screen_detection(gray)
        if fft_result["detected"]:
            return fft_result

        return {"detected": False, "type": None, "confidence": 0.0, "dl_enhanced": False}

    def _fft_screen_detection(self, gray: np.ndarray) -> dict:
        """Detect screen replay using FFT analysis."""
        try:
            fft = np.fft.fft2(gray)
            fft_shift = np.fft.fftshift(fft)
            magnitude_spectrum = np.abs(fft_shift)

            h, w = gray.shape
            center_h, center_w = h // 2, w // 2

            # Check for periodic patterns
            r = min(h, w) // 4
            ring_values = []
            for angle in range(0, 360, 10):
                px = int(center_w + r * np.cos(np.radians(angle)))
                py = int(center_h + r * np.sin(np.radians(angle)))
                if 0 <= px < w and 0 <= py < h:
                    ring_values.append(magnitude_spectrum[py, px])

            if ring_values:
                ring_variance = np.var(ring_values)
                if ring_variance > 1e12:
                    return {
                        "detected": True,
                        "type": "SCREEN_REPLAY",
                        "confidence": 0.75,
                        "dl_enhanced": False,
                        "method": "fft",
                        "details": {"ring_variance": round(ring_variance, 2)}
                    }

        except Exception:
            pass

        return {"detected": False, "type": None, "confidence": 0.0, "dl_enhanced": False}

    def _texture_based_spoofing(self, gray: np.ndarray, face_locations: list) -> dict:
        """Texture-based spoofing detection using LBP-like analysis."""
        if not face_locations:
            return {"detected": False, "type": None, "confidence": 0.0}

        try:
            x, y, w, h = face_locations[0]
            face_roi = gray[y:y+h, x:x+w]

            if face_roi.size == 0:
                return {"detected": False, "type": None, "confidence": 0.0}

            # Sobel edge detection
            sobelx = cv2.Sobel(face_roi, cv2.CV_64F, 1, 0, ksize=3)
            sobely = cv2.Sobel(face_roi, cv2.CV_64F, 0, 1, ksize=3)
            sobel_magnitude = np.sqrt(sobelx**2 + sobely**2)

            edge_mean = np.mean(sobel_magnitude)
            edge_std = np.std(sobel_magnitude)

            # Real faces have more texture variation
            if edge_mean < 10 and edge_std < 5:
                return {
                    "detected": True,
                    "type": "FLAT_IMAGE",
                    "confidence": 0.72,
                    "dl_enhanced": False,
                    "method": "texture",
                    "details": {"edge_mean": round(edge_mean, 2), "edge_std": round(edge_std, 2)}
                }

        except Exception:
            pass

        return {"detected": False, "type": None, "confidence": 0.0}

    def _color_based_spoofing(self, rgb_array: np.ndarray, face_locations: list) -> dict:
        """Color-based spoofing detection."""
        if not face_locations:
            return {"detected": False, "type": None, "confidence": 0.0}

        try:
            x, y, w, h = face_locations[0]
            face_roi = rgb_array[y:y+h, x:x+w]

            if face_roi.size == 0:
                return {"detected": False, "type": None, "confidence": 0.0}

            b, g, r = cv2.split(face_roi)

            # Check color histogram distribution
            r_hist = cv2.calcHist([r], [0], None, [256], [0, 256])
            g_hist = cv2.calcHist([g], [0], None, [256], [0, 256])
            b_hist = cv2.calcHist([b], [0], None, [256], [0, 256])

            # Real faces typically have more natural color distribution
            r_entropy = -np.sum(r_hist / np.sum(r_hist) * np.log2(r_hist / np.sum(r_hist) + 1e-10))
            g_entropy = -np.sum(g_hist / np.sum(g_hist) * np.log2(g_hist / np.sum(g_hist) + 1e-10))
            b_entropy = -np.sum(b_hist / np.sum(b_hist) * np.log2(b_hist / np.sum(b_hist) + 1e-10))
            avg_entropy = (r_entropy + g_entropy + b_entropy) / 3

            # Low entropy indicates unnatural color distribution (screen/print)
            if avg_entropy < 4.5:
                return {
                    "detected": True,
                    "type": "SCREEN_DISPLAY",
                    "confidence": 0.68,
                    "dl_enhanced": False,
                    "method": "color",
                    "details": {"avg_entropy": round(avg_entropy, 2)}
                }

        except Exception:
            pass

        return {"detected": False, "type": None, "confidence": 0.0}

    # === Gaze Analysis ===
    def _analyze_gaze(self, eye_tracking: dict, face_locations: list, frame_shape: tuple) -> dict:
        """Analyze per-frame gaze direction from normalized pupil positions."""
        result = {
            "gaze_off_screen": False,
            "gaze_off_screen_candidate": False,
            "gaze_confidence": 0.0,
            "primary_direction": "CENTER",
            "off_duration": 0,
            "off_duration_ms": 0,
            "rapid_eye_movement": False,
            "movement_count": 0,
            "attention_score": 1.0,
            "offset": {"x": 0.0, "y": 0.0},
        }

        if not face_locations or len(face_locations) != 1 or eye_tracking.get("eye_count", 0) == 0:
            return result

        eye_samples = [
            eye for eye in (eye_tracking.get("left_eye"), eye_tracking.get("right_eye"))
            if isinstance(eye, dict) and isinstance(eye.get("normalized_pupil"), dict)
        ]
        eye_samples = [
            eye for eye in eye_samples
            if float(eye.get("pupil_confidence", 0.0)) >= 0.15
        ]
        if not eye_samples:
            return result

        norm_x_values = [float(eye["normalized_pupil"].get("x", 0.5)) for eye in eye_samples]
        norm_y_values = [float(eye["normalized_pupil"].get("y", 0.5)) for eye in eye_samples]
        confidences = [float(eye.get("pupil_confidence", 0.0)) for eye in eye_samples]
        avg_x = float(np.mean(norm_x_values))
        avg_y = float(np.mean(norm_y_values))
        offset_x = avg_x - 0.5
        offset_y = avg_y - 0.5
        sample_penalty = 0.15 if len(eye_samples) < 2 else 0.0
        tracking_confidence = float(eye_tracking.get("tracking_confidence", 0.0))
        gaze_confidence = max(
            0.0,
            min(0.98, 0.55 * float(np.mean(confidences)) + 0.45 * tracking_confidence - sample_penalty),
        )

        horizontal_threshold = 0.17
        vertical_threshold = 0.18
        direction = "CENTER"
        if abs(offset_x) >= horizontal_threshold and abs(offset_x) >= abs(offset_y) * 0.85:
            direction = "LEFT" if offset_x < 0 else "RIGHT"
        elif abs(offset_y) >= vertical_threshold:
            direction = "UP" if offset_y < 0 else "DOWN"

        attention_score = 1.0 - (abs(offset_x) * 1.35 + abs(offset_y) * 1.15)
        result.update({
            "gaze_confidence": round(float(gaze_confidence), 3),
            "primary_direction": direction,
            "gaze_off_screen_candidate": direction != "CENTER" and gaze_confidence >= 0.50,
            "attention_score": round(max(0.0, min(1.0, attention_score)), 2),
            "offset": {"x": round(offset_x, 3), "y": round(offset_y, 3)},
        })

        return result

    def _update_tracking_state(
        self,
        tracking_key: str | None,
        sample_time: float | None,
        face_count: int,
        eye_tracking: dict,
        gaze_analysis: dict,
    ) -> dict[str, dict[str, Any]]:
        if not tracking_key:
            return {"eye_tracking": {}, "gaze_analysis": {}}

        now_ms = self._to_millis(sample_time)
        self._ensure_tracking_state_defaults()
        self._prune_tracking_state(now_ms)
        state = self._tracking_state.get(tracking_key)
        if state is None:
            state = {
                "last_seen_ms": now_ms,
                "closed_since_ms": None,
                "off_screen_since_ms": None,
                "off_screen_direction": "CENTER",
                "closed_streak": 0,
                "off_screen_streak": 0,
                "gaze_history": [],
                "last_primary_direction": "CENTER",
                "last_eye_state": "UNKNOWN",
            }
            self._tracking_state[tracking_key] = state

        state["last_seen_ms"] = now_ms

        if face_count != 1:
            state["closed_since_ms"] = None
            state["off_screen_since_ms"] = None
            state["closed_streak"] = 0
            state["off_screen_streak"] = 0
            state["last_primary_direction"] = "CENTER"
            state["last_eye_state"] = "UNKNOWN"
            state["gaze_history"] = []
            return {
                "eye_tracking": {
                    "closure_duration": 0,
                    "closure_duration_ms": 0,
                    "prolonged_eye_closure": False,
                },
                "gaze_analysis": {
                    "gaze_off_screen": False,
                    "off_duration": 0,
                    "off_duration_ms": 0,
                    "rapid_eye_movement": False,
                    "movement_count": 0,
                },
            }

        eye_state = str(eye_tracking.get("eye_state", "UNKNOWN")).upper()
        is_closed = eye_state == "CLOSED"
        if is_closed:
            if state["closed_since_ms"] is None:
                state["closed_since_ms"] = now_ms
            state["closed_streak"] = int(state.get("closed_streak", 0)) + 1
        else:
            state["closed_since_ms"] = None
            state["closed_streak"] = 0

        closure_duration_ms = 0
        prolonged_eye_closure = False
        if state["closed_since_ms"] is not None:
            closure_duration_ms = max(0, now_ms - int(state["closed_since_ms"]))
            prolonged_eye_closure = (
                state["closed_streak"] >= self._eye_closure_required_frames
                or closure_duration_ms >= self._eye_closure_required_ms
            )

        current_direction = str(gaze_analysis.get("primary_direction", "CENTER")).upper()
        candidate_off_screen = bool(gaze_analysis.get("gaze_off_screen_candidate"))
        current_confidence = float(gaze_analysis.get("gaze_confidence", 0.0))
        if candidate_off_screen and current_confidence >= 0.5:
            if state["off_screen_since_ms"] is None or state["off_screen_direction"] != current_direction:
                state["off_screen_since_ms"] = now_ms
                state["off_screen_streak"] = 1
                state["off_screen_direction"] = current_direction
            else:
                state["off_screen_streak"] = int(state.get("off_screen_streak", 0)) + 1
        else:
            state["off_screen_since_ms"] = None
            state["off_screen_streak"] = 0
            if current_direction == "CENTER":
                state["off_screen_direction"] = "CENTER"

        off_duration_ms = 0
        gaze_off_screen = False
        if state["off_screen_since_ms"] is not None:
            off_duration_ms = max(0, now_ms - int(state["off_screen_since_ms"]))
            gaze_off_screen = (
                state["off_screen_streak"] >= self._gaze_required_frames
                or off_duration_ms >= self._gaze_required_ms
            )

        gaze_history = state.setdefault("gaze_history", [])
        gaze_history.append({
            "direction": current_direction,
            "confidence": round(current_confidence, 3),
            "ts": now_ms,
        })
        gaze_history[:] = [item for item in gaze_history if now_ms - int(item["ts"]) <= 6000]
        movement_count = 0
        for index in range(1, len(gaze_history)):
            prev_dir = str(gaze_history[index - 1].get("direction", "CENTER")).upper()
            curr_dir = str(gaze_history[index].get("direction", "CENTER")).upper()
            if prev_dir != "CENTER" and curr_dir != "CENTER" and prev_dir != curr_dir:
                movement_count += 1
        rapid_eye_movement = movement_count >= 3 and len(gaze_history) >= 4

        attention_score = float(gaze_analysis.get("attention_score", 1.0))
        if prolonged_eye_closure:
            attention_score = min(attention_score, 0.35)
        if gaze_off_screen:
            attention_score = min(attention_score, 0.45)

        state["last_primary_direction"] = current_direction
        state["last_eye_state"] = eye_state

        return {
            "eye_tracking": {
                "closure_duration": round(closure_duration_ms / 1000.0, 1),
                "closure_duration_ms": int(closure_duration_ms),
                "prolonged_eye_closure": prolonged_eye_closure,
                "eye_state": "CLOSED" if prolonged_eye_closure else eye_state,
            },
            "gaze_analysis": {
                "gaze_off_screen": gaze_off_screen,
                "off_duration": round(off_duration_ms / 1000.0, 1),
                "off_duration_ms": int(off_duration_ms),
                "rapid_eye_movement": rapid_eye_movement,
                "movement_count": movement_count,
                "attention_score": round(max(0.0, min(1.0, attention_score)), 2),
            },
        }

    def _ensure_tracking_state_defaults(self) -> None:
        if not hasattr(self, "_tracking_state"):
            self._tracking_state = {}
        self._tracking_state_ttl_seconds = getattr(self, "_tracking_state_ttl_seconds", 180)
        self._max_tracking_states = getattr(self, "_max_tracking_states", 256)
        self._gaze_required_frames = getattr(self, "_gaze_required_frames", 3)
        self._gaze_required_ms = getattr(self, "_gaze_required_ms", 2500)
        self._eye_closure_required_frames = getattr(self, "_eye_closure_required_frames", 2)
        self._eye_closure_required_ms = getattr(self, "_eye_closure_required_ms", 2000)

    def _prune_tracking_state(self, now_ms: int) -> None:
        if not self._tracking_state:
            return
        cutoff_ms = now_ms - int(self._tracking_state_ttl_seconds * 1000)
        stale_keys = [
            key for key, state in self._tracking_state.items()
            if int(state.get("last_seen_ms", 0)) < cutoff_ms
        ]
        for key in stale_keys:
            self._tracking_state.pop(key, None)

        if len(self._tracking_state) <= self._max_tracking_states:
            return

        overflow = len(self._tracking_state) - self._max_tracking_states
        ordered = sorted(
            self._tracking_state.items(),
            key=lambda item: int(item[1].get("last_seen_ms", 0)),
        )
        for key, _ in ordered[:overflow]:
            self._tracking_state.pop(key, None)

    def _resolve_tracking_key(self, metadata: dict[str, Any]) -> str | None:
        for key in ("attempt_id", "attemptId", "session_id", "sessionId", "student_id", "studentId"):
            value = metadata.get(key)
            if value not in (None, ""):
                return f"{key}:{value}"
        return None

    def _resolve_sample_time(self, metadata: dict[str, Any]) -> float | None:
        captured_at = metadata.get("captured_at") or metadata.get("capturedAt")
        if not captured_at:
            return time.time()
        if isinstance(captured_at, (int, float)):
            return float(captured_at) / 1000.0 if captured_at > 10_000_000_000 else float(captured_at)
        try:
            normalized = str(captured_at).replace("Z", "+00:00")
            return datetime.fromisoformat(normalized).timestamp()
        except Exception:
            return time.time()

    def _resolve_camera_state(self, metadata: dict[str, Any]) -> dict[str, Any]:
        camera_on = self._coerce_bool(self._metadata_value(metadata, "cameraOn", "camera_on"))
        track_enabled = self._coerce_bool(self._metadata_value(metadata, "trackEnabled", "track_enabled"))
        track_ready_state = self._normalize_text(self._metadata_value(metadata, "trackReadyState", "track_ready_state"))
        state = {
            "camera_on": camera_on,
            "track_enabled": track_enabled,
            "track_ready_state": track_ready_state,
            "camera_off": False,
            "reason": None,
        }
        ready_state = track_ready_state.upper() if track_ready_state else ""
        if camera_on is False:
            state["camera_off"] = True
            state["reason"] = "cameraOn=false"
        elif track_enabled is False:
            state["camera_off"] = True
            state["reason"] = "trackEnabled=false"
        elif ready_state in {"ENDED", "INACTIVE"}:
            state["camera_off"] = True
            state["reason"] = f"trackReadyState={ready_state.lower()}"
        return state

    def _build_camera_off_response(self, metadata: dict[str, Any], camera_state: dict[str, Any]) -> dict[str, Any]:
        signals = [self._signal(
            "NO_CAMERA",
            "HIGH",
            0.99,
            {
                "derivedFromMetadata": True,
                "cameraState": camera_state,
                "reason": camera_state.get("reason") or "Camera is disabled",
                "recommendation": "Turn the camera back on to continue AI monitoring",
            },
        )]
        return {
            "status": "NO_CAMERA",
            "face_count": 0,
            "eye_count": 0,
            "face_detected": False,
            "multiple_faces": False,
            "face_quality": "NO_CAMERA",
            "frame_quality": "NO_CAMERA",
            "average_brightness": 0.0,
            "signals": signals,
            "warnings": self._generate_warnings(signals),
            "eye_state": "UNKNOWN",
            "eye_aspect_ratio": 0.0,
            "blink_rate": 0,
            "eye_tracking_confidence": 0.0,
            "closure_duration_ms": 0,
            "gaze_direction": "UNKNOWN",
            "gaze_off_screen": False,
            "gaze_confidence": 0.0,
            "off_screen_duration_ms": 0,
            "attention_score": 0.0,
            "visual_overlay": {
                "status": "NO_CAMERA",
                "label": "Camera tắt",
                "tone": "muted",
                "imageWidth": 0,
                "imageHeight": 0,
                "faceBoxes": [],
                "eyeBoxes": [],
                "pupilPoints": [],
            },
            "diagnostics": {
                "cv_ready": self.cv_ready(),
                "dnn_ready": self.dnn_ready(),
                "eye_landmark_ready": self.eye_landmark_ready(),
                "spoofing_dl_ready": self.spoofing_dl_ready(),
                "detection_method": "NO_CAMERA",
                "face_detector": self.face_detector_status(),
                "image_width": 0,
                "image_height": 0,
                "face_locations": [],
                "spoofing_check": {
                    "detected": False,
                    "type": None,
                    "confidence": 0.0,
                    "dl_enhanced": False,
                    "method": "camera_off",
                },
                "occlusion_check": {
                    "mask_detected": False,
                    "sunglasses_detected": False,
                    "partial_face": False,
                    "coverage_percent": 0,
                },
                "position_analysis": {
                    "face_turned": False,
                    "face_not_centered": False,
                    "turn_angle": 0,
                    "center_offset": {"x": 0, "y": 0},
                },
                "eye_tracking": {"eye_count": 0},
                "gaze_analysis": {
                    "gaze_off_screen": False,
                    "rapid_eye_movement": False,
                    "primary_direction": "UNKNOWN",
                    "attention_score": 0.0,
                    "gaze_confidence": 0.0,
                },
                "camera_state": camera_state,
                "metadata": metadata or {},
            },
        }

    def _metadata_value(self, metadata: dict[str, Any], *keys: str) -> Any:
        for key in keys:
            if key in metadata and metadata.get(key) not in (None, ""):
                return metadata.get(key)
        return None

    def _normalize_text(self, value: Any) -> str | None:
        if value is None:
            return None
        text = str(value).strip()
        return text or None

    def _coerce_bool(self, value: Any) -> bool | None:
        if isinstance(value, bool):
            return value
        if value is None:
            return None
        text = str(value).strip().lower()
        if not text:
            return None
        if text in {"true", "1", "yes", "y", "on"}:
            return True
        if text in {"false", "0", "no", "n", "off"}:
            return False
        return None

    def _to_millis(self, sample_time: float | None) -> int:
        if sample_time is None:
            return int(time.time() * 1000)
        return int(sample_time * 1000)

    # === Face Detection Methods ===
    def _detect_face_locations(self, rgb_array: np.ndarray, gray: np.ndarray) -> tuple[int, list[tuple[int, int, int, int]], str]:
        if self.dnn_ready():
            face_count, face_locations, used_haar_fallback = self._detect_faces_dnn(rgb_array, gray)
            if face_count > 0:
                if used_haar_fallback:
                    return face_count, face_locations, "HAAR_FALLBACK"
                return face_count, face_locations, self._dnn_model_type or "DNN"
            if self.cv_ready():
                haar_count, haar_locations = self._detect_faces_haar(gray)
                if haar_count > 0:
                    return haar_count, haar_locations, f"{self._dnn_model_type or 'DNN'}+HAAR"
            return face_count, face_locations, self._dnn_model_type or "DNN"

        if self.cv_ready():
            face_count, face_locations = self._detect_faces_haar(gray)
            return face_count, face_locations, "HAAR"

        return 0, [], "UNAVAILABLE"

    def _detect_faces_dnn(self, rgb_array: np.ndarray, gray: np.ndarray) -> tuple[int, list[tuple[int, int, int, int]], bool]:
        """Detect faces using DNN."""
        if not self.dnn_ready():
            face_count, face_locations = self._detect_faces_haar(gray)
            return face_count, face_locations, True

        try:
            h, w = gray.shape
            bgr_array = cv2.cvtColor(rgb_array, cv2.COLOR_RGB2BGR)
            blob = cv2.dnn.blobFromImage(
                cv2.resize(bgr_array, (300, 300)), 1.0, (300, 300), (104.0, 177.0, 123.0), swapRB=False, crop=False)

            self._dnn_net.setInput(blob)
            detections = self._dnn_net.forward()

            faces = []
            for i in range(detections.shape[2]):
                confidence = detections[0, 0, i, 2]
                if confidence > self._dnn_confidence_threshold:
                    x1 = int(detections[0, 0, i, 3] * w)
                    y1 = int(detections[0, 0, i, 4] * h)
                    x2 = int(detections[0, 0, i, 5] * w)
                    y2 = int(detections[0, 0, i, 6] * h)
                    x1, y1 = max(0, x1), max(0, y1)
                    x2, y2 = min(w, x2), min(h, y2)
                    if x2 > x1 and y2 > y1:
                        faces.append((x1, y1, x2 - x1, y2 - y1))

            merged_faces = self._merge_face_locations([tuple(f) for f in faces])
            return len(merged_faces), merged_faces, False

        except Exception:
            logger.warning("DNN face detection failed, falling back to Haar", exc_info=True)
            face_count, face_locations = self._detect_faces_haar(gray)
            return face_count, face_locations, True

    def _detect_faces_haar(self, gray_image: np.ndarray) -> tuple:
        """Detect faces using Haar cascades."""
        if not self.cv_ready():
            return 0, []

        candidates: list[tuple[int, int, int, int]] = []
        variants = [gray_image]
        try:
            equalized = cv2.equalizeHist(gray_image)
            variants.append(equalized)
        except Exception:
            pass

        for variant in variants:
            candidates.extend(self._detect_with_cascade(variant, self._face_cascade))

        # Frontal Haar misses many real webcam frames when the student turns
        # slightly. Profile detection is advisory but useful as a fallback.
        if self._profile_cascade is not None and not self._profile_cascade.empty():
            for variant in variants:
                candidates.extend(self._detect_with_cascade(variant, self._profile_cascade))
                try:
                    flipped = cv2.flip(variant, 1)
                    frame_width = gray_image.shape[1]
                    for (x, y, w, h) in self._detect_with_cascade(flipped, self._profile_cascade):
                        candidates.append((max(0, frame_width - x - w), y, w, h))
                except Exception:
                    pass

        faces = self._merge_face_locations(candidates)
        return len(faces), faces

    def _detect_with_cascade(self, gray_image: np.ndarray, cascade) -> list[tuple[int, int, int, int]]:
        if cascade is None or cascade.empty():
            return []
        try:
            detections = cascade.detectMultiScale(
                gray_image,
                scaleFactor=1.08,
                minNeighbors=4,
                minSize=(36, 36),
            )
            return [tuple(int(v) for v in face) for face in detections]
        except Exception:
            return []

    def _merge_face_locations(self, faces: list[tuple[int, int, int, int]]) -> list[tuple[int, int, int, int]]:
        merged: list[tuple[int, int, int, int]] = []
        for face in sorted(faces, key=lambda f: f[2] * f[3], reverse=True):
            if face[2] <= 0 or face[3] <= 0:
                continue
            match_index = next(
                (
                    index for index, existing in enumerate(merged)
                    if self._rect_iou(face, existing) >= 0.28
                    or self._rect_center_close(face, existing)
                ),
                None,
            )
            if match_index is None:
                merged.append(face)
            else:
                merged[match_index] = self._union_rect(merged[match_index], face)
        return merged

    def _rect_iou(self, a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> float:
        ax1, ay1, aw, ah = a
        bx1, by1, bw, bh = b
        ax2, ay2 = ax1 + aw, ay1 + ah
        bx2, by2 = bx1 + bw, by1 + bh
        ix1, iy1 = max(ax1, bx1), max(ay1, by1)
        ix2, iy2 = min(ax2, bx2), min(ay2, by2)
        intersection = max(0, ix2 - ix1) * max(0, iy2 - iy1)
        if intersection == 0:
            return 0.0
        union = aw * ah + bw * bh - intersection
        return intersection / union if union > 0 else 0.0

    def _rect_center_close(self, a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> bool:
        ax, ay, aw, ah = a
        bx, by, bw, bh = b
        acx, acy = ax + aw / 2, ay + ah / 2
        bcx, bcy = bx + bw / 2, by + bh / 2
        return abs(acx - bcx) <= max(aw, bw) * 0.25 and abs(acy - bcy) <= max(ah, bh) * 0.25

    def _union_rect(self, a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
        ax1, ay1, aw, ah = a
        bx1, by1, bw, bh = b
        x1, y1 = min(ax1, bx1), min(ay1, by1)
        x2, y2 = max(ax1 + aw, bx1 + bw), max(ay1 + ah, by1 + bh)
        return x1, y1, x2 - x1, y2 - y1

    # === Occlusion Detection ===
    def _detect_occlusion(self, gray: np.ndarray, face_locations: list, eye_tracking: dict) -> dict:
        """Detect face occlusions."""
        result = {
            "mask_detected": False,
            "sunglasses_detected": False,
            "partial_face": False,
            "coverage_percent": 100
        }

        if not face_locations or len(face_locations) != 1:
            return result

        (x, y, w, h) = face_locations[0]
        eye_count = eye_tracking.get("eye_count", 0)

        # Check upper region for sunglasses
        upper_face = gray[y:y + int(h * 0.45), x:x + w]
        if upper_face.size > 0:
            upper_mean = np.mean(upper_face)
            upper_var = np.var(upper_face)
            if upper_mean < 50 and upper_var < 200 and eye_count == 0 and w > 100:
                result["sunglasses_detected"] = True
                result["coverage_percent"] -= 20

        # Check for mask
        lower_face = gray[y + int(h * 0.75):y + h, x:x + w]
        middle_face = gray[y + int(h * 0.45):y + int(h * 0.75), x:x + w]
        if lower_face.size > 0 and middle_face.size > 0:
            lower_mean = np.mean(lower_face)
            lower_var = np.var(lower_face)
            middle_mean = np.mean(middle_face)
            if lower_var < 150 and abs(lower_mean - middle_mean) < 30:
                if self._smile_cascade:
                    mouth_region = gray[y + int(h * 0.6):y + h, x:x + w]
                    if mouth_region.size > 0:
                        smiles = self._smile_cascade.detectMultiScale(mouth_region, 1.7, 20)
                        if len(smiles) == 0:
                            result["mask_detected"] = True
                            result["coverage_percent"] -= 30

        # Check partial face
        face_area = w * h
        frame_area = gray.shape[0] * gray.shape[1]
        if face_area / frame_area < 0.08:
            result["partial_face"] = True

        margin_x = min(x + w // 2, gray.shape[1] - x - w // 2)
        margin_y = min(y + h // 2, gray.shape[0] - y - h // 2)
        if margin_x < gray.shape[1] * 0.15 or margin_y < gray.shape[0] * 0.1:
            result["partial_face"] = True
            result["coverage_percent"] -= 15

        return result

    # === Position Analysis ===
    def _analyze_face_position(self, face_locations: list, frame_shape: tuple) -> dict:
        """Analyze face position."""
        result = {
            "face_turned": False,
            "face_not_centered": False,
            "turn_angle": 0,
            "center_offset": {}
        }

        if not face_locations or len(face_locations) != 1:
            return result

        (x, y, w, h) = face_locations[0]
        frame_h, frame_w = frame_shape[:2]

        face_center_x, face_center_y = x + w // 2, y + h // 2
        frame_center_x, frame_center_y = frame_w // 2, frame_h // 2

        offset_x, offset_y = face_center_x - frame_center_x, face_center_y - frame_center_y
        result["center_offset"] = {
            "x": offset_x, "y": offset_y,
            "x_percent": round((offset_x / frame_w) * 100, 1),
            "y_percent": round((offset_y / frame_h) * 100, 1)
        }

        if abs(offset_x) > frame_w * 0.15 or abs(offset_y) > frame_h * 0.15:
            result["face_not_centered"] = True

        aspect_ratio = w / h if h > 0 else 1.0
        if aspect_ratio > 0.9 or aspect_ratio < 0.6:
            result["face_turned"] = True
            result["turn_angle"] = 45 if aspect_ratio > 0.9 else -45

        return result

    # === Quality Assessment ===
    def _assess_face_quality(self, face_count: int, eye_count: int, brightness: float, variance: float) -> str:
        if face_count == 0: return "NO_FACE"
        if face_count > 1: return "MULTIPLE_FACES"
        if eye_count == 0: return "EYES_NOT_VISIBLE"
        if brightness < 50: return "TOO_DARK"
        if brightness > 240: return "TOO_BRIGHT"
        if variance < 80: return "BLURRY"
        return "GOOD"

    def _assess_frame_quality(self, brightness: float, variance: float) -> str:
        if brightness < 40 or variance < 50: return "POOR"
        if brightness < 60 or variance < 80: return "FAIR"
        if brightness > 240: return "OVEREXPOSED"
        return "GOOD"

    def _generate_warnings(self, signals: list) -> list:
        warnings = []
        critical = [s for s in signals if s["severity"] == "CRITICAL"]
        if critical:
            warnings.append(f"CRITICAL: {len(critical)} critical issue(s) detected")
        high = [s for s in signals if s["severity"] == "HIGH"]
        if high:
            warnings.append(f"HIGH: {len(high)} high-priority issue(s) need attention")
        return warnings

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

    def analyze_behavior(
        self,
        paste_length: int = 0,
        tab_switch_count: int = 0,
        idle_seconds: int = 0,
        typing_intervals: list[int] | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        intervals = [v for v in (typing_intervals or []) if v is not None and v >= 0]
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

        return {"status": "DONE", "signals": signals, "diagnostics": diagnostics}
