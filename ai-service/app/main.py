from __future__ import annotations

import logging
import hmac
import os
from functools import lru_cache

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from starlette.requests import Request
from starlette.responses import JSONResponse

from .ocr import OcrEngine
from .proctor import ProctorAnalyzer, proctor_runtime_status
from .identity import IdentityVerifier
from .generator import QuestionGenerator
from .evaluator import EssayEvaluator
from .analytics import PerformancePredictor, QuestionQualityAnalyzer
from .chat import AiChatAssistant
from .config import env_bool
from .model_key_resolver import available_models, has_any_key
from .schemas import (
    BehaviorAnalysisRequest,
    BehaviorAnalysisResponse,
    FrameAnalysisRequest,
    FrameAnalysisResponse,
    IdentityVerifyRequest,
    IdentityVerifyResponse,
    GenerateQuestionsRequest,
    AiGenerateQuestionsResponse,
    EssayEvaluationRequest,
    AiEssayEvaluationResponse,
    PerformancePredictionRequest,
    PerformancePredictionResponse,
    QuestionQualityRequest,
    QuestionQualityResponse,
    ChatRequest,
    ChatResponse,
    ChatModelsResponse,
    HealthResponse,
    OcrProcessResponse,
)

logger = logging.getLogger(__name__)

APP_TITLE = "FE_DEMO AI Service"
APP_VERSION = "0.2.0"
PROCTOR_FRAME_RATE_LIMIT = os.getenv("PROCTOR_FRAME_RATE_LIMIT", "600/minute")
WARMUP_ON_STARTUP = env_bool("AI_SERVICE_WARMUP_ON_STARTUP", False)

app = FastAPI(title=APP_TITLE, version=APP_VERSION)
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter


def _cors_allowed_origins() -> list[str]:
    raw = os.getenv("APP_CORS_ALLOWED_ORIGINS", "")
    return [origin.strip() for origin in raw.split(",") if origin.strip()]


# ─── Rate limit exceeded handler ───────────────────────────────────────────────
@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    return JSONResponse(
        status_code=429,
        content={"error": "RATE_LIMIT_EXCEEDED", "message": "Quá nhiều yêu cầu. Vui lòng thử lại sau."},
    )


