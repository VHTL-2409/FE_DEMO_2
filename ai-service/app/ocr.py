from __future__ import annotations

import io
import os
import shutil
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Any

from fastapi import UploadFile
from PIL import Image, ImageOps

from .config import env_int

try:
    import fitz  # PyMuPDF
except Exception:  # pragma: no cover - optional dependency guard
    fitz = None

try:
    import pytesseract
except Exception:  # pragma: no cover - optional dependency guard
    pytesseract = None

DEFAULT_TESSERACT_CMD = r"C:\Program Files\Tesseract-OCR\tesseract.exe"


@dataclass
class OcrBlock:
    page: int
    text: str
    confidence: float


class OcrEngine:
    def __init__(self) -> None:
        self.pdf_dpi = env_int("AI_SERVICE_OCR_PDF_DPI", 220)
        self.threshold = env_int("AI_SERVICE_OCR_THRESHOLD", 180)
        self.tesseract_cmd = resolve_tesseract_cmd()
        if pytesseract is not None and self.tesseract_cmd is not None:
            pytesseract.pytesseract.tesseract_cmd = self.tesseract_cmd

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
            text, confidence = self._extract_text_and_confidence(prepared, language)
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
                "tesseract_cmd": self.tesseract_cmd or getattr(pytesseract.pytesseract, "tesseract_cmd", "tesseract"),
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
                    pixmap = document.load_page(index).get_pixmap(dpi=self.pdf_dpi)
                    images.append(Image.open(io.BytesIO(pixmap.tobytes("png"))).convert("RGB"))
                return images
            finally:
                document.close()

        return [Image.open(io.BytesIO(file_bytes)).convert("RGB")]

    def _extract_text_and_confidence(self, image: Image.Image, language: str) -> tuple[str, float]:
        data = pytesseract.image_to_data(image, lang=language, output_type=pytesseract.Output.DICT)
        confidences = []
        lines: dict[tuple[int, int, int], list[tuple[int, str]]] = {}

        texts = data.get("text", [])
        conf_values = data.get("conf", [])
        block_nums = data.get("block_num", [])
        par_nums = data.get("par_num", [])
        line_nums = data.get("line_num", [])
        word_nums = data.get("word_num", [])

        for idx, raw_text in enumerate(texts):
            text = (raw_text or "").strip()
            if text:
                key = (
                    int(block_nums[idx]) if idx < len(block_nums) else 0,
                    int(par_nums[idx]) if idx < len(par_nums) else 0,
                    int(line_nums[idx]) if idx < len(line_nums) else 0,
                )
                word_num = int(word_nums[idx]) if idx < len(word_nums) else idx
                lines.setdefault(key, []).append((word_num, text))

            raw_confidence = conf_values[idx] if idx < len(conf_values) else None
            try:
                parsed = float(raw_confidence)
            except (TypeError, ValueError):
                continue
            if parsed >= 0:
                confidences.append(parsed / 100.0)
        if not confidences:
            confidence = 0.0
        else:
            confidence = sum(confidences) / len(confidences)

        text_lines = [
            " ".join(word for _word_num, word in sorted(words, key=lambda item: item[0]))
            for _key, words in sorted(lines.items(), key=lambda item: item[0])
        ]
        return "\n".join(line for line in text_lines if line).strip(), confidence

    def _preprocess(self, image: Image.Image) -> Image.Image:
        grayscale = ImageOps.grayscale(image)
        # Mild contrast cleanup keeps Vietnamese glyphs readable without over-binarizing.
        return grayscale.point(lambda pixel: 255 if pixel > self.threshold else pixel)

    def _detect_file_type(self, file: UploadFile) -> str:
        filename = (file.filename or "").lower()
        content_type = (file.content_type or "").lower()
        if filename.endswith(".pdf") or content_type == "application/pdf":
            return "pdf"
        return "image"

    def _tesseract_available(self) -> bool:
        return _tesseract_available_cached()


@lru_cache(maxsize=1)
def _tesseract_available_cached() -> bool:
    if pytesseract is None:
        return False
    cmd = resolve_tesseract_cmd()
    if cmd is not None:
        pytesseract.pytesseract.tesseract_cmd = cmd
        return True
    return False


def resolve_tesseract_cmd() -> str | None:
    for candidate in (os.getenv("TESSERACT_CMD"), shutil.which("tesseract"), DEFAULT_TESSERACT_CMD):
        if not candidate:
            continue
        path = Path(candidate)
        if path.exists():
            return str(path)
        if not os.path.isabs(candidate) and _binary_on_path(candidate):
            return candidate
    return None


def _binary_on_path(name: str) -> bool:
    for path_entry in os.environ.get("PATH", "").split(os.pathsep):
        candidate = os.path.join(path_entry, name)
        if os.path.exists(candidate) or os.path.exists(candidate + ".exe"):
            return True
    return False
