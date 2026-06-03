from __future__ import annotations

import base64
import io
import os
import re
import shutil
import unicodedata
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter, ImageOps

try:
    import cv2
except Exception:
    cv2 = None

try:
    import pytesseract
except Exception:
    pytesseract = None


class IdentityVerifier:
    DEFAULT_TESSERACT_CMD = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

    def __init__(self) -> None:
        self._face_cascade = None
        self._tesseract_cmd = self._resolve_tesseract_cmd()
        self._ocr_available = pytesseract is not None and self._tesseract_cmd is not None
        self._last_ocr_error: str | None = None
        if pytesseract is not None and self._tesseract_cmd is not None:
            pytesseract.pytesseract.tesseract_cmd = self._tesseract_cmd
        if cv2 is not None:
            try:
                self._face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
            except Exception:
                self._face_cascade = None

    def _resolve_tesseract_cmd(self) -> str | None:
        for candidate in (os.getenv("TESSERACT_CMD"), shutil.which("tesseract"), self.DEFAULT_TESSERACT_CMD):
            if candidate and Path(candidate).exists():
                return str(Path(candidate))
        return None

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
        matched_fields, mismatched_fields = self._compare_expected(expected, ocr_fields, document_type)
        face_match = self._face_match(document_image, selfie_image, document_faces, selfie_faces)
        document_face_crop = self._document_face_crop(document_image, document_faces)
        liveness = self._liveness(selfie_image, selfie_faces)

        signals = []
        review_reasons = []
        if not document_text.strip() and not self._ocr_available:
            signals.append(self._signal(
                "IDENTITY_OCR_UNAVAILABLE",
                "MEDIUM",
                0.68,
                {"reason": self._last_ocr_error or "OCR engine unavailable"},
            ))
            review_reasons.append("Cần giám thị kiểm tra giấy tờ do OCR chưa sẵn sàng")
        elif not document_text.strip():
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

        if selfie_faces.get("count", 0) == 1 and not liveness.get("passed"):
            signals.append(self._signal("LIVENESS_CHALLENGE_FAILED", "HIGH", 0.86, liveness))
            review_reasons.append("Liveness check failed")

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
        elif not liveness.get("passed", False):
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
            "document_face_crop": document_face_crop,
            "liveness": liveness,
            "signals": signals,
            "review_reason": "; ".join(dict.fromkeys(review_reasons)) or None,
            "diagnostics": {
                "documentType": document_type,
                "metadata": metadata,
                "ocrReady": self._ocr_available,
                "ocrError": self._last_ocr_error,
                "tesseractCmd": self._tesseract_cmd,
                "faceDetectorReady": self._face_cascade is not None and not self._face_cascade.empty(),
                "documentFaces": document_faces,
                "selfieFaces": selfie_faces,
            },
        }

    def recheck(
        self,
        image_base64: str,
        reference_face_base64: str,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        metadata = metadata or {}
        frame_image = self._decode_image(image_base64)
        reference_image = self._decode_image(reference_face_base64)
        frame_faces = self._detect_faces(frame_image)
        reference_faces = {"count": 1, "boxes": [[0, 0, reference_image.width, reference_image.height]], "method": "REFERENCE_CROP"}
        face_match = self._face_match(reference_image, frame_image, reference_faces, frame_faces)
        liveness = self._liveness(frame_image, frame_faces)

        signals = []
        review_reasons = []
        if frame_faces.get("method") == "UNAVAILABLE":
            signals.append(self._signal("IDENTITY_REVIEW_REQUIRED", "MEDIUM", 0.68, {"reason": "Face detector unavailable"}))
            review_reasons.append("Cần giám thị kiểm tra danh tính")
        elif frame_faces.get("count", 0) == 0:
            signals.append(self._signal("IDENTITY_FACE_MISMATCH", "HIGH", 0.9, {"reason": "No face in camera frame"}))
            review_reasons.append("Không phát hiện khuôn mặt trong khung hình")
        elif frame_faces.get("count", 0) > 1:
            signals.append(self._signal("IDENTITY_FACE_MISMATCH", "HIGH", 0.88, {"reason": "Multiple faces in camera frame"}))
            review_reasons.append("Khung hình có nhiều khuôn mặt")
        elif face_match.get("available") and not face_match.get("matched"):
            signals.append(self._signal("IDENTITY_FACE_MISMATCH", "HIGH", 0.86, face_match))
            review_reasons.append("Khuôn mặt camera không khớp ảnh giấy tờ")
        elif not face_match.get("available"):
            signals.append(self._signal("IDENTITY_REVIEW_REQUIRED", "MEDIUM", 0.68, {"reason": face_match.get("reason")}))
            review_reasons.append("Cần giám thị kiểm tra khuôn mặt")

        if frame_faces.get("count", 0) == 1 and not liveness.get("passed"):
            signals.append(self._signal("LIVENESS_CHALLENGE_FAILED", "HIGH", 0.86, liveness))
            review_reasons.append("Liveness check failed")

        confidence = round(max(0.0, min(1.0, float(face_match.get("confidence") or 0.0) * 0.75 + float(liveness.get("score") or 0.0) * 0.25)), 3)
        if frame_faces.get("method") == "UNAVAILABLE":
            verification_status = "NEEDS_REVIEW"
        elif frame_faces.get("count", 0) != 1:
            verification_status = "REJECTED"
        elif not liveness.get("passed", False):
            verification_status = "NEEDS_REVIEW"
        elif face_match.get("matched") and confidence >= 0.62:
            verification_status = "VERIFIED"
        else:
            verification_status = "NEEDS_REVIEW"

        return {
            "status": "DONE",
            "verification_status": verification_status,
            "confidence": confidence,
            "matched_fields": {},
            "mismatched_fields": {},
            "document_ocr": {},
            "face_match": face_match,
            "document_face_crop": {},
            "liveness": liveness,
            "signals": signals,
            "review_reason": "; ".join(dict.fromkeys(review_reasons)) or None,
            "diagnostics": {
                "metadata": metadata,
                "frameFaces": frame_faces,
                "referenceFace": {"width": reference_image.width, "height": reference_image.height},
            },
        }

    def _decode_image(self, image_base64: str) -> Image.Image:
        raw = image_base64.split(",", 1)[1] if "," in image_base64 else image_base64
        image_bytes = base64.b64decode(raw)
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")

    def _ocr(self, image: Image.Image) -> str:
        self._last_ocr_error = None
        if pytesseract is None:
            self._last_ocr_error = "pytesseract module unavailable"
            return ""
        if not self._ocr_available:
            self._last_ocr_error = "tesseract executable unavailable"
            return ""

        errors: list[str] = []
        for prepared in self._ocr_variants(image):
            text = self._ocr_with_fallback_languages(prepared, errors)
            if text:
                return text
        self._last_ocr_error = "; ".join(dict.fromkeys(errors)) if errors else None
        return ""

    def _ocr_variants(self, image: Image.Image) -> list[Image.Image]:
        grayscale = ImageOps.grayscale(image)
        contrasted = ImageEnhance.Contrast(grayscale).enhance(1.8)
        sharpened = contrasted.filter(ImageFilter.SHARPEN)
        threshold = sharpened.point(lambda p: 255 if p > 150 else 0)
        return [grayscale, contrasted, sharpened, threshold]

    def _ocr_with_fallback_languages(self, image: Image.Image, errors: list[str]) -> str:
        for lang in ("vie+eng", "eng"):
            try:
                text = pytesseract.image_to_string(image, lang=lang).strip()
                if text:
                    return text
            except Exception as exc:
                errors.append(f"{lang}: {type(exc).__name__}: {exc}")
        return ""

    def _extract_fields(self, text: str) -> dict[str, Any]:
        lines = [line.strip() for line in (text or "").splitlines() if line.strip()]
        joined = " ".join(lines)
        fields: dict[str, Any] = {}

        document_match = re.search(r"\b(\d{12})\b", joined)
        if document_match:
            fields["documentNumber"] = document_match.group(1)

        code_match = None
        if not document_match:
            code_match = re.search(r"\b(?:mssv|student\s*id|id|so|số|no)[:\s/.-]*([A-Z0-9]{5,20})\b", joined, re.IGNORECASE)
        if code_match:
            fields["studentCode"] = code_match.group(1)
        elif document_match:
            fields["studentCode"] = document_match.group(1)

        dob_match = re.search(r"\b(\d{1,2}[/-]\d{1,2}[/-]\d{4})\b", joined)
        if dob_match:
            fields["dateOfBirth"] = dob_match.group(1)

        name = self._extract_name(lines, joined)
        if name:
            fields["fullName"] = name
        elif lines:
            candidate = self._best_name_candidate(lines)
            if len(candidate) >= 6 and not re.search(r"\d{4,}", candidate):
                fields["fullName"] = candidate
        return fields

    def _extract_name(self, lines: list[str], joined: str) -> str | None:
        label_pattern = re.compile(r"(họ\s*và\s*tên|ho\s*va\s*ten|họ\s*tên|ho\s*ten|full\s*name|name)", re.IGNORECASE)
        stop_pattern = re.compile(r"(ngày\s*sinh|date\s*of\s*birth|giới\s*tính|sex|quốc\s*tịch|nationality)", re.IGNORECASE)
        for index, line in enumerate(lines):
            if not label_pattern.search(line):
                continue
            after_label = re.split(label_pattern, line, maxsplit=1)[-1]
            after_label = re.sub(r"^[\s:/.-]+", "", after_label).strip()
            after_label = re.sub(r"^(?:full\s*name|name|họ\s*và\s*tên|ho\s*va\s*ten|họ\s*tên|ho\s*ten)[:\s/.-]+", "", after_label, flags=re.IGNORECASE).strip()
            if after_label and not stop_pattern.search(after_label):
                return " ".join(after_label.split())
            if index + 1 < len(lines):
                next_line = stop_pattern.split(lines[index + 1], maxsplit=1)[0].strip()
                if next_line and not re.search(r"\d{4,}", next_line):
                    return " ".join(next_line.split())

        name_match = re.search(
            r"(?:họ\s*và\s*tên|ho\s*va\s*ten|họ\s*tên|ho\s*ten|full\s*name|name)[:\s/.-]+(.+?)(?:ngày\s*sinh|date\s*of\s*birth|giới\s*tính|sex|$)",
            joined,
            re.IGNORECASE,
        )
        if not name_match:
            return None
        candidate = name_match.group(1).strip(" :/.-")
        return " ".join(candidate.split()) if candidate else None

    def _best_name_candidate(self, lines: list[str]) -> str:
        excluded = (
            "cong hoa",
            "cộng hòa",
            "socialist",
            "citizen",
            "identity",
            "date of birth",
            "nationality",
            "place",
            "sex",
        )
        candidates = []
        for line in lines:
            normalized = self._normalize(line)
            if any(token.replace(" ", "") in normalized for token in excluded):
                continue
            if re.search(r"\d", line):
                continue
            if len(line.split()) < 2:
                continue
            letters = [ch for ch in line if ch.isalpha()]
            uppercase_ratio = sum(1 for ch in letters if ch.isupper()) / max(len(letters), 1)
            candidates.append((uppercase_ratio, len(line), line))
        if candidates:
            return max(candidates)[2]
        return max(lines[:5], key=len)

    def _detect_faces(self, image: Image.Image) -> dict[str, Any]:
        if cv2 is None or self._face_cascade is None or self._face_cascade.empty():
            return {"count": 0, "boxes": [], "method": "UNAVAILABLE"}
        gray = np.array(image.convert("L"))
        faces = self._face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(40, 40))
        boxes = [[int(x), int(y), int(w), int(h)] for (x, y, w, h) in faces]
        boxes = self._filter_face_boxes(boxes)
        return {"count": len(boxes), "boxes": boxes, "method": "HAAR"}

    def _filter_face_boxes(self, boxes: list[list[int]]) -> list[list[int]]:
        if len(boxes) <= 1:
            return boxes
        largest_area = max(w * h for _, _, w, h in boxes)
        if largest_area <= 0:
            return boxes
        filtered = [
            box
            for box in boxes
            if (box[2] * box[3]) / largest_area >= 0.25
        ]
        return filtered or boxes

    def _face_match(self, document_image: Image.Image, selfie_image: Image.Image, document_faces: dict[str, Any], selfie_faces: dict[str, Any]) -> dict[str, Any]:
        if document_faces.get("count", 0) < 1 or selfie_faces.get("count", 0) < 1:
            return {"available": False, "matched": False, "confidence": 0.0, "reason": "Missing face in document or selfie"}
        if document_faces.get("count", 0) > 1 or selfie_faces.get("count", 0) > 1:
            return {"available": True, "matched": False, "confidence": 0.2, "reason": "Multiple faces detected"}
        try:
            doc_face = self._crop_face(document_image, document_faces["boxes"][0])
            selfie_face = self._crop_face(selfie_image, selfie_faces["boxes"][0])
            similarity, components = self._face_similarity(doc_face, selfie_face)
            confidence = round(max(0.0, min(1.0, similarity)), 3)
            if confidence < 0.45:
                return {
                    "available": True,
                    "matched": False,
                    "confidence": confidence,
                    "method": "opencv_composite_fallback",
                    "components": components,
                    "reason": "Face similarity below mismatch threshold",
                }
            if confidence < 0.55:
                return {
                    "available": False,
                    "matched": False,
                    "confidence": confidence,
                    "method": "opencv_composite_fallback",
                    "components": components,
                    "reason": "Low-confidence fallback face comparison",
                }
            return {
                "available": True,
                "matched": True,
                "confidence": confidence,
                "method": "opencv_composite_fallback",
                "components": components,
            }
        except Exception:
            return {"available": False, "matched": False, "confidence": 0.0, "reason": "Face comparison failed"}

    def _document_face_crop(self, document_image: Image.Image, document_faces: dict[str, Any]) -> dict[str, Any]:
        if document_faces.get("method") == "UNAVAILABLE":
            return {"available": False, "reason": "Face detector unavailable", "method": "UNAVAILABLE"}
        if document_faces.get("count", 0) < 1:
            return {"available": False, "reason": "No face in document", "method": document_faces.get("method")}
        if document_faces.get("count", 0) > 1:
            return {"available": False, "reason": "Multiple faces in document", "method": document_faces.get("method"), "boxes": document_faces.get("boxes", [])}
        box = document_faces["boxes"][0]
        crop = self._crop_face(document_image, box)
        buffer = io.BytesIO()
        crop.save(buffer, format="JPEG", quality=90)
        image_base64 = base64.b64encode(buffer.getvalue()).decode("ascii")
        area = max(0, int(box[2])) * max(0, int(box[3]))
        image_area = max(1, document_image.width * document_image.height)
        quality = round(max(0.0, min(1.0, area / image_area * 8.0)), 3)
        return {
            "available": True,
            "imageBase64": image_base64,
            "image_base64": image_base64,
            "box": box,
            "confidence": max(0.55, quality),
            "quality": quality,
            "method": document_faces.get("method"),
        }

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
        pad_x = int(w * 0.18)
        pad_y = int(h * 0.22)
        left = max(0, x - pad_x)
        top = max(0, y - pad_y)
        right = min(image.width, x + w + pad_x)
        bottom = min(image.height, y + h + pad_y)
        return image.crop((left, top, right, bottom)).resize((160, 160))

    def _face_similarity(self, first: Image.Image, second: Image.Image) -> tuple[float, dict[str, float]]:
        color_score = self._color_similarity(first, second)
        gray_score = self._gray_similarity(first, second)
        edge_score = self._edge_similarity(first, second)
        keypoint_score = self._keypoint_similarity(first, second)
        composite = (
            0.30 * color_score
            + 0.35 * gray_score
            + 0.20 * edge_score
            + 0.15 * keypoint_score
        )
        components = {
            "color": round(color_score, 3),
            "gray": round(gray_score, 3),
            "edge": round(edge_score, 3),
            "keypoint": round(keypoint_score, 3),
        }
        return composite, components

    def _color_similarity(self, first: Image.Image, second: Image.Image) -> float:
        first_hist = self._histogram(first)
        second_hist = self._histogram(second)
        return float(np.minimum(first_hist, second_hist).sum())

    def _gray_similarity(self, first: Image.Image, second: Image.Image) -> float:
        first_arr = np.array(ImageOps.grayscale(first), dtype=np.float32)
        second_arr = np.array(ImageOps.grayscale(second), dtype=np.float32)
        first_arr = (first_arr - first_arr.mean()) / (first_arr.std() + 1e-6)
        second_arr = (second_arr - second_arr.mean()) / (second_arr.std() + 1e-6)
        correlation = float(np.mean(first_arr * second_arr))
        return max(0.0, min(1.0, (correlation + 1.0) / 2.0))

    def _edge_similarity(self, first: Image.Image, second: Image.Image) -> float:
        first_edges = self._edge_map(first)
        second_edges = self._edge_map(second)
        intersection = np.logical_and(first_edges, second_edges).sum()
        union = np.logical_or(first_edges, second_edges).sum()
        if union == 0:
            return 0.0
        return float(intersection / union)

    def _edge_map(self, image: Image.Image) -> np.ndarray:
        gray = np.array(ImageOps.grayscale(image))
        if cv2 is not None:
            return cv2.Canny(gray, 70, 180) > 0
        return np.array(ImageOps.grayscale(image).filter(ImageFilter.FIND_EDGES)) > 30

    def _keypoint_similarity(self, first: Image.Image, second: Image.Image) -> float:
        if cv2 is None:
            return 0.0
        first_gray = np.array(ImageOps.grayscale(first))
        second_gray = np.array(ImageOps.grayscale(second))
        orb = cv2.ORB_create(nfeatures=120)
        first_keypoints, first_descriptors = orb.detectAndCompute(first_gray, None)
        second_keypoints, second_descriptors = orb.detectAndCompute(second_gray, None)
        if first_descriptors is None or second_descriptors is None or not first_keypoints or not second_keypoints:
            return 0.0
        matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
        matches = matcher.match(first_descriptors, second_descriptors)
        if not matches:
            return 0.0
        good = [match for match in matches if match.distance <= 72]
        return min(1.0, len(good) / max(min(len(first_keypoints), len(second_keypoints)), 1))

    def _histogram(self, image: Image.Image) -> np.ndarray:
        arr = np.array(image.convert("RGB"))
        hist, _ = np.histogramdd(arr.reshape(-1, 3), bins=(8, 8, 8), range=((0, 256), (0, 256), (0, 256)))
        hist = hist.astype("float32").ravel()
        total = hist.sum()
        return hist / total if total > 0 else hist

    def _compare_expected(
        self,
        expected: dict[str, Any],
        ocr_fields: dict[str, Any],
        document_type: str | None = None,
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        matched = {}
        mismatched = {}
        aliases = {
            "fullName": ["fullName", "name", "displayName"],
            "studentCode": ["studentCode", "studentId", "code", "documentNumber"],
            "citizenId": ["citizenId", "citizenID", "cccd", "documentNumber"],
            "dateOfBirth": ["dateOfBirth", "birthDate", "dob"],
        }
        normalized_doc_type = self._normalize(document_type or "")
        required = set()
        if "cccd" in normalized_doc_type or "citizen" in normalized_doc_type:
            required.update(("citizenId", "fullName", "dateOfBirth"))
        elif "student" in normalized_doc_type:
            required.update(("studentCode", "fullName"))
            if ocr_fields.get("dateOfBirth"):
                required.add("dateOfBirth")
        for canonical, keys in aliases.items():
            expected_value = next((expected.get(key) for key in keys if expected.get(key)), None)
            observed_value = next((ocr_fields.get(key) for key in keys if ocr_fields.get(key)), None)
            if canonical == "citizenId" and observed_value is None:
                observed_value = ocr_fields.get("documentNumber")
            if not expected_value:
                continue
            if not observed_value:
                if canonical in required:
                    mismatched[canonical] = {"expected": expected_value, "observed": None, "reason": "missing_in_document"}
                continue
            if self._field_matches(canonical, expected_value, observed_value):
                matched[canonical] = {"expected": expected_value, "observed": observed_value}
            else:
                mismatched[canonical] = {"expected": expected_value, "observed": observed_value}
        return matched, mismatched

    def _field_matches(self, canonical: str, expected_value: Any, observed_value: Any) -> bool:
        if canonical == "dateOfBirth":
            expected_date = self._normalize_date(expected_value)
            observed_date = self._normalize_date(observed_value)
            if expected_date and observed_date:
                return expected_date == observed_date
        expected_norm = self._normalize(expected_value)
        observed_norm = self._normalize(observed_value)
        return expected_norm in observed_norm or observed_norm in expected_norm

    def _normalize_date(self, value: Any) -> str | None:
        text = str(value or "").strip()
        patterns = (
            r"^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$",
            r"^(\d{1,2})[-/](\d{1,2})[-/](\d{4})$",
        )
        for index, pattern in enumerate(patterns):
            match = re.match(pattern, text)
            if not match:
                continue
            if index == 0:
                year, month, day = match.groups()
            else:
                day, month, year = match.groups()
            return f"{int(year):04d}{int(month):02d}{int(day):02d}"
        digits = re.sub(r"\D+", "", text)
        return digits if len(digits) == 8 else None

    def _normalize(self, value: Any) -> str:
        text = str(value).strip().lower().replace("đ", "d")
        text = unicodedata.normalize("NFKD", text)
        text = "".join(ch for ch in text if not unicodedata.combining(ch))
        return re.sub(r"[^a-z0-9]+", "", text)

    def _signal(self, signal_type: str, severity: str, confidence: float, evidence: dict[str, Any]) -> dict[str, Any]:
        return {
            "signal_type": signal_type,
            "severity": severity,
            "confidence": max(0.0, min(1.0, confidence)),
            "evidence": evidence or {},
        }
