import base64
import io
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient
from PIL import Image

from app import main as app_main
from app.identity import IdentityVerifier
from app.schemas import IdentityVerifyResponse


def image_to_base64(color=(120, 120, 120)):
    image = Image.new("RGB", (120, 120), color=color)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG")
    return base64.b64encode(buffer.getvalue()).decode("ascii")


class UnavailableFaceVerifier(IdentityVerifier):
    def _ocr(self, image):
        return "Họ tên: Nguyen Van A\nMSSV: SV12345"

    def _detect_faces(self, image):
        return {"count": 0, "boxes": [], "method": "UNAVAILABLE"}

    def _face_match(self, document_image, selfie_image, document_faces, selfie_faces):
        return {
            "available": False,
            "matched": False,
            "confidence": 0.0,
            "reason": "Face detector unavailable",
        }

    def _liveness(self, selfie_image, selfie_faces):
        return {"passed": False, "score": 0.0, "method": "test"}


class MatchedIdentityVerifier(IdentityVerifier):
    def _ocr(self, image):
        return "Họ tên: Nguyen Van A\nMSSV: SV12345"

    def _detect_faces(self, image):
        return {"count": 1, "boxes": [[20, 20, 60, 60]], "method": "TEST"}

    def _face_match(self, document_image, selfie_image, document_faces, selfie_faces):
        return {"available": True, "matched": True, "confidence": 0.95, "method": "test"}

    def _liveness(self, selfie_image, selfie_faces):
        return {"passed": True, "score": 0.92, "method": "test"}