# ─── CORS — read allowed origins from env (never use wildcard with credentials) ─
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_allowed_origins(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Service singletons via cached factory ─────────────────────────────────────
@lru_cache(maxsize=1)
def get_ocr_engine() -> OcrEngine:
    return OcrEngine()

@lru_cache(maxsize=1)
def get_proctor_analyzer() -> ProctorAnalyzer:
    return ProctorAnalyzer()

@lru_cache(maxsize=1)
def get_identity_verifier() -> IdentityVerifier:
    return IdentityVerifier()

@lru_cache(maxsize=1)
def get_question_generator() -> QuestionGenerator:
    return QuestionGenerator()

@lru_cache(maxsize=1)
def get_essay_evaluator() -> EssayEvaluator:
    return EssayEvaluator()

@lru_cache(maxsize=1)
def get_performance_predictor() -> PerformancePredictor:
    return PerformancePredictor()

@lru_cache(maxsize=1)
def get_quality_analyzer() -> QuestionQualityAnalyzer:
    return QuestionQualityAnalyzer()

@lru_cache(maxsize=1)
def get_chat_assistant() -> AiChatAssistant:
    return AiChatAssistant()


# ─── API Key Authentication ────────────────────────────────────────────────────
async def verify_api_key(x_api_key: str = Header(None, alias="X-API-Key")) -> str:
    expected = _expected_api_key()
    if not expected:
        logger.warning("AI_SERVICE_API_KEY not set — auth bypassed")
        return ""
    if not x_api_key or not hmac.compare_digest(x_api_key, expected):
        raise HTTPException(status_code=401, detail="Invalid or missing X-API-Key header")
    return x_api_key


@lru_cache(maxsize=1)
def _expected_api_key() -> str:
    return os.getenv("AI_SERVICE_API_KEY", "")


# ─── Startup / shutdown lifecycle ─────────────────────────────────────────────
@app.on_event("startup")
async def on_startup() -> None:
    logger.info("AI Service starting up...")
    if not WARMUP_ON_STARTUP:
        logger.info("AI service warmup disabled; heavy analyzers initialize lazily")
        return
    for name, factory in [
        ("ocr", get_ocr_engine),
        ("proctor", get_proctor_analyzer),
        ("identity_verifier", get_identity_verifier),
        ("question_generator", get_question_generator),
        ("essay_evaluator", get_essay_evaluator),
        ("chat_assistant", get_chat_assistant),
    ]:
        try:
            svc = factory()
            ready = getattr(svc, "is_ready", getattr(svc, "is_available", lambda: True))
            if not ready():
                logger.warning(f"{name} not ready at startup")
        except Exception as exc:
            logger.error(f"Failed to initialize {name}: {exc}")
    logger.info("AI Service startup complete")


# ─── Health ───────────────────────────────────────────────────────────────────
@app.get("/health", response_model=HealthResponse, tags=["health"])
async def health() -> HealthResponse:
    ocr = get_ocr_engine()
    ocr_ready = ocr.is_ready()
    runtime = proctor_runtime_status()
    return HealthResponse(
        status="ok",
        ocr_ready=ocr_ready,
        cv_ready=runtime["cv_ready"],
        tesseract_ready=ocr_ready,
    )


@app.get("/health/detailed", tags=["health"])
async def health_detailed() -> dict:
    return {
        "status": "ok",
        "services": {
            "ocr": get_ocr_engine().is_ready(),
            "proctor": get_proctor_analyzer().is_ready(),
            "proctor_face_detector": get_proctor_analyzer().face_detector_status(),
            "identity_verifier": True,
            "question_generator": get_question_generator().is_available(),
            "essay_evaluator": get_essay_evaluator().is_available(),
            "performance_predictor": get_performance_predictor().is_available(),
            "quality_analyzer": get_quality_analyzer().is_available(),
            "chat": get_chat_assistant().is_available(),
            "chat_models": available_models(),
        },
        "version": APP_VERSION,
    }


# ─── OCR ──────────────────────────────────────────────────────────────────────
@app.post("/ocr/process", response_model=OcrProcessResponse, tags=["ocr"])
@limiter.limit("20/minute")
async def process_ocr(
    request: Request,
    file: UploadFile = File(...),
    language: str = Form("vie+eng"),
    max_pages: int = Form(5),
    _api_key: str = Depends(verify_api_key),
) -> OcrProcessResponse:
    file_bytes = await file.read()
    if len(file_bytes) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File exceeds 10 MB limit")
    try:
        result = await run_in_threadpool(
            get_ocr_engine().process,
            file=file,
            file_bytes=file_bytes,
            language=language,
            max_pages=max_pages,
        )
        return OcrProcessResponse.model_validate(result)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        logger.error("OCR process failed", exc_info=True)
        raise HTTPException(status_code=400, detail=str(exc)) from exc


# ─── Proctoring ───────────────────────────────────────────────────────────────
@app.post("/proctor/analyze/frame", response_model=FrameAnalysisResponse, tags=["proctor"])
@limiter.limit(PROCTOR_FRAME_RATE_LIMIT)
async def analyze_frame(
    request: Request,
    req: FrameAnalysisRequest,
    _api_key: str = Depends(verify_api_key),
) -> FrameAnalysisResponse:
    try:
        metadata = dict(req.metadata or {})
        if req.attempt_id is not None:
            metadata.setdefault("attempt_id", req.attempt_id)
            metadata.setdefault("attemptId", req.attempt_id)
        if req.student_id is not None:
            metadata.setdefault("student_id", req.student_id)
            metadata.setdefault("studentId", req.student_id)
        if req.captured_at:
            metadata.setdefault("captured_at", req.captured_at)
            metadata.setdefault("capturedAt", req.captured_at)
        result = await run_in_threadpool(
            get_proctor_analyzer().analyze_frame,
            req.image_base64,
            metadata,
        )
        return FrameAnalysisResponse.model_validate(result)
    except Exception as exc:
        logger.error("Frame analysis failed", exc_info=True)
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/proctor/analyze/behavior", response_model=BehaviorAnalysisResponse, tags=["proctor"])
@limiter.limit("60/minute")
async def analyze_behavior(
    request: Request,
    req: BehaviorAnalysisRequest,
    _api_key: str = Depends(verify_api_key),
) -> BehaviorAnalysisResponse:
    try:
        result = await run_in_threadpool(
            get_proctor_analyzer().analyze_behavior,
            paste_length=req.paste_length,
            tab_switch_count=req.tab_switch_count,
            idle_seconds=req.idle_seconds,
            typing_intervals=req.typing_intervals,
            metadata=req.metadata,
        )
        return BehaviorAnalysisResponse.model_validate(result)
    except Exception as exc:
        logger.error("Behavior analysis failed", exc_info=True)
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/identity/verify", response_model=IdentityVerifyResponse, tags=["identity"])
@limiter.limit("30/minute")
async def verify_identity(
    request: Request,
    req: IdentityVerifyRequest,
    _api_key: str = Depends(verify_api_key),
) -> IdentityVerifyResponse:
    try:
        result = await run_in_threadpool(
            get_identity_verifier().verify,
            document_image_base64=req.document_image_base64,
            selfie_image_base64=req.selfie_image_base64,
            expected=req.expected,
            document_type=req.document_type,
            metadata={
                **(req.metadata or {}),
                "attemptId": req.attempt_id,
                "studentId": req.student_id,
                "capturedAt": req.captured_at,
            },
        )
        return IdentityVerifyResponse.model_validate(result)
    except Exception as exc:
        logger.error("Identity verification failed", exc_info=True)
        raise HTTPException(status_code=400, detail=str(exc)) from exc


# ─── AI Generation ─────────────────────────────────────────────────────────────
@app.post("/ai/generate/questions", response_model=AiGenerateQuestionsResponse, tags=["ai"])
@limiter.limit("30/minute")
async def generate_questions(
    request: Request,
    req: GenerateQuestionsRequest,
    _api_key: str = Depends(verify_api_key),
) -> AiGenerateQuestionsResponse:
    if not req.topic and not req.text:
        raise HTTPException(
            status_code=400,
            detail="Either 'topic' or 'text' must be provided"
        )
    try:
        if req.topic:
            result = await run_in_threadpool(
                get_question_generator().generate_from_topic,
                topic=req.topic,
                count=req.count,
                difficulty=req.difficulty,
                language=req.language,
            )
        else:
            result = await run_in_threadpool(
                get_question_generator().generate_from_text,
                text=req.text or "",
                count=req.count,
                difficulty=req.difficulty,
                language=req.language,
            )
        return AiGenerateQuestionsResponse.model_validate(result)
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Question generation failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/evaluate/essay", response_model=AiEssayEvaluationResponse, tags=["ai"])
@limiter.limit("30/minute")
async def evaluate_essay(
    request: Request,
    req: EssayEvaluationRequest,
    _api_key: str = Depends(verify_api_key),
) -> AiEssayEvaluationResponse:
    if not req.question or not req.answer:
        raise HTTPException(
            status_code=400,
            detail="Both 'question' and 'answer' must be provided"
        )
    try:
        result = await run_in_threadpool(
            get_essay_evaluator().evaluate,
            question=req.question,
            answer=req.answer,
            rubric=req.rubric,
            max_score=req.max_score,
            language="vi",
        )
        return AiEssayEvaluationResponse.model_validate(result)
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Essay evaluation failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


# ─── AI Analytics ──────────────────────────────────────────────────────────────
@app.post("/ai/analytics/predict", response_model=PerformancePredictionResponse, tags=["ai"])
@limiter.limit("30/minute")
async def predict_performance(
    request: Request,
    req: PerformancePredictionRequest,
    _api_key: str = Depends(verify_api_key),
) -> PerformancePredictionResponse:
    try:
        result = await run_in_threadpool(
            get_performance_predictor().predict_next_score,
            student_id=req.student_id,
            history=req.history,
            exam_id=req.exam_id,
        )
        return PerformancePredictionResponse.model_validate(result)
    except Exception as exc:
        logger.error("Performance prediction failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/ai/analytics/recommendations/{student_id}", tags=["ai"])
@limiter.limit("30/minute")
async def get_recommendations(
    request: Request,
    student_id: int,
    history: str = "",
    _api_key: str = Depends(verify_api_key),
) -> dict:
    history_list = []
    if history:
        import json
        try:
            history_list = json.loads(history)
        except (json.JSONDecodeError, ValueError) as exc:
            raise HTTPException(status_code=400, detail="Invalid 'history' JSON") from exc
    try:
        return await run_in_threadpool(
            get_performance_predictor().get_study_recommendations,
            student_id=student_id,
            history=history_list,
        )
    except Exception as exc:
        logger.error("Recommendations failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/analytics/question-quality", response_model=QuestionQualityResponse, tags=["ai"])
@limiter.limit("30/minute")
async def analyze_question_quality(
    request: Request,
    req: QuestionQualityRequest,
    _api_key: str = Depends(verify_api_key),
) -> QuestionQualityResponse:
    try:
        result = await run_in_threadpool(
            get_quality_analyzer().analyze_question,
            question_content=req.question_content,
            options=req.options,
            correct_answer=req.correct_answer,
            difficulty=req.difficulty,
        )
        return QuestionQualityResponse.model_validate(result)
    except Exception as exc:
        logger.error("Question quality analysis failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/ai/chat/models", response_model=ChatModelsResponse, tags=["ai"])
async def list_chat_models(_api_key: str = Depends(verify_api_key)) -> ChatModelsResponse:
    return {
        "models": available_models(),
        "available": has_any_key(),
    }


@app.post("/ai/chat", response_model=ChatResponse, tags=["ai"])
@limiter.limit("60/minute")
async def chat_completion(
    request: Request,
    req: ChatRequest,
    _api_key: str = Depends(verify_api_key),
) -> ChatResponse:
    try:
        payload = [m.model_dump() for m in req.messages]
        result = await run_in_threadpool(
            get_chat_assistant().chat,
            messages=payload,
            model=req.model,
        )
        return ChatResponse.model_validate(result)
    except Exception as exc:
        logger.error("Chat completion failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/analytics/difficulty-distribution", tags=["ai"])
@limiter.limit("30/minute")
async def analyze_difficulty_distribution(
    request: Request,
    questions: list[dict],
    _api_key: str = Depends(verify_api_key),
) -> dict:
    try:
        return await run_in_threadpool(
            get_quality_analyzer().analyze_difficulty_distribution,
            questions=questions,
        )
    except Exception as exc:
        logger.error("Difficulty distribution analysis failed", exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc)) from exc


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8090)
