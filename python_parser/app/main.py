"""
main.py — FastAPI service for exam PDF parsing.

Endpoints:
  - GET  /health            — health check
  - POST /parse-exam        — upload PDF and parse (multipart/form-data)
  - POST /parse-exam/file   — parse from a local file path (JSON body)
"""

from __future__ import annotations

import os
import uuid
import tempfile
from pathlib import Path

from fastapi import FastAPI, File, UploadFile, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel

from .schemas import ParseRequest, ParseResponse, TemplateType
from .parser_router import ParserRouter
from .profiler import build_pdf_profile

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


# ─── Models ──────────────────────────────────────────────────────────────────

class FilePathRequest(BaseModel):
    """Request body for /parse-exam/file endpoint."""
    filePath: str
    sessionId: str | None = None
    forceTemplate: str | None = None


class HealthResponse(BaseModel):
    status: str
    version: str
    parser_count: int


# ─── Helpers ─────────────────────────────────────────────────────────────────

def get_session_id(existing: str | None) -> str:
    return existing or str(uuid.uuid4())


def save_upload(file: UploadFile) -> str:
    """Save uploaded PDF to temp file, return path."""
    suffix = Path(file.filename or ".pdf").suffix or ".pdf"
    fd, path = tempfile.mkstemp(suffix=suffix, dir=TEMP_DIR)
    os.close(fd)
    with open(path, "wb") as f:
        f.write(file.file.read())
    return path


def force_template_from_str(s: str | None) -> TemplateType | None:
    if s is None:
        return None
    try:
        return TemplateType(s)
    except ValueError:
        return None


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
    forceTemplate: str | None = None,
):
    """
    Upload a PDF or DOCX file and parse it.

    Parameters:
      - file: PDF or DOCX file (multipart/form-data)
      - sessionId: optional session ID (generated if not provided)
      - forceTemplate: force a specific template
          (template_01_math_broken | template_02_clean_mcq | template_03_math_answer_grid |
           template_04_docx_vietnamese | template_05_docx_database)

    Returns:
      - ParseResponse with metadata, questions, and report
    """
    sid = get_session_id(sessionId)

    # Validate file type
    filename = file.filename or ""
    suffix = Path(filename).suffix.lower()
    if suffix not in (".pdf", ".docx"):
        raise HTTPException(400, "Only PDF and DOCX files are supported")

    # Save uploaded file
    pdf_path = save_upload(file)
    print(f"[api] Session {sid}: received {filename}, saved to {pdf_path}", flush=True)

    try:
        # Route to best parser
        router = ParserRouter()
        response = router.route(
            pdf_path,
            session_id=sid,
            force_template=force_template_from_str(forceTemplate),
        )

        # Save output
        session_dir = os.path.join(OUTPUT_DIR, sid)
        os.makedirs(session_dir, exist_ok=True)

        # Save cropped images for image-mode questions
        saved_images = _save_crop_images(pdf_path, response.questions, session_dir)

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
            os.unlink(pdf_path)
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

    sid = get_session_id(body.sessionId)

    try:
        router = ParserRouter()
        response = router.route(
            pdf_path,
            session_id=sid,
            force_template=force_template_from_str(body.forceTemplate),
        )

        # Save cropped images
        session_dir = os.path.join(OUTPUT_DIR, sid)
        os.makedirs(session_dir, exist_ok=True)
        saved_images = _save_crop_images(pdf_path, response.questions, session_dir)

        for q in response.questions:
            if q.render.mode.value == "image" and q.number in saved_images:
                q.render.imagePath = saved_images[q.number]

        return response

    except Exception as e:
        print(f"[api] Session {sid}: parse failed: {e}", flush=True)
        raise HTTPException(500, f"Parse failed: {e}")


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
    dpi: int = 300,
) -> dict[int, str]:
    """
    Save cropped PNG images for questions that use image render mode.
    Returns {question_num: image_path}.
    """
    from .utils.image_cropper import crop_question

    saved: dict[int, str] = {}

    for q in questions:
        if q.render.mode.value != "image":
            continue
        if not q.render.bbox:
            continue

        filename = f"q_{q.number:03d}.png"
        output_path = os.path.join(output_dir, filename)

        result = crop_question(
            pdf_path=pdf_path,
            page_num=q.page,
            bbox=tuple(q.render.bbox),
            output_path=output_path,
            dpi=dpi,
        )

        if result:
            saved[q.number] = result

    return saved


# ─── Entry point ─────────────────────────────────────────────────────────────

def run():
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    run()
