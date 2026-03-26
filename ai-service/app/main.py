from __future__ import annotations

from fastapi import FastAPI, File, Form, HTTPException, UploadFile

from .ocr import OcrEngine
from .proctor import ProctorAnalyzer
from .schemas import (
    BehaviorAnalysisRequest,
    BehaviorAnalysisResponse,
    FrameAnalysisRequest,
    FrameAnalysisResponse,
    HealthResponse,
    OcrProcessResponse,
)

app = FastAPI(title="FE_DEMO AI Service", version="0.1.0")
ocr_engine = OcrEngine()
proctor_analyzer = ProctorAnalyzer()


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        ocr_ready=ocr_engine.is_ready(),
        cv_ready=proctor_analyzer.cv_ready(),
        tesseract_ready=ocr_engine.is_ready(),
    )


@app.post("/ocr/process", response_model=OcrProcessResponse)
async def process_ocr(
    file: UploadFile = File(...),
    language: str = Form("vie+eng"),
    max_pages: int = Form(5),
) -> OcrProcessResponse:
    file_bytes = await file.read()
    try:
        return OcrProcessResponse.model_validate(
            ocr_engine.process(file=file, file_bytes=file_bytes, language=language, max_pages=max_pages)
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/proctor/analyze/frame", response_model=FrameAnalysisResponse)
def analyze_frame(request: FrameAnalysisRequest) -> FrameAnalysisResponse:
    try:
        return FrameAnalysisResponse.model_validate(
            proctor_analyzer.analyze_frame(request.image_base64, request.metadata)
        )
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/proctor/analyze/behavior", response_model=BehaviorAnalysisResponse)
def analyze_behavior(request: BehaviorAnalysisRequest) -> BehaviorAnalysisResponse:
    try:
        return BehaviorAnalysisResponse.model_validate(
            proctor_analyzer.analyze_behavior(
                paste_length=request.paste_length,
                tab_switch_count=request.tab_switch_count,
                idle_seconds=request.idle_seconds,
                typing_intervals=request.typing_intervals,
                metadata=request.metadata,
            )
        )
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=400, detail=str(exc)) from exc
