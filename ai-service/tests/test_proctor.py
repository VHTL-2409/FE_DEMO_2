import base64
import io
import unittest
from unittest.mock import patch

from PIL import Image
import numpy as np

from app.proctor import ProctorAnalyzer
from app.schemas import BehaviorAnalysisRequest, FrameAnalysisRequest


def image_to_base64(color=(0, 0, 0)):
    image = Image.new("RGB", (160, 120), color=color)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG")
    return base64.b64encode(buffer.getvalue()).decode("ascii")


class NoFaceAnalyzer(ProctorAnalyzer):
    def cv_ready(self):
        return True

    def dnn_ready(self):
        return False

    def _detect_faces_haar(self, gray_image):
        return 0, []


class OneFaceAnalyzer(ProctorAnalyzer):
    def cv_ready(self):
        return True

    def dnn_ready(self):
        return False

    def _detect_faces_haar(self, gray_image):
        return 1, [(40, 20, 70, 70)]

    def _track_eyes(self, rgb_array, gray, face_locations):
        return {
            "eye_count": 1,
            "left_eye": None,
            "right_eye": None,
            "eye_state": "OPEN",
            "blink_anomaly": False,
            "blink_rate": 0,
            "prolonged_eye_closure": False,
            "closure_duration": 0,
            "eye_aspect_ratio": 0.25,
            "landmarks": [(58, 48)],
        }

    def _detect_spoofing_deep(self, rgb_array, gray, face_count, face_locations):
        return {
            "detected": False,
            "type": None,
            "confidence": 0.0,
            "dl_enhanced": False,
            "method": "test",
        }


class FixedEyeAnalyzer(ProctorAnalyzer):
    def __init__(
        self,
        left_normalized=(0.5, 0.5),
        right_normalized=(0.5, 0.5),
        eye_state="OPEN",
        tracking_confidence=0.9,
    ):
        super().__init__()
        self.left_normalized = left_normalized
        self.right_normalized = right_normalized
        self.eye_state = eye_state
        self.tracking_confidence = tracking_confidence

    def cv_ready(self):
        return True

    def dnn_ready(self):
        return False

    def _detect_faces_haar(self, gray_image):
        return 1, [(40, 20, 70, 70)]

    def _track_eyes(self, rgb_array, gray, face_locations):
        eye_confidence = self.tracking_confidence
        openness = 0.65 if self.eye_state != "CLOSED" else 0.08
        detail_left = {
            "box": [42, 32, 14, 8],
            "center": [49, 36],
            "pupil_center": [46, 36],
            "normalized_pupil": {"x": self.left_normalized[0], "y": self.left_normalized[1]},
            "pupil_confidence": eye_confidence,
            "aspect_ratio": 0.42,
            "openness_score": openness,
            "eye_state": self.eye_state,
        }
        detail_right = {
            "box": [66, 32, 14, 8],
            "center": [73, 36],
            "pupil_center": [70, 36],
            "normalized_pupil": {"x": self.right_normalized[0], "y": self.right_normalized[1]},
            "pupil_confidence": eye_confidence,
            "aspect_ratio": 0.42,
            "openness_score": openness,
            "eye_state": self.eye_state,
        }
        return {
            "eye_count": 2,
            "left_eye": detail_left,
            "right_eye": detail_right,
            "eye_state": self.eye_state,
            "blink_anomaly": False,
            "blink_rate": 0,
            "prolonged_eye_closure": False,
            "closure_duration": 0,
            "closure_duration_ms": 0,
            "eye_aspect_ratio": 0.42,
            "tracking_confidence": eye_confidence,
            "raw_eye_count": 2,
            "landmarks": [detail_left["pupil_center"], detail_right["pupil_center"]],
            "pupil_positions": [detail_left["normalized_pupil"], detail_right["normalized_pupil"]],
        }

    def _detect_spoofing_deep(self, rgb_array, gray, face_count, face_locations):
        return {
            "detected": False,
            "type": None,
            "confidence": 0.0,
            "dl_enhanced": False,
            "method": "test",
        }


