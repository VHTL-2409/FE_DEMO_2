"""
main.py — FastAPI service for exam PDF parsing.

Endpoints:
  - GET  /health            — health check
  - POST /parse-exam        — upload PDF and parse (multipart/form-data)
  - POST /parse-exam/file   — parse from a local file path (JSON body)
"""

from __future__ import annotations

import os
import shutil
import tempfile
import uuid
from functools import lru_cache
from pathlib import Path

import fitz
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel

from .config import env_int
from .parser_router import ParserRouter
from .profiler import build_pdf_profile
from .schemas import ParseResponse
from .utils.pdf_reader import PdfReader

# ─── App setup ────────────────────────────────────────────────────────────────

app = FastAPI(
    title="Exam Parser API",
    description="Template-based PDF exam parser with OCR fallback",
    version="1.0.0",
)

# Temp directory for uploaded files
TEMP_DIR = tempfile.gettempdir()
OUTPUT_DIR = os.path.join(TEMP_DIR, "exam_parser_output")
os.makedirs(OUTPUT_DIR, exist_ok=True)
IMAGE_EXTENSIONS = frozenset({".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tif", ".tiff"})
SUPPORTED_EXTENSIONS = frozenset({".pdf", ".docx", *IMAGE_EXTENSIONS})


DEFAULT_CROP_DPI = env_int("PARSER_CROP_DPI", 220)


# ─── Models ──────────────────────────────────────────────────────────────────

class FilePathRequest(BaseModel):
    """Request body for /parse-exam/file endpoint."""
    filePath: str
    sessionId: str | None = None


class HealthResponse(BaseModel):
    status: str
    version: str
    parser_count: int


# ─── Helpers ─────────────────────────────────────────────────────────────────

def get_session_id(existing: str | None) -> str:
    return existing or str(uuid.uuid4())


@lru_cache(maxsize=1)
def get_parser_router() -> ParserRouter:
    return ParserRouter()


def save_upload(file: UploadFile) -> str:
    """Save uploaded PDF to temp file, return path."""
    suffix = Path(file.filename or ".pdf").suffix or ".pdf"
    fd, path = tempfile.mkstemp(suffix=suffix, dir=TEMP_DIR)
    os.close(fd)
    with open(path, "wb") as f:
        shutil.copyfileobj(file.file, f, length=1024 * 1024)
    return path


def _is_supported_image(path: str) -> bool:
    return Path(path).suffix.lower() in IMAGE_EXTENSIONS


def _validate_supported_extension(suffix: str) -> None:
    if suffix.lower() not in SUPPORTED_EXTENSIONS:
        raise HTTPException(400, "Only PDF, DOCX, and image files are supported")


def _convert_image_to_pdf(image_path: str) -> str:
    fd, pdf_path = tempfile.mkstemp(suffix=".pdf", dir=TEMP_DIR)
    os.close(fd)
    doc = fitz.open()
    try:
        img_doc = fitz.open(image_path)
        try:
            rect = img_doc[0].rect
        finally:
            img_doc.close()
        page = doc.new_page(width=rect.width, height=rect.height)
        page.insert_image(page.rect, filename=image_path)
        doc.save(pdf_path)
    finally:
        doc.close()
    return pdf_path


def _prepare_parse_input(path: str) -> tuple[str, list[str]]:
    cleanup_paths: list[str] = []
    if _is_supported_image(path):
        converted_pdf = _convert_image_to_pdf(path)
        cleanup_paths.append(converted_pdf)
        return converted_pdf, cleanup_paths
    return path, cleanup_paths


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse)
async def health():
    """Health check endpoint."""
    return HealthResponse(
        status="ok",
        version="1.0.0",
        parser_count=5,
    )


