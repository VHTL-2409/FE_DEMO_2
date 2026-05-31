"""
ocr_utils.py — Optional OCR helpers for scanned PDFs and image uploads.
"""

from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
import io
import os
from typing import Optional

try:
    from PIL import Image
except Exception:  # pragma: no cover - optional dependency
    Image = None

try:
    import pytesseract
    from pytesseract import Output
except Exception:  # pragma: no cover - optional dependency
    pytesseract = None
    Output = None


@dataclass(frozen=True)
class OcrWord:
    text: str
    x0: float
    y0: float
    x1: float
    y1: float


def is_ocr_available() -> bool:
    return Image is not None and pytesseract is not None and Output is not None and _tesseract_binary_available()


@lru_cache(maxsize=1)
def _tesseract_binary_available() -> bool:
    if pytesseract is None:
        return False
    tesseract_cmd = getattr(pytesseract.pytesseract, "tesseract_cmd", "tesseract")
    if not tesseract_cmd:
        return False
    if os.path.isabs(tesseract_cmd):
        return os.path.exists(tesseract_cmd)
    for path_entry in os.environ.get("PATH", "").split(os.pathsep):
        candidate = os.path.join(path_entry, tesseract_cmd)
        if os.path.exists(candidate) or os.path.exists(candidate + ".exe"):
            return True
    return False


def _open_image(image_bytes: bytes):
    if Image is None:
        return None
    try:
        return Image.open(io.BytesIO(image_bytes))
    except Exception:
        return None


def ocr_image_bytes_to_text(
    image_bytes: bytes,
    *,
    lang: str = "vie+eng",
    config: str = "--psm 6",
) -> str:
    if not is_ocr_available():
        return ""
    image = _open_image(image_bytes)
    if image is None:
        return ""
    try:
        return pytesseract.image_to_string(image, lang=lang, config=config).strip()
    except Exception:
        try:
            return pytesseract.image_to_string(image, lang="eng", config=config).strip()
        except Exception:
            return ""


def ocr_image_bytes_to_words(
    image_bytes: bytes,
    *,
    lang: str = "vie+eng",
    config: str = "--psm 6",
    scale: float = 1.0,
) -> list[OcrWord]:
    if not is_ocr_available():
        return []
    image = _open_image(image_bytes)
    if image is None:
        return []

    data: Optional[dict] = None
    try:
        data = pytesseract.image_to_data(
            image,
            lang=lang,
            config=config,
            output_type=Output.DICT,
        )
    except Exception:
        try:
            data = pytesseract.image_to_data(
                image,
                lang="eng",
                config=config,
                output_type=Output.DICT,
            )
        except Exception:
            return []

    words: list[OcrWord] = []
    for i, text in enumerate(data.get("text", [])):
        word = (text or "").strip()
        if not word:
            continue
        try:
            left = float(data["left"][i]) / max(scale, 1e-6)
            top = float(data["top"][i]) / max(scale, 1e-6)
            width = float(data["width"][i]) / max(scale, 1e-6)
            height = float(data["height"][i]) / max(scale, 1e-6)
        except Exception:
            continue
        words.append(
            OcrWord(
                text=word,
                x0=left,
                y0=top,
                x1=left + width,
                y1=top + height,
            )
        )
    return words
