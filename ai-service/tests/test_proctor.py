import base64
import io
import unittest

from PIL import Image

from app.proctor import ProctorAnalyzer


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


if __name__ == "__main__":
    unittest.main()