@app.post("/parse-exam", response_model=ParseResponse)
async def parse_exam_upload(
    file: UploadFile = File(...),
    sessionId: str | None = None,
):
    """
    Upload a PDF or DOCX file and parse it.

    Parameters:
      - file: PDF or DOCX file (multipart/form-data)
      - sessionId: optional session ID (generated if not provided)
    Returns:
      - ParseResponse with metadata, questions, and report
    """
    sid = get_session_id(sessionId)

    # Validate file type
    filename = file.filename or ""
    suffix = Path(filename).suffix.lower()
    _validate_supported_extension(suffix)

    # Save uploaded file
    uploaded_path = save_upload(file)
    parse_path = uploaded_path
    extra_cleanup: list[str] = []
    try:
        parse_path, extra_cleanup = _prepare_parse_input(uploaded_path)
    except Exception as e:
        try:
            os.unlink(uploaded_path)
        except OSError:
            pass
        raise HTTPException(400, f"Unsupported image conversion failed: {e}")

    print(f"[api] Session {sid}: received {filename}, saved to {uploaded_path}", flush=True)

    try:
        # Route to best parser
        router = get_parser_router()
        response = router.route(
            parse_path,
            session_id=sid,
        )

        # Save output
        session_dir = os.path.join(OUTPUT_DIR, sid)
        os.makedirs(session_dir, exist_ok=True)

        # Save cropped images for image-mode questions
        saved_images = _save_crop_images(parse_path, response.questions, session_dir)

        # Update image paths in response
        for q in response.questions:
            if q.render.mode.value == "image" and q.number in saved_images:
                q.render.imagePath = saved_images[q.number]

        print(f"[api] Session {sid}: parsed {len(response.questions)} questions, "
              f"template={response.report.selectedTemplate.value}, "
              f"confidence={response.report.confidence:.2f}",
              flush=True)

        return response

    except Exception as e:
        print(f"[api] Session {sid}: parse failed: {e}", flush=True)
        raise HTTPException(500, f"Parse failed: {e}")

    finally:
        # Clean up uploaded file
        try:
            os.unlink(uploaded_path)
        except OSError:
            pass
        for path in extra_cleanup:
            try:
                os.unlink(path)
            except OSError:
                pass


@app.post("/parse-exam/file", response_model=ParseResponse)
async def parse_exam_filepath(body: FilePathRequest):
    """
    Parse a PDF from a local file path (for internal use / testing).
    """
    pdf_path = body.filePath
    if not os.path.isfile(pdf_path):
        raise HTTPException(404, f"File not found: {pdf_path}")

    suffix = Path(pdf_path).suffix.lower()
    _validate_supported_extension(suffix)

    sid = get_session_id(body.sessionId)
    parse_path = pdf_path
    extra_cleanup: list[str] = []

    try:
        parse_path, extra_cleanup = _prepare_parse_input(pdf_path)
        router = get_parser_router()
        response = router.route(
            parse_path,
            session_id=sid,
        )

        # Save cropped images
        session_dir = os.path.join(OUTPUT_DIR, sid)
        os.makedirs(session_dir, exist_ok=True)
        saved_images = _save_crop_images(parse_path, response.questions, session_dir)

        for q in response.questions:
            if q.render.mode.value == "image" and q.number in saved_images:
                q.render.imagePath = saved_images[q.number]

        return response

    except Exception as e:
        print(f"[api] Session {sid}: parse failed: {e}", flush=True)
        raise HTTPException(500, f"Parse failed: {e}")
    finally:
        for path in extra_cleanup:
            try:
                os.unlink(path)
            except OSError:
                pass


@app.get("/parse-exam/debug")
async def debug_reconstruct(filePath: str):
    """
    Debug endpoint: inspect the reconstructed rows and question regions
    for a given PDF path without doing full parsing.

    Useful for verifying:
      - Reconstructed rows look correct (no "x ²" corruption)
      - Question boundaries are detected
      - Option markers are found

    Query params:
      - filePath: absolute path to the PDF file

    Returns:
      - row_samples: first 30 reconstructed rows with y-coordinates
      - question_regions: detected question boundaries
      - total_rows / total_questions
    """
    if not os.path.isfile(filePath):
        raise HTTPException(404, f"File not found: {filePath}")

    from .utils.math_text_engine import MathPdfTextEngine

    engine = MathPdfTextEngine(filePath)
    result = engine.extract()

    return {
        "total_pages": result.debug_info.get("total_pages"),
        "total_rows": result.debug_info.get("total_rows"),
        "total_questions": result.debug_info.get("total_questions"),
        "row_samples": [
            {
                "y": r.y_center,
                "token_count": len(r.tokens),
                "text": r.text[:120],
            }
            for r in result.rows[:50]
        ],
        "question_regions": [
            {
                "number": q.number,
                "page": q.page,
                "y0": round(q.y0, 1),
                "y1": round(q.y1, 1),
                "line_count": len(q.raw_lines),
                "first_line": q.raw_lines[0][:80] if q.raw_lines else "",
            }
            for q in result.questions
        ],
    }