class IdentityVerifierTest(unittest.TestCase):
    def test_tesseract_resolver_uses_configured_env_path(self):
        with patch("app.identity.Path.exists", return_value=True), patch.dict("os.environ", {"TESSERACT_CMD": r"C:\Custom\tesseract.exe"}):
            verifier = IdentityVerifier()

        self.assertEqual(verifier._tesseract_cmd, r"C:\Custom\tesseract.exe")

    def test_tesseract_resolver_falls_back_to_windows_default(self):
        with patch("app.identity.shutil.which", return_value=None), patch("app.identity.Path.exists", return_value=True), patch.dict("os.environ", {}, clear=True):
            verifier = IdentityVerifier()

        self.assertEqual(verifier._tesseract_cmd, IdentityVerifier.DEFAULT_TESSERACT_CMD)

    def test_ocr_unavailable_is_reported_as_review_not_unreadable_document(self):
        class OcrUnavailableVerifier(MatchedIdentityVerifier):
            def __init__(self):
                super().__init__()
                self._ocr_available = False
                self._tesseract_cmd = None

            def _ocr(self, image):
                self._last_ocr_error = "tesseract executable unavailable"
                return ""

        result = OcrUnavailableVerifier().verify(
            document_image_base64=image_to_base64(),
            selfie_image_base64=image_to_base64(),
            expected={},
        )

        IdentityVerifyResponse.model_validate(result)
        self.assertEqual(result["verification_status"], "NEEDS_REVIEW")
        self.assertFalse(result["diagnostics"]["ocrReady"])
        self.assertIn("IDENTITY_OCR_UNAVAILABLE", [signal["signal_type"] for signal in result["signals"]])

    def test_extract_fields_supports_cccd_number_date_and_name(self):
        result = IdentityVerifier()._extract_fields(
            "CĂN CƯỚC CÔNG DÂN\n"
            "Số / No.: 049204010238\n"
            "Họ và tên / Full name: VĂN HỮU THÀNH LONG\n"
            "Ngày sinh / Date of birth: 24/09/2004"
        )

        self.assertEqual(result["documentNumber"], "049204010238")
        self.assertEqual(result["studentCode"], "049204010238")
        self.assertEqual(result["dateOfBirth"], "24/09/2004")
        self.assertEqual(result["fullName"], "VĂN HỮU THÀNH LONG")

    def test_compare_expected_matches_vietnamese_names_without_accents(self):
        matched, mismatched = IdentityVerifier()._compare_expected(
            {"fullName": "VĂN HỮU THÀNH LONG", "studentCode": "049204010238"},
            {"fullName": "VAN HỮU THANH LONG", "documentNumber": "049204010238"},
        )

        self.assertIn("fullName", matched)
        self.assertIn("studentCode", matched)
        self.assertFalse(mismatched)

    def test_face_match_uses_composite_similarity_for_same_face_crop(self):
        verifier = IdentityVerifier()
        image = Image.new("RGB", (180, 180), color=(180, 145, 125))
        pixels = image.load()
        for x in range(60, 125):
            for y in range(45, 55):
                pixels[x, y] = (40, 35, 30)
        for x in range(70, 85):
            for y in range(85, 95):
                pixels[x, y] = (25, 25, 25)
        for x in range(110, 125):
            for y in range(85, 95):
                pixels[x, y] = (25, 25, 25)
        for x in range(85, 115):
            for y in range(125, 130):
                pixels[x, y] = (100, 45, 45)
        result = verifier._face_match(
            image,
            image,
            {"count": 1, "boxes": [[35, 35, 100, 100]]},
            {"count": 1, "boxes": [[35, 35, 100, 100]]},
        )

        self.assertTrue(result["matched"])
        self.assertGreaterEqual(result["confidence"], 0.55)
        self.assertEqual(result["method"], "opencv_composite_fallback")
        self.assertIn("components", result)

    def test_face_detector_unavailable_needs_review_not_verified(self):
        result = UnavailableFaceVerifier().verify(
            document_image_base64=image_to_base64(),
            selfie_image_base64=image_to_base64(),
            expected={"fullName": "Nguyen Van A", "studentCode": "SV12345"},
        )

        IdentityVerifyResponse.model_validate(result)
        self.assertEqual(result["verification_status"], "NEEDS_REVIEW")
        self.assertIn("Cần giám thị", result["review_reason"])

    def test_identity_endpoint_accepts_camel_case_and_returns_backend_contract(self):
        payload = {
            "attemptId": 801,
            "studentId": 901,
            "documentImageBase64": image_to_base64(),
            "selfieImageBase64": image_to_base64(),
            "documentType": "STUDENT_ID",
            "expected": {"fullName": "Nguyen Van A", "studentCode": "SV12345"},
            "metadata": {"source": "test"},
        }

        with patch.object(app_main, "get_identity_verifier", return_value=MatchedIdentityVerifier()):
            response = TestClient(app_main.app).post("/identity/verify", json=payload)

        self.assertEqual(response.status_code, 200)
        body = response.json()
        IdentityVerifyResponse.model_validate(body)
        self.assertEqual(body["verificationStatus"], "VERIFIED")
        self.assertIn("matchedFields", body)
        self.assertIn("documentOcr", body)
        self.assertIn("faceMatch", body)
        self.assertTrue(body["documentFaceCrop"]["available"])
        self.assertIn("imageBase64", body["documentFaceCrop"])

    def test_identity_recheck_endpoint_compares_reference_face_to_frame(self):
        payload = {
            "attemptId": 803,
            "studentId": 903,
            "imageBase64": image_to_base64(),
            "referenceFaceBase64": image_to_base64(),
            "metadata": {"checkType": "PERIODIC"},
        }

        with patch.object(app_main, "get_identity_verifier", return_value=MatchedIdentityVerifier()):
            response = TestClient(app_main.app).post("/identity/recheck", json=payload)

        self.assertEqual(response.status_code, 200)
        body = response.json()
        IdentityVerifyResponse.model_validate(body)
        self.assertEqual(body["verificationStatus"], "VERIFIED")
        self.assertIn("faceMatch", body)

    def test_ml_risk_endpoint_accepts_backend_camel_case_contract(self):
        payload = {
            "attemptId": 802,
            "studentId": 902,
            "examId": 1002,
            "signals": {
                "tabSwitchCount": 4,
                "windowBlurCount": 2,
                "fullscreenExitCount": 1,
                "devtoolsOpened": 1,
                "totalSignalCount": 8,
                "highSignalCount": 1,
                "mediumSignalCount": 3,
                "signalsPerMinute": 0.7,
            },
            "context": {"isSharedIp": True},
            "temporal": {"impossiblyFastAnswers": True},
        }

        response = TestClient(app_main.app).post("/ml/risk/predict", json=payload)

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertIn("mlScore", body)
        self.assertIn("riskLevel", body)
        self.assertIn("fraudProbability", body)
        self.assertIn("modelVersion", body)
        self.assertIn("diagnostics", body)
        self.assertEqual(body["diagnostics"]["algorithm"], "weighted_signal_risk_v1")
        self.assertEqual(body["diagnostics"]["attemptId"], 802)
        self.assertEqual(body["diagnostics"]["inputContractVersion"], "ml-risk-v1")
        self.assertEqual(body["diagnostics"]["scoringSource"], "HEURISTIC_BASELINE")
        self.assertFalse(body["diagnostics"]["trainedModelLoaded"])
        self.assertGreater(len(body["diagnostics"]["featureContributions"]), 0)
        self.assertIn("NO_TRAINED_MODEL_LOADED", body["diagnostics"]["warnings"])

    def test_ml_risk_status_reports_heuristic_not_trained_model(self):
        response = TestClient(app_main.app).get("/ml-risk/status")

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertTrue(body["available"])
        self.assertEqual(body["algorithm"], "weighted_signal_risk_v1")
        self.assertFalse(body["trainedModelLoaded"])
        self.assertEqual(body["status"], "HEURISTIC_READY")
        self.assertEqual(body["diagnostics"]["inputContractVersion"], "ml-risk-v1")

    def test_production_mode_rejects_missing_ai_service_api_key(self):
        with patch.dict("os.environ", {"APP_ENV": "production", "AI_SERVICE_ALLOW_AUTH_BYPASS": "false"}):
            app_main._expected_api_key.cache_clear()
            response = TestClient(app_main.app).get("/ml-risk/status")
            app_main._expected_api_key.cache_clear()

        self.assertEqual(response.status_code, 401)

    def test_health_detailed_includes_ml_risk_readiness(self):
        response = TestClient(app_main.app).get("/health/detailed")

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertIn("ml_risk", body["services"])
        self.assertEqual(body["services"]["ml_risk"]["status"], "HEURISTIC_READY")
        self.assertFalse(body["services"]["ml_risk"]["trainedModelLoaded"])


if __name__ == "__main__":
    unittest.main()
