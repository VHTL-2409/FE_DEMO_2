from __future__ import annotations

import base64
import io
import re
import unicodedata
from typing import Any

import numpy as np
from PIL import Image, ImageOps

try:
    import cv2
except Exception:
    cv2 = None

try:
    import pytesseract
except Exception:
    pytesseract = None


class IdentityVerifier:
    def __init__(self) -> None:
        self._face_cascade = None
        if cv2 is not None:
            try:
                self._face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
            except Exception:
                self._face_cascade = None

    def verify(
        self,
        document_image_base64: str,
        selfie_image_base64: str,
        expected: dict[str, Any] | None = None,
        document_type: str | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        expected = expected or {}
        metadata = metadata or {}
        document_image = self._decode_image(document_image_base64)
        selfie_image = self._decode_image(selfie_image_base64)

        document_text = self._ocr(document_image)
        ocr_fields = self._extract_fields(document_text)
        document_faces = self._detect_faces(document_image)
        selfie_faces = self._detect_faces(selfie_image)
        face_detector_unavailable = (
            document_faces.get("method") == "UNAVAILABLE"
            or selfie_faces.get("method") == "UNAVAILABLE"
        )
        matched_fields, mismatched_fields = self._compare_expected(expected, ocr_fields)
        face_match = self._face_match(document_image, selfie_image, document_faces, selfie_faces)
        liveness = self._liveness(selfie_image, selfie_faces)

        signals = []
        review_reasons = []
        if not document_text.strip():
            signals.append(self._signal("IDENTITY_DOCUMENT_UNREADABLE", "HIGH", 0.86, {"reason": "No OCR text extracted"}))
            review_reasons.append("Không đọc được giấy tờ")
        if mismatched_fields:
            signals.append(self._signal("IDENTITY_DOCUMENT_MISMATCH", "HIGH", 0.88, {"mismatchedFields": mismatched_fields}))
            review_reasons.append("Thông tin giấy tờ không khớp hồ sơ")
        if face_detector_unavailable:
            signals.append(self._signal(
                "IDENTITY_REVIEW_REQUIRED",
                "MEDIUM",
                0.68,
                {"reason": "Face detector unavailable"},
            ))
            review_reasons.append("Cần giám thị kiểm tra danh tính")
        elif selfie_faces["count"] == 0:
            signals.append(self._signal("LIVENESS_CHALLENGE_FAILED", "HIGH", 0.90, {"reason": "No face in selfie"}))
            review_reasons.append("Không phát hiện khuôn mặt trong selfie")
        elif selfie_faces["count"] > 1:
            signals.append(self._signal("LIVENESS_CHALLENGE_FAILED", "HIGH", 0.88, {"reason": "Multiple faces in selfie"}))
            review_reasons.append("Selfie có nhiều khuôn mặt")
        if face_match.get("available") and not face_match.get("matched"):
            signals.append(self._signal("IDENTITY_FACE_MISMATCH", "HIGH", 0.84, face_match))
            review_reasons.append("Khuôn mặt selfie không khớp giấy tờ")
        elif not face_match.get("available"):
            signals.append(self._signal("IDENTITY_REVIEW_REQUIRED", "MEDIUM", 0.68, {"reason": face_match.get("reason")}))
            review_reasons.append("Cần giám thị kiểm tra khuôn mặt")

        confidence_parts = []
        if matched_fields:
            confidence_parts.append(0.35 * (len(matched_fields) / max(len(matched_fields) + len(mismatched_fields), 1)))
        elif expected:
            confidence_parts.append(0.1)
        else:
            confidence_parts.append(0.2)
        confidence_parts.append(0.35 * float(face_match.get("confidence") or 0.0))
        confidence_parts.append(0.20 * float(liveness.get("score") or 0.0))
        confidence_parts.append(0.10 if document_text.strip() else 0.0)
        confidence = round(max(0.0, min(1.0, sum(confidence_parts))), 3)

        if face_detector_unavailable:
            verification_status = "NEEDS_REVIEW"
        elif selfie_faces["count"] == 0 or document_faces["count"] == 0:
            verification_status = "REJECTED"
        elif mismatched_fields:
            verification_status = "NEEDS_REVIEW"
        elif confidence >= 0.72 and face_match.get("matched", False):
            verification_status = "VERIFIED"
        else:
            verification_status = "NEEDS_REVIEW"

        return {
            "status": "DONE",
            "verification_status": verification_status,
            "confidence": confidence,
            "matched_fields": matched_fields,
            "mismatched_fields": mismatched_fields,
            "document_ocr": {
                "text": document_text,
                "fields": ocr_fields,
                "confidence": 0.85 if document_text.strip() else 0.0,
            },
            "face_match": face_match,
            "liveness": liveness,
            "signals": signals,
            "review_reason": "; ".join(dict.fromkeys(review_reasons)) or None,
            "diagnostics": {
                "documentType": document_type,
                "metadata": metadata,
                "ocrReady": pytesseract is not None,
                "faceDetectorReady": self._face_cascade is not None and not self._face_cascade.empty(),
                "documentFaces": document_faces,
                "selfieFaces": selfie_faces,
            },
        }

    def _decode_image(self, image_base64: str) -> Image.Image:
        raw = image_base64.split(",", 1)[1] if "," in image_base64 else image_base64
        image_bytes = base64.b64decode(raw)
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")

    def _ocr(self, image: Image.Image) -> str:
        if pytesseract is None:
            return ""
        try:
            normalized = ImageOps.grayscale(image)
            return pytesseract.image_to_string(normalized, lang="vie+eng").strip()
        except Exception:
            try:
                return pytesseract.image_to_string(image, lang="eng").strip()
            except Exception:
                return ""

    def _extract_fields(self, text: str) -> dict[str, Any]:
        lines = [line.strip() for line in (text or "").splitlines() if line.strip()]
        joined = " ".join(lines)
        fields: dict[str, Any] = {}
        code_match = re.search(r"\b(?:mssv|student\s*id|id|so|số)[:\s]*([A-Z0-9]{5,20})\b", joined, re.IGNORECASE)
        if code_match:
            fields["studentCode"] = code_match.group(1)
        dob_match = re.search(r"\b(\d{1,2}[/-]\d{1,2}[/-]\d{4})\b", joined)
        if dob_match:
            fields["dateOfBirth"] = dob_match.group(1)
        name_match = re.search(r"(?:họ\s*tên|ho\s*ten|full\s*name|name)[:\s]+([A-ZÀ-Ỵ][A-ZÀ-Ỵa-zà-ỵ\s]{3,80})", joined, re.IGNORECASE)
        if name_match:
            fields["fullName"] = " ".join(name_match.group(1).split())
        elif lines:
            candidate = max(lines[:5], key=len)
            if len(candidate) >= 6 and not re.search(r"\d{4,}", candidate):
                fields["fullName"] = candidate
        return fields

    def _detect_faces(self, image: Image.Image) -> dict[str, Any]:
        if cv2 is None or self._face_cascade is None or self._face_cascade.empty():
            return {"count": 0, "boxes": [], "method": "UNAVAILABLE"}
        gray = np.array(image.convert("L"))
        faces = self._face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(40, 40))
        boxes = [[int(x), int(y), int(w), int(h)] for (x, y, w, h) in faces]
        return {"count": len(boxes), "boxes": boxes, "method": "HAAR"}

    def _face_match(self, document_image: Image.Image, selfie_image: Image.Image, document_faces: dict[str, Any], selfie_faces: dict[str, Any]) -> dict[str, Any]:
        if document_faces.get("count", 0) < 1 or selfie_faces.get("count", 0) < 1:
            return {"available": False, "matched": False, "confidence": 0.0, "reason": "Missing face in document or selfie"}
        if document_faces.get("count", 0) > 1 or selfie_faces.get("count", 0) > 1:
            return {"available": True, "matched": False, "confidence": 0.2, "reason": "Multiple faces detected"}
        try:
            doc_face = self._crop_face(document_image, document_faces["boxes"][0])
            selfie_face = self._crop_face(selfie_image, selfie_faces["boxes"][0])
            doc_hist = self._histogram(doc_face)
            selfie_hist = self._histogram(selfie_face)
            similarity = float(np.minimum(doc_hist, selfie_hist).sum())
            confidence = round(max(0.0, min(1.0, similarity)), 3)
            return {
                "available": True,
                "matched": confidence >= 0.55,
                "confidence": confidence,
                "method": "color_histogram_fallback",
            }
        except Exception:
            return {"available": False, "matched": False, "confidence": 0.0, "reason": "Face comparison failed"}

    def _liveness(self, selfie_image: Image.Image, selfie_faces: dict[str, Any]) -> dict[str, Any]:
        if selfie_faces.get("count", 0) != 1:
            return {"passed": False, "score": 0.0, "method": "single_frame_quality"}
        gray = np.array(selfie_image.convert("L"))
        brightness = float(gray.mean()) if gray.size else 0.0
        variance = float(gray.var()) if gray.size else 0.0
        score = 0.45
        if 45 <= brightness <= 230:
            score += 0.25
        if variance >= 80:
            score += 0.20
        score += 0.10
        score = round(max(0.0, min(1.0, score)), 3)
        return {"passed": score >= 0.55, "score": score, "method": "single_frame_quality", "brightness": round(brightness, 2), "variance": round(variance, 2)}

    def _crop_face(self, image: Image.Image, box: list[int]) -> Image.Image:
        x, y, w, h = box
        return image.crop((x, y, x + w, y + h)).resize((96, 96))

    def _histogram(self, image: Image.Image) -> np.ndarray:
        arr = np.array(image.convert("RGB"))
        hist, _ = np.histogramdd(arr.reshape(-1, 3), bins=(8, 8, 8), range=((0, 256), (0, 256), (0, 256)))
        hist = hist.astype("float32").ravel()
        total = hist.sum()
        return hist / total if total > 0 else hist

    def _compare_expected(self, expected: dict[str, Any], ocr_fields: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
        matched = {}
        mismatched = {}
        aliases = {
            "fullName": ["fullName", "name", "displayName"],
            "studentCode": ["studentCode", "studentId", "code"],
            "dateOfBirth": ["dateOfBirth", "birthDate", "dob"],
        }
        for canonical, keys in aliases.items():
            expected_value = next((expected.get(key) for key in keys if expected.get(key)), None)
            observed_value = ocr_fields.get(canonical)
            if not expected_value or not observed_value:
                continue
            if self._normalize(expected_value) in self._normalize(observed_value) or self._normalize(observed_value) in self._normalize(expected_value):
                matched[canonical] = {"expected": expected_value, "observed": observed_value}
            else:
                mismatched[canonical] = {"expected": expected_value, "observed": observed_value}
        return matched, mismatched

    def _normalize(self, value: Any) -> str:
        text = unicodedata.normalize("NFKD", str(value).strip().lower())
        text = "".join(ch for ch in text if not unicodedata.combining(ch))
        return re.sub(r"[^a-z0-9]+", "", text)

    def _signal(self, signal_type: str, severity: str, confidence: float, evidence: dict[str, Any]) -> dict[str, Any]:
        return {
            "signal_type": signal_type,
            "severity": severity,
            "confidence": max(0.0, min(1.0, confidence)),
            "evidence": evidence or {},
        }
