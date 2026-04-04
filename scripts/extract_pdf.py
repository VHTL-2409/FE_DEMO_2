#!/usr/bin/env python3
"""
extract_pdf.py — Extract text from PDF files.
Combines the best of multiple approaches:
  1. pdfplumber (best for text-based PDFs, Vietnamese)
  2. pdfminer.six (fallback for text extraction)
  3. PyMuPDF + Tesseract OCR (for image-based/scanned PDFs)

OCR features:
  - Tesseract v5 with Vietnamese (vie) + English (eng) language data
  - Layout-preserving OCR (process each page as image)
  - Automatic fallback: if text extraction yields < threshold chars/page, use OCR
  - DPI scaling for better math symbol recognition

Usage:
    python extract_pdf.py <input.pdf> [--output <output.txt>] [--ocr] [--force-ocr]
    python extract_pdf.py <input.pdf> --library pdfplumber  # force specific library

    If --output is omitted, prints to stdout.
Exit codes:
    0  — success
    1  — file not found or not a PDF
    2  — no text extracted (image-only PDF, OCR also failed)
"""

import sys
import os
import re
import io
import argparse
from pathlib import Path


# ─── Text Extraction ────────────────────────────────────────────────────────────

def extract_with_pdfplumber(filepath: str) -> str:
    """Extract text using pdfplumber — best for Vietnamese and layout."""
    import pdfplumber
    lines = []
    with pdfplumber.open(filepath) as pdf:
        for page in pdf.pages:
            text = page.extract_text()
            if text:
                lines.append(text)
            else:
                # Empty page — might be image-based
                lines.append("")
        lines.append("")
    return "\n".join(lines)


def extract_with_pdfminer(filepath: str) -> str:
    """Fallback: extract text using pdfminer.six with layout analysis."""
    from pdfminer.high_level import extract_text
    return extract_text(filepath, layout_mode='layout')


def extract_with_pymupdf_text(filepath: str) -> str:
    """Fallback 2: extract text using PyMuPDF."""
    import fitz
    lines = []
    with fitz.open(filepath) as doc:
        for page in doc:
            text = page.get_text()
            if text:
                lines.append(text)
            lines.append("")
    return "\n".join(lines)


# ─── OCR Setup ─────────────────────────────────────────────────────────────────

_TESSERACT_PATH = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
_OCR_AVAILABLE = None
_OCR_ERROR = None


def check_ocr_available():
    """Check if OCR (Tesseract + pytesseract) is available."""
    global _OCR_AVAILABLE, _OCR_ERROR
    if _OCR_AVAILABLE is not None:
        return _OCR_AVAILABLE

    _OCR_AVAILABLE = False
    try:
        import pytesseract
        if not os.path.isfile(_TESSERACT_PATH):
            _OCR_ERROR = f"Tesseract not found at: {_TESSERACT_PATH}"
            return False
        pytesseract.pytesseract.tesseract_cmd = _TESSERACT_PATH
        version = pytesseract.get_tesseract_version()
        _OCR_ERROR = None
        _OCR_AVAILABLE = True
        return True
    except ImportError:
        _OCR_ERROR = "pytesseract not installed. Run: pip install pytesseract"
        return False
    except Exception as e:
        _OCR_ERROR = f"Tesseract error: {e}"
        return False


def extract_with_ocr(filepath: str, dpi: int = 300) -> str:
    """
    Extract text using PyMuPDF to render PDF pages as images + Tesseract OCR.
    Uses Vietnamese (vie) + English (eng) languages.
    """
    import pytesseract
    import fitz

    pytesseract.pytesseract.tesseract_cmd = _TESSERACT_PATH

    results = []
    with fitz.open(filepath) as doc:
        for page_num, page in enumerate(doc, 1):
            # Render page at high DPI for better OCR quality
            mat = fitz.Matrix(dpi / 72.0, dpi / 72.0)
            pix = page.get_pixmap(matrix=mat)
            img_data = pix.tobytes("png")

            try:
                img = __import__('PIL').Image.open(io.BytesIO(img_data))
            except Exception:
                # PIL not available — use raw bytes
                img = None

            if img is not None:
                # Preprocess: convert to grayscale for better OCR accuracy
                try:
                    import PIL.Image
                    img = img.convert('L')  # Grayscale
                    # Increase contrast slightly for math-heavy documents
                    from PIL import ImageEnhance
                    enhancer = ImageEnhance.Contrast(img)
                    img = enhancer.enhance(1.2)
                except Exception:
                    pass  # Keep original if preprocessing fails

                # OCR with Vietnamese + English
                # PSM 6 = Assume a uniform block of text (best for exam documents)
                # OEM 3 = Neural nets LSTM (best accuracy, requires traineddata with LSTM)
                try:
                    text = pytesseract.image_to_string(
                        img,
                        lang='vie+eng',
                        config='--psm 6 --oem 3'
                    )
                except Exception as e:
                    text = f"[OCR ERROR page {page_num}: {e}]"

                # If OCR returned very little text, try with English only
                if len(text.strip()) < 20:
                    try:
                        text = pytesseract.image_to_string(
                            img,
                            lang='eng',
                            config='--psm 6 --oem 3'
                        )
                    except Exception:
                        pass

                results.append(f"[--- Trang {page_num} (OCR) ---]")
                results.append(text.strip())
            else:
                results.append(f"[--- Trang {page_num} (render failed) ---]")

            results.append("")

    return "\n".join(results)


