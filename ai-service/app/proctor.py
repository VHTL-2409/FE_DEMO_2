from __future__ import annotations

import base64
import io
import os
from typing import Any

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
        self._dnn_confidence_threshold = 0.5
        self._eye_landmarks = None
        self._face_landmark_detector = None
        self._spoofing_model = None
        self._spoofing_dl = False

        if cv2 is not None:
            self._init_haar_cascades()
            self._init_dnn_face_detection()
            self._init_eye_landmark_detector()
            self._init_spoofing_detector()

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
            return

        try:
            base_dir = os.path.dirname(__file__)
            prototxt_path = os.path.join(base_dir, "models", "deploy.prototxt.txt")
            model_path = os.path.join(base_dir, "models", "res10_300x300_ssd_iter_140000.caffemodel")

            if os.path.exists(prototxt_path) and os.path.exists(model_path):
                self._dnn_net = cv2.dnn.readNetFromCaffe(prototxt_path, model_path)
                self._dnn_model_type = "CAFFE"
                return

            dnn_proto = "opencv_face_detector.pbtxt"
            dnn_model = "opencv_face_detector_uint8.pb"
            if os.path.exists(dnn_proto) and os.path.exists(dnn_model):
                self._dnn_net = cv2.dnn.readNetFromTensorflow(dnn_model, dnn_proto)
                self._dnn_model_type = "TENSORFLOW"
                return
        except Exception:
            pass

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

    def eye_landmark_ready(self) -> bool:
        return self._eye_landmarks and self._face_landmark_detector is not None

    def spoofing_dl_ready(self) -> bool:
        return self._spoofing_dl and self._spoofing_model is not None

    def analyze_frame(self, image_base64: str, metadata: dict[str, Any] | None = None) -> dict[str, Any]:
        """Comprehensive frame analysis including face detection, eye tracking, and spoofing detection."""
        image = self._decode_image(image_base64)
        rgb_array = np.array(image)
        gray = np.array(image.convert("L"))

        # Face detection
        if self.dnn_ready():
            face_count, face_locations = self._detect_faces_dnn(rgb_array, gray)
        else:
            face_count, face_locations = self._detect_faces_haar(gray)

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

        signals = []

        # === FACE DETECTION SIGNALS ===
        if (self.dnn_ready() or self.cv_ready()) and face_count == 0:
            signals.append(self._signal(
                "FACE_NOT_DETECTED", "HIGH", 0.92,
                {"faceCount": 0, "reason": "No face found in frame",
                 "recommendation": "Ensure face is clearly visible and well-lit",
                 "detectionMethod": "DNN" if self.dnn_ready() else "HAAR"}
            ))

        elif (self.dnn_ready() or self.cv_ready()) and face_count > 1:
            signals.append(self._signal(
                "MULTIPLE_FACES", "CRITICAL", 0.98,
                {"faceCount": face_count, "reason": f"{face_count} faces detected",
                 "recommendation": "Only one person should be in frame",
                 "detectionMethod": "DNN" if self.dnn_ready() else "HAAR"}
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
                 "reason": "Eyes closed for extended period",
                 "recommendation": "Keep eyes open during exam"}))

        # === GAZE TRACKING SIGNALS ===
        if gaze_analysis["gaze_off_screen"]:
            signals.append(self._signal("GAZE_OFF_SCREEN", "HIGH", gaze_analysis["gaze_confidence"],
                {"gazeDirection": gaze_analysis.get("primary_direction", "UNKNOWN"),
                 "duration": gaze_analysis.get("off_duration", 0),
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
            # Gaze analysis fields
            "gaze_direction": gaze_analysis.get("primary_direction", "CENTER"),
            "gaze_off_screen": gaze_analysis.get("gaze_off_screen", False),
            "attention_score": gaze_analysis.get("attention_score", 1.0),
            "diagnostics": {
                "cv_ready": self.cv_ready(),
                "dnn_ready": self.dnn_ready(),
                "eye_landmark_ready": self.eye_landmark_ready(),
                "spoofing_dl_ready": self.spoofing_dl_ready(),
                "detection_method": "DNN" if self.dnn_ready() else "HAAR",
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

    # === Advanced Eye Tracking ===
    def _track_eyes(self, rgb_array: np.ndarray, gray: np.ndarray, face_locations: list) -> dict:
        """Advanced eye tracking with blink detection and eye state analysis."""
        result = {
            "eye_count": 0,
            "left_eye": None,
            "right_eye": None,
            "eye_state": "OPEN",  # OPEN, CLOSED, PARTIAL
            "blink_anomaly": False,
            "blink_rate": 0,
            "prolonged_eye_closure": False,
            "closure_duration": 0,
            "eye_aspect_ratio": 0,
            "landmarks": []
        }

        if not face_locations or len(face_locations) != 1 or not self.cv_ready():
            return result

        (x, y, w, h) = face_locations[0]

        # Detect eyes in upper half of face
        roi = gray[y:y + int(h * 0.5), x:x + w]
        eyes = self._eye_cascade.detectMultiScale(roi, 1.1, 5) if self._eye_cascade else []

        result["eye_count"] = len(eyes)

        if len(eyes) >= 2:
            # Calculate eye aspect ratio (EAR) for blink detection
            eye_points = []
            for (ex, ey, ew, eh) in eyes[:2]:
                eye_points.append((ex + ew//2, ey + eh//2))
                eye_points.append((ex, ey))
                eye_points.append((ex + ew, ey))
                eye_points.append((ex, ey + eh))
                eye_points.append((ex + ew, ey + eh))

            if len(eye_points) >= 10:
                # Simple EAR approximation
                vertical_1 = np.linalg.norm(np.array(eye_points[1]) - np.array(eye_points[5]))
                vertical_2 = np.linalg.norm(np.array(eye_points[3]) - np.array(eye_points[7]))
                horizontal = np.linalg.norm(np.array(eye_points[0]) - np.array(eye_points[6]))

                ear = (vertical_1 + vertical_2) / (2.0 * horizontal + 1e-6)
                result["eye_aspect_ratio"] = round(ear, 3)

                # Detect blink (EAR < 0.2 typically indicates closed eyes)
                if ear < 0.2:
                    result["eye_state"] = "CLOSED"
                    result["closure_duration"] = 1  # Would need temporal tracking
                elif ear < 0.3:
                    result["eye_state"] = "PARTIAL"
                else:
                    result["eye_state"] = "OPEN"

        # Store landmark positions if available
        if len(eyes) > 0:
            result["landmarks"] = [(int(x + e[0] + e[2]//2), int(y + e[1] + e[3]//2)) for e in eyes]

        return result

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
        """Analyze gaze direction and attention patterns."""
        result = {
            "gaze_off_screen": False,
            "gaze_confidence": 0.0,
            "primary_direction": "CENTER",
            "off_duration": 0,
            "rapid_eye_movement": False,
            "movement_count": 0,
            "attention_score": 1.0
        }

        if not face_locations or eye_tracking.get("eye_count", 0) == 0:
            return result

        # Calculate gaze direction based on eye positions
        landmarks = eye_tracking.get("landmarks", [])
        if len(landmarks) < 2:
            return result

        (x, y, w, h) = face_locations[0]
        frame_h, frame_w = frame_shape[:2]
        face_center_x = x + w // 2
        face_center_y = y + h // 2

        # Estimate gaze based on eye positions relative to face center
        left_eye = landmarks[0] if len(landmarks) > 0 else None
        right_eye = landmarks[1] if len(landmarks) > 1 else None

        if left_eye and right_eye:
            eye_mid_x = (left_eye[0] + right_eye[0]) / 2
            eye_mid_y = (left_eye[1] + right_eye[1]) / 2

            # Calculate offset from center
            offset_x = (eye_mid_x - face_center_x) / (w / 2 + 1e-6)
            offset_y = (eye_mid_y - face_center_y) / (h / 2 + 1e-6)

            # Determine gaze direction
            threshold = 0.3  # 30% offset threshold
            if abs(offset_x) > threshold:
                result["primary_direction"] = "LEFT" if offset_x < 0 else "RIGHT"
                result["gaze_off_screen"] = True
            elif abs(offset_y) > threshold:
                result["primary_direction"] = "UP" if offset_y < 0 else "DOWN"
                result["gaze_off_screen"] = True

            # Calculate confidence based on eye visibility
            eye_ear = eye_tracking.get("eye_aspect_ratio", 0)
            if eye_ear > 0.2:
                result["gaze_confidence"] = min(0.95, 0.5 + eye_ear * 2)
            else:
                result["gaze_confidence"] = 0.3  # Low confidence when eyes partially closed

            # Calculate attention score
            result["attention_score"] = round(1.0 - abs(offset_x) * 0.5 - abs(offset_y) * 0.5, 2)
            result["attention_score"] = max(0.0, min(1.0, result["attention_score"]))

        return result

    # === Face Detection Methods ===
    def _detect_faces_dnn(self, rgb_array: np.ndarray, gray: np.ndarray) -> tuple:
        """Detect faces using DNN."""
        if not self.dnn_ready():
            return self._detect_faces_haar(gray)

        try:
            h, w = gray.shape
            blob = cv2.dnn.blobFromImage(
                cv2.resize(rgb_array, (300, 300)), 1.0, (300, 300), (104.0, 177.0, 123.0))

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
                    faces.append((x1, y1, x2 - x1, y2 - y1))

            return len(faces), [tuple(f) for f in faces]

        except Exception:
            return self._detect_faces_haar(gray)

    def _detect_faces_haar(self, gray_image: np.ndarray) -> tuple:
        """Detect faces using Haar cascades."""
        if not self.cv_ready():
            return 0, []

        faces = self._face_cascade.detectMultiScale(
            gray_image, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))
        return len(faces), [tuple(f) for f in faces]

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