@app.get("/parse-exam/profile")
async def profile_pdf(filePath: str):
    """
    Get the PDF profile without parsing.
    Useful for debugging / checking which template will be selected.
    """
    if not os.path.isfile(filePath):
        raise HTTPException(404, f"File not found: {filePath}")

    profile = build_pdf_profile(filePath)
    return {
        "total_pages": profile.total_pages,
        "language": profile.language,
        "has_cau_pattern": profile.has_cau_pattern,
        "has_question_pattern": profile.has_question_pattern,
        "has_answer_section": profile.has_answer_section,
        "has_answer_grid": profile.has_answer_grid,
        "has_solution_section": profile.has_solution_section,
        "has_essay_section": profile.has_essay_section,
        "formula_noise_score": round(profile.formula_noise_score, 4),
        "image_block_count": profile.image_block_count,
        "avg_chars_per_page": round(profile.avg_chars_per_page, 1),
        "likely_template": profile.likely_template,
        "scores": {
            "template_01_math_broken": round(profile.score_template_01, 3),
            "template_02_clean_mcq": round(profile.score_template_02, 3),
            "template_03_math_answer_grid": round(profile.score_template_03, 3),
        },
    }


@app.get("/parse-exam/image/{session_id}/{question_num}")
async def get_question_image(session_id: str, question_num: int):
    """Get the cropped image for a specific question."""
    session_dir = os.path.join(OUTPUT_DIR, session_id)
    image_path = os.path.join(session_dir, f"q_{question_num:03d}.png")

    if not os.path.isfile(image_path):
        raise HTTPException(404, f"Image not found for question {question_num}")

    return FileResponse(image_path, media_type="image/png")


@app.delete("/parse-exam/session/{session_id}")
async def delete_session(session_id: str):
    """Delete a parse session and its cached files."""
    import shutil
    session_dir = os.path.join(OUTPUT_DIR, session_id)
    if os.path.isdir(session_dir):
        shutil.rmtree(session_dir)
    return {"status": "deleted", "session_id": session_id}


# ─── Internal helpers ─────────────────────────────────────────────────────────

def _save_crop_images(
    pdf_path: str,
    questions: list,
    output_dir: str,
    dpi: int = DEFAULT_CROP_DPI,
) -> dict[int, str]:
    """
    Save cropped PNG images for questions that use image render mode.
    Returns {question_num: image_path}.
    """
    saved: dict[int, str] = {}
    os.makedirs(output_dir, exist_ok=True)

    image_questions = [
        q for q in questions
        if q.render.mode.value == "image" and q.render.bbox
    ]
    if not image_questions:
        return saved

    with PdfReader(pdf_path) as reader:
        page_size_cache: dict[int, tuple[float, float]] = {}
        for q in image_questions:
            try:
                if q.page not in page_size_cache:
                    page_size_cache[q.page] = reader.get_page_size(q.page)
                page_width, page_height = page_size_cache[q.page]
                bbox = tuple(q.render.bbox)
                x0 = max(0.0, bbox[0] - 8.0)
                y0 = max(0.0, bbox[1] - 8.0)
                x1 = min(page_width, bbox[2] + 8.0)
                y1 = min(page_height, bbox[3] + 8.0)
                png_bytes = reader.render_page_as_image(q.page, dpi=dpi, clip=(x0, y0, x1, y1))

                filename = f"q_{q.number:03d}.png"
                output_path = os.path.join(output_dir, filename)
                with open(output_path, "wb") as f:
                    f.write(png_bytes)
                saved[q.number] = output_path
            except Exception as e:
                print(f"[api] Failed to crop question {getattr(q, 'number', '?')}: {e}", flush=True)

    return saved


# ─── Entry point ─────────────────────────────────────────────────────────────

def run():
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    run()