# ─── Hybrid extraction: text-first, OCR fallback ───────────────────────────────

def extract_hybrid(filepath: str) -> str:
    """
    Try text extraction first, then use OCR if results are poor.
    Strategy:
      1. pdfplumber (best for Vietnamese)
      2. If < 50 chars/page on average → OCR
      3. If still < threshold → fallback to OCR
    """
    # Try text extraction
    text = None
    library_used = None

    def try_lib(name, fn):
        nonlocal text, library_used
        if text is not None:
            return True
        try:
            text = fn(filepath)
            library_used = name
            return True
        except Exception as e:
            return False

    if not try_lib("pdfplumber", extract_with_pdfplumber):
        if not try_lib("pdfminer", extract_with_pdfminer):
            try_lib("pymupdf", extract_with_pymupdf_text)

    if text is None:
        return "", None

    # Analyze quality: if very little text extracted, switch to OCR
    if check_ocr_available():
        lines = [l for l in text.split('\n') if l.strip()]
        avg_chars = sum(len(l) for l in lines) / max(len(lines), 1)
        min_threshold = 30  # chars per non-empty line

        if avg_chars < min_threshold and len(lines) > 0:
            # Poor text extraction — try OCR
            text = extract_with_ocr(filepath)
            library_used = "ocr"

    return text, library_used


# ─── Post-processing ──────────────────────────────────────────────────────────

def post_process_extracted_text(text: str) -> str:
    """
    Post-process extracted text to fix common issues:
      1. Join lines that end with math connectors (+, -, =, etc.)
      2. Reconstruct fraction bars: numerator / bar / denominator → (num)/(den)
      3. Fix common encoding artifacts on Windows
    """
    if not text:
        return text

    # Fix common encoding artifacts
    replacements = [
        ("\ufffd", ""),
        ("\u1eeb", "ệ"), ("\u1ee7", "ỉ"), ("\u1ea7", "ả"),
        ("\u1ea1", "ạ"), ("\u1eb9", "ẹ"), ("\u1ecb", "ị"),
        ("\u1ed9", "ộ"), ("\u1edd", "ờ"), ("\u1ee5", "õ"),
        # OCR artifacts
        ("\n[—–-]{3,}\n", "\n"),  # Remove line separators
        ("[ ]{2,}", " "),           # Collapse multiple spaces
    ]
    for old, new in replacements:
        text = text.replace(old, new)

    lines = text.split('\n')
    result = []
    i = 0
    while i < len(lines):
        line = lines[i].rstrip()

        # Heuristic 1: Fraction bar pattern — numerator / bar / denominator
        if i + 2 < len(lines):
            current = lines[i].strip()
            bar_line = lines[i + 1].strip()
            next_expr = lines[i + 2].strip()
            if is_fraction_pattern(current, bar_line, next_expr):
                line = f"({current})/({next_expr})"
                i += 2

        # Heuristic 2: Line ends with math connector → join with next
        elif i + 1 < len(lines):
            next_line = lines[i + 1].strip()
            math_connector = re.compile(
                r'[+\-–—=<>∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≡≈±×÷·\\]+$'
            )
            if math_connector.search(line) and next_line:
                if is_math_fragment(next_line):
                    line = line + ' ' + next_line
                    i += 1

        # Heuristic 3: Short math token at start → join with previous
        if i > 0 and result:
            prev = result[-1].rstrip()
            if (re.search(r'[+\-–—=<>(∫∑∏√]+$', prev) and is_short_math_token(line)):
                result[-1] = prev + line
                i += 1
                continue

        result.append(line)
        i += 1

    return '\n'.join(result)