class ProctorAnalyzerTests(unittest.TestCase):
    def test_black_frame_returns_no_face_signal(self):
        result = NoFaceAnalyzer().analyze_frame(image_to_base64((0, 0, 0)))

        self.assertEqual(result["status"], "DONE")
        self.assertEqual(result["face_count"], 0)
        self.assertTrue(
            any(signal["signal_type"] == "FACE_NOT_DETECTED" for signal in result["signals"])
        )

    def test_one_face_path_does_not_crash_with_eye_tracking_dict(self):
        result = OneFaceAnalyzer().analyze_frame(image_to_base64((128, 128, 128)))

        self.assertEqual(result["status"], "DONE")
        self.assertEqual(result["face_count"], 1)
        self.assertIn("signals", result)
        self.assertIn("gaze_direction", result)

    def test_visual_overlay_contains_face_eye_and_pupil_boxes(self):
        result = FixedEyeAnalyzer().analyze_frame(image_to_base64((128, 128, 128)))

        overlay = result["visual_overlay"]
        self.assertEqual(overlay["imageWidth"], 160)
        self.assertEqual(overlay["imageHeight"], 120)
        self.assertEqual(overlay["status"], "TRACKING")
        self.assertEqual(overlay["label"], "Đang theo dõi")
        self.assertEqual(overlay["tone"], "success")
        self.assertEqual(len(overlay["faceBoxes"]), 1)
        self.assertEqual(len(overlay["eyeBoxes"]), 2)
        self.assertEqual(len(overlay["pupilPoints"]), 2)

    def test_gaze_off_screen_requires_multiple_samples(self):
        analyzer = FixedEyeAnalyzer(left_normalized=(0.20, 0.50), right_normalized=(0.18, 0.51))
        frame = image_to_base64((128, 128, 128))

        analyzer.analyze_frame(frame, {"attempt_id": 777, "captured_at": "2026-05-07T00:00:00Z"})
        analyzer.analyze_frame(frame, {"attempt_id": 777, "captured_at": "2026-05-07T00:00:01Z"})
        result = analyzer.analyze_frame(frame, {"attempt_id": 777, "captured_at": "2026-05-07T00:00:02Z"})

        self.assertEqual(result["gaze_direction"], "LEFT")
        self.assertTrue(result["gaze_off_screen"])
        self.assertGreaterEqual(result["off_screen_duration_ms"], 2000)
        self.assertTrue(any(signal["signal_type"] == "GAZE_OFF_SCREEN" for signal in result["signals"]))
        self.assertEqual(result["visual_overlay"]["status"], "GAZE_AWAY")
        self.assertEqual(result["visual_overlay"]["tone"], "danger")

    def test_prolonged_eye_closure_requires_state(self):
        analyzer = FixedEyeAnalyzer(eye_state="CLOSED", tracking_confidence=0.7)
        frame = image_to_base64((128, 128, 128))

        analyzer.analyze_frame(frame, {"attempt_id": 888, "captured_at": "2026-05-07T00:00:00Z"})
        result = analyzer.analyze_frame(frame, {"attempt_id": 888, "captured_at": "2026-05-07T00:00:01Z"})

        self.assertEqual(result["eye_state"], "CLOSED")
        self.assertTrue(result["eye_count"] >= 1)
        self.assertTrue(any(signal["signal_type"] == "EYES_CLOSED_PROLONGED" for signal in result["signals"]))
        self.assertEqual(result["visual_overlay"]["status"], "EYES_CLOSED")
        self.assertEqual(result["visual_overlay"]["label"], "Mắt nhắm")

    def test_request_models_accept_camel_case_payloads(self):
        frame = FrameAnalysisRequest.model_validate({
            "attemptId": 11,
            "studentId": 22,
            "imageBase64": image_to_base64((10, 10, 10)),
            "capturedAt": "2026-05-07T00:00:00Z",
        })
        behavior = BehaviorAnalysisRequest.model_validate({
            "attemptId": 11,
            "studentId": 22,
            "pasteLength": 12,
            "tabSwitchCount": 3,
            "idleSeconds": 5,
            "typingIntervals": [100, 120],
        })

        self.assertEqual(frame.image_base64[:10], image_to_base64((10, 10, 10))[:10])
        self.assertEqual(frame.attempt_id, 11)
        self.assertEqual(frame.student_id, 22)
        self.assertEqual(frame.captured_at, "2026-05-07T00:00:00Z")
        self.assertEqual(behavior.paste_length, 12)
        self.assertEqual(behavior.tab_switch_count, 3)
        self.assertEqual(behavior.typing_intervals, [100, 120])

    def test_haar_detection_merges_duplicate_boxes_from_multiple_passes(self):
        analyzer = NoFaceAnalyzer()
        faces = analyzer._merge_face_locations([
            (20, 20, 70, 70),
            (23, 22, 68, 68),
            (120, 20, 60, 60),
        ])

        self.assertEqual(len(faces), 2)
        self.assertTrue(any(face[0] <= 20 and face[2] >= 71 for face in faces))

    def test_dnn_detection_converts_rgb_to_bgr_before_blob_creation(self):
        analyzer = ProctorAnalyzer.__new__(ProctorAnalyzer)
        analyzer._dnn_net = type("FakeNet", (), {
            "setInput": lambda self, blob: None,
            "forward": lambda self: np.array([[[[0.0, 0.0, 0.98, 0.10, 0.20, 0.60, 0.80]]]], dtype=np.float32),
        })()
        analyzer._dnn_model_type = "CAFFE"
        analyzer._dnn_confidence_threshold = 0.5
        analyzer._face_detector_backend = "DNN_CAFFE"
        analyzer._face_detector_error = None

        captured = {}

        def fake_blob_from_image(image, *args, **kwargs):
            captured["image"] = image.copy()
            return np.zeros((1, 1, 1, 1), dtype=np.float32)

        with patch("app.proctor.cv2.dnn.blobFromImage", side_effect=fake_blob_from_image):
            rgb_array = np.full((8, 8, 3), [12, 34, 56], dtype=np.uint8)
            gray = np.zeros((8, 8), dtype=np.uint8)
            face_count, faces, used_fallback = analyzer._detect_faces_dnn(rgb_array, gray)

        self.assertEqual(face_count, 1)
        self.assertFalse(used_fallback)
        self.assertEqual(faces, [(0, 1, 4, 5)])
        self.assertEqual(captured["image"][0, 0].tolist(), [56, 34, 12])

    def test_analyze_frame_uses_haar_fallback_when_dnn_finds_no_face(self):
        class NoFaceNet:
            def setInput(self, blob):
                return None

            def forward(self):
                return np.zeros((1, 1, 0, 7), dtype=np.float32)

        class DnnFallbackAnalyzer(ProctorAnalyzer):
            def __init__(self):
                self._dnn_net = NoFaceNet()
                self._dnn_model_type = "CAFFE"
                self._face_detector_backend = "DNN_CAFFE"
                self._face_detector_error = None
                self._dnn_confidence_threshold = 0.5
                self.eye_tracking = {"eye_count": 2}
                self.spoofing = {"detected": False, "confidence": 0.0, "type": None, "dl_enhanced": False}
                self.occlusion = {"mask_detected": False, "sunglasses_detected": False, "partial_face": False, "coverage_percent": 100}
                self.position = {"face_turned": False, "face_not_centered": False, "turn_angle": 0, "center_offset": {"x": 0, "y": 0}}
                self.gaze = {"gaze_off_screen": False, "rapid_eye_movement": False, "gaze_confidence": 0.0, "primary_direction": "CENTER", "off_duration": 0, "movement_count": 0, "attention_score": 1.0}

            def dnn_ready(self):
                return True

            def cv_ready(self):
                return True

            def eye_landmark_ready(self):
                return False

            def spoofing_dl_ready(self):
                return False

            def _track_eyes(self, rgb_array, gray, face_locations):
                return {
                    "eye_count": 2,
                    "left_eye": None,
                    "right_eye": None,
                    "eye_state": "OPEN",
                    "blink_anomaly": False,
                    "blink_rate": 0,
                    "prolonged_eye_closure": False,
                    "closure_duration": 0,
                    "closure_duration_ms": 0,
                    "eye_aspect_ratio": 0.28,
                    "tracking_confidence": 0.9,
                    "raw_eye_count": 2,
                    "landmarks": [(65, 55), (95, 55)],
                    "pupil_positions": [],
                }

            def _detect_spoofing_deep(self, rgb_array, gray, face_count, face_locations):
                return self.spoofing

            def _detect_occlusion(self, gray, face_locations, eye_tracking):
                return self.occlusion

            def _analyze_face_position(self, face_locations, frame_shape):
                return self.position

            def _analyze_gaze(self, eye_tracking, face_locations, frame_shape):
                return self.gaze

            def _detect_faces_haar(self, gray_image):
                return 1, [(20, 18, 60, 60)]

        result = DnnFallbackAnalyzer().analyze_frame(image_to_base64((128, 128, 128)))

        self.assertEqual(result["face_count"], 1)
        self.assertEqual(result["diagnostics"]["detection_method"], "CAFFE+HAAR")
        self.assertEqual(result["diagnostics"]["face_detector"]["backend"], "DNN_CAFFE")


if __name__ == "__main__":
    unittest.main()
