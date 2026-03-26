from __future__ import annotations

import io
import os
from dataclasses import dataclass
from typing import Any

from fastapi import UploadFile
from PIL import Image, ImageOps

try:
    import fitz  # PyMuPDF
except Exception:  # pragma: no cover - optional dependency guard
    fitz = None

try:
    import pytesseract
except Exception:  # pragma: no cover - optional dependency guard
    pytesseract = None


@dataclass
class OcrBlock:
    page: int
    text: str
    confidence: float


class OcrEngine:
    def is_ready(self) -> bool:
        return pytesseract is not None and self._tesseract_available()

    def process(self, file: UploadFile, file_bytes: bytes, language: str, max_pages: int) -> dict[str, Any]:
        if pytesseract is None:
            raise RuntimeError("pytesseract is not installed")
        if not self._tesseract_available():
            raise RuntimeError("tesseract binary is not available")

        images = self._load_images(file, file_bytes, max_pages=max_pages)
        blocks: list[OcrBlock] = []
        for page_index, image in enumerate(images, start=1):
            prepared = self._preprocess(image)
            text = pytesseract.image_to_string(prepared, lang=language).strip()
            confidence = self._average_confidence(prepared, language)
            blocks.append(OcrBlock(page=page_index, text=text, confidence=confidence))

        combined_text = "\n\n".join(block.text for block in blocks if block.text)
        average_confidence = (
            sum(block.confidence for block in blocks) / len(blocks)
            if blocks else 0.0
        )
        return {
            "status": "DONE",
            "filename": file.filename or "upload",
            "file_type": self._detect_file_type(file),
            "page_count": len(blocks),
            "average_confidence": round(average_confidence, 4),
            "needs_review": average_confidence < 0.7 or not combined_text.strip(),
            "text": combined_text,
            "blocks": [
                {
                    "page": block.page,
                    "text": block.text,
                    "confidence": round(block.confidence, 4),
                }
                for block in blocks
            ],
            "diagnostics": {
                "tesseract_cmd": getattr(pytesseract.pytesseract, "tesseract_cmd", "tesseract"),
                "language": language,
            },
        }

    def _load_images(self, file: UploadFile, file_bytes: bytes, max_pages: int) -> list[Image.Image]:
        file_type = self._detect_file_type(file)
        if file_type == "pdf":
            if fitz is None:
                raise RuntimeError("PyMuPDF is required for PDF OCR")
            document = fitz.open(stream=file_bytes, filetype="pdf")
            images: list[Image.Image] = []
            try:
                page_limit = min(document.page_count, max_pages)
                for index in range(page_limit):
                    pixmap = document.load_page(index).get_pixmap(dpi=300)
                    images.append(Image.open(io.BytesIO(pixmap.tobytes("png"))).convert("RGB"))
                return images
            finally:
                document.close()

        return [Image.open(io.BytesIO(file_bytes)).convert("RGB")]

    def _average_confidence(self, image: Image.Image, language: str) -> float:
        data = pytesseract.image_to_data(image, lang=language, output_type=pytesseract.Output.DICT)
        confidences = []
        for raw_confidence in data.get("conf", []):
            try:
                parsed = float(raw_confidence)
            except (TypeError, ValueError):
                continue
            if parsed >= 0:
                confidences.append(parsed / 100.0)
        if not confidences:
            return 0.0
        return sum(confidences) / len(confidences)

    def _preprocess(self, image: Image.Image) -> Image.Image:
        grayscale = ImageOps.grayscale(image)
        # Mild contrast cleanup keeps Vietnamese glyphs readable without over-binarizing.
        return grayscale.point(lambda pixel: 255 if pixel > 180 else pixel)

    def _detect_file_type(self, file: UploadFile) -> str:
        filename = (file.filename or "").lower()
        content_type = (file.content_type or "").lower()
        if filename.endswith(".pdf") or content_type == "application/pdf":
            return "pdf"
        return "image"

    def _tesseract_available(self) -> bool:
        tesseract_cmd = getattr(pytesseract.pytesseract, "tesseract_cmd", "tesseract")
        return bool(tesseract_cmd) and (os.path.isabs(tesseract_cmd) or self._binary_on_path(tesseract_cmd))

    def _binary_on_path(self, name: str) -> bool:
        for path_entry in os.environ.get("PATH", "").split(os.pathsep):
            candidate = os.path.join(path_entry, name)
            if os.path.exists(candidate) or os.path.exists(candidate + ".exe"):
                return True
        return False