def is_math_fragment(text: str) -> bool:
    if not text:
        return False
    if re.match(r'(?i)^(câu|bài|question|đáp\s*án|trang)\s*\d+', text):
        return False
    if re.match(r'^\s*[A-Da-d][\.\):]', text):
        return False
    return len(text) <= 20


def is_short_math_token(text: str) -> bool:
    if not text:
        return False
    return len(text) <= 5 and not text[0].isupper()


def is_fraction_pattern(num: str, bar: str, den: str) -> bool:
    if not num or not bar or not den:
        return False
    if re.match(r'(?i)^(câu|bài|question)\s*\d+', num):
        return False
    if len(num) > 25 or len(den) > 25:
        return False
    if not (re.match(r'[-–—=_~]{2,10}$', bar) or (len(bar) <= 6 and bar.strip() == '')):
        return False
    if re.match(r'(?i)^(đáp\s*án|answer|bảng)', den):
        return False
    return bool(re.search(r'[\dA-Za-z]', num)) and bool(re.search(r'[\dA-Za-z]', den))


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Extract text from PDF using text extraction + OCR fallback"
    )
    parser.add_argument("input", help="Path to input PDF file")
    parser.add_argument("--output", "-o", help="Output file path (default: stdout)")
    parser.add_argument("--library", choices=["pdfplumber", "pdfminer", "pymupdf", "auto"],
                        default="auto", help="PDF library to use (default: auto)")
    parser.add_argument("--ocr", action="store_true",
                        help="Enable OCR for image-based PDFs")
    parser.add_argument("--force-ocr", action="store_true",
                        help="Force OCR on all pages (skip text extraction)")
    parser.add_argument("--dpi", type=int, default=300,
                        help="DPI for OCR rendering (default: 300)")
    args = parser.parse_args()

    filepath = os.path.abspath(args.input)
    if not os.path.isfile(filepath):
        print(f"ERROR: File not found: {filepath}", file=sys.stderr)
        sys.exit(1)

    text = None
    library_used = None

    if args.force_ocr:
        if not check_ocr_available():
            print(f"ERROR: OCR not available: {_OCR_ERROR}", file=sys.stderr)
            sys.exit(2)
        text = extract_with_ocr(filepath, dpi=args.dpi)
        library_used = "ocr"
    elif args.library == "auto":
        text, library_used = extract_hybrid(filepath)
    elif args.library == "ocr":
        if not check_ocr_available():
            print(f"ERROR: OCR not available: {_OCR_ERROR}", file=sys.stderr)
            sys.exit(2)
        text = extract_with_ocr(filepath, dpi=args.dpi)
        library_used = "ocr"
    else:
        extractors = {
            "pdfplumber": extract_with_pdfplumber,
            "pdfminer": extract_with_pdfminer,
            "pymupdf": extract_with_pymupdf_text,
        }
        fn = extractors.get(args.library)
        if fn:
            try:
                text = fn(filepath)
                library_used = args.library
            except Exception as e:
                print(f"ERROR: {args.library} failed: {e}", file=sys.stderr)
                sys.exit(1)

    if text is None:
        print("ERROR: No text extracted from PDF.", file=sys.stderr)
        sys.exit(2)

    # Post-process
    text = post_process_extracted_text(text)

    if not text.strip():
        print("WARNING: No text extracted. File may be empty or OCR failed.", file=sys.stderr)
        sys.exit(2)

    if len(text.strip()) < 50:
        print("WARNING: Very little text extracted. File may be image-based (scanned).", file=sys.stderr)

    # Write output
    try:
        encoded = text.encode('utf-8', errors='replace').decode('utf-8', errors='replace')
    except Exception:
        encoded = text

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(encoded)
        print(f"[Written to {args.output}]", file=sys.stderr)
    else:
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass
        try:
            sys.stdout.buffer.write(text.encode('utf-8', errors='replace'))
        except (AttributeError, TypeError):
            sys.stdout.write(text)

    if library_used:
        print(f"[Extracted with: {library_used}]", file=sys.stderr)

    sys.exit(0)


if __name__ == "__main__":
    main()
