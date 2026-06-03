from __future__ import annotations

import logging
import hmac
import os
import base64
import binascii
import io
import time
import uuid
import asyncio
from contextlib import asynccontextmanager
from functools import lru_cache

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, UnidentifiedImageError
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
from .config import env_bool, load_settings
from .ml_risk import MLRiskEngine
from .model_key_resolver import available_models, has_any_key
from .schemas import (
    BehaviorAnalysisRequest,
    BehaviorAnalysisResponse,
    MLRiskPredictionRequest,
    MLRiskPredictionResponse,
    MLRiskStatusResponse,
    FrameAnalysisRequest,
    FrameAnalysisResponse,
    IdentityRecheckRequest,
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
SETTINGS = load_settings()
PROCTOR_FRAME_RATE_LIMIT = SETTINGS.proctor_frame_rate_limit
WARMUP_ON_STARTUP = SETTINGS.warmup_on_startup
FRAME_ANALYSIS_SEMAPHORE = asyncio.Semaphore(SETTINGS.proctor_frame_max_in_flight)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("AI Service starting up...")
    if not WARMUP_ON_STARTUP:
        logger.info("AI service warmup disabled; heavy analyzers initialize lazily")
    else:
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
    yield


app = FastAPI(title=APP_TITLE, version=APP_VERSION, lifespan=lifespan)
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter


@app.middleware("http")
async def request_log_middleware(request: Request, call_next):
    started_at = time.perf_counter()
    request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
    try:
        response = await call_next(request)
    except Exception:
        latency_ms = int((time.perf_counter() - started_at) * 1000)
        logger.exception(
            "request_failed requestId=%s method=%s path=%s latencyMs=%s",
            request_id,
            request.method,
            request.url.path,
            latency_ms,
        )
        raise
    latency_ms = int((time.perf_counter() - started_at) * 1000)
    response.headers["X-Request-ID"] = request_id
    logger.info(
        "request_complete requestId=%s method=%s path=%s status=%s latencyMs=%s",
        request_id,
        request.method,
        request.url.path,
        response.status_code,
        latency_ms,
    )
    return response


def _cors_allowed_origins() -> list[str]:
    return SETTINGS.cors_allowed_origins


# ─── Rate limit exceeded handler ───────────────────────────────────────────────
@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    retry_after_ms = SETTINGS.proctor_frame_retry_after_ms
    content = {
        "error": "RATE_LIMIT_EXCEEDED",
        "status": "AI_BUSY",
        "message": "Too many requests. Retry later.",
        "retryAfterMs": retry_after_ms,
    }
    if request.url.path == "/proctor/analyze/frame":
        content.update(_busy_frame_response({}, 0, "frame rate limit exceeded"))
    return JSONResponse(
        status_code=429,
        headers={"Retry-After": str(max(1, int(retry_after_ms / 1000)))},
        content=content,
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

@lru_cache(maxsize=1)
def get_ml_risk_engine() -> MLRiskEngine:
    return MLRiskEngine()


# ─── API Key Authentication ────────────────────────────────────────────────────
async def verify_api_key(x_api_key: str = Header(None, alias="X-API-Key")) -> str:
    expected = _expected_api_key()
    if not expected:
        app_env = os.getenv("APP_ENV", os.getenv("ENV", "development")).strip().lower()
        allow_bypass = env_bool("AI_SERVICE_ALLOW_AUTH_BYPASS", app_env not in {"prod", "production"})
        if allow_bypass:
            logger.warning("AI_SERVICE_API_KEY not set; auth bypassed for non-production mode")
            return ""
        raise HTTPException(status_code=401, detail="AI_SERVICE_API_KEY is required")
    if not x_api_key or not hmac.compare_digest(x_api_key, expected):
        raise HTTPException(status_code=401, detail="Invalid or missing X-API-Key header")
    return x_api_key


@lru_cache(maxsize=1)
def _expected_api_key() -> str:
    return os.getenv("AI_SERVICE_API_KEY", "")


# ─── Health ───────────────────────────────────────────────────────────────────
@app.get("/health", response_model=HealthResponse, tags=["health"])
async def health() -> HealthResponse:
    runtime = proctor_runtime_status()
    return HealthResponse(
        status="ok",
        ocr_ready=True,
        cv_ready=runtime["cv_ready"],
        tesseract_ready=True,
    )


def _readiness_payload() -> dict:
    proctor = get_proctor_analyzer()
    face_detector = proctor.face_detector_status()
    services = {
        "ocr": get_ocr_engine().is_ready(),
        "proctor": proctor.is_ready(),
        "proctor_face_detector": face_detector,
        "identity_verifier": True,
        "question_generator": get_question_generator().is_available(),
        "essay_evaluator": get_essay_evaluator().is_available(),
        "performance_predictor": get_performance_predictor().is_available(),
        "quality_analyzer": get_quality_analyzer().is_available(),
        "chat": get_chat_assistant().is_available(),
        "chat_models": available_models(),
        "ml_risk": get_ml_risk_engine().status().model_dump(by_alias=True),
    }
    ready = bool(services["proctor"] and face_detector.get("ready"))
    return {
        "status": "ready" if ready else "degraded",
        "ready": ready,
        "services": services,
        "version": APP_VERSION,
        "settings": {
            "appEnv": SETTINGS.app_env,
            "production": SETTINGS.is_production,
            "apiKeyConfigured": SETTINGS.api_key_configured,
            "corsOriginsConfigured": bool(SETTINGS.cors_allowed_origins),
            "proctorFrameRateLimit": SETTINGS.proctor_frame_rate_limit,
            "proctorFrameMaxInFlight": SETTINGS.proctor_frame_max_in_flight,
            "proctorFrameTimeoutMs": SETTINGS.proctor_frame_timeout_ms,
            "proctorFrameMaxBase64Chars": SETTINGS.proctor_frame_max_base64_chars,
            "proctorFrameMaxDecodedBytes": SETTINGS.proctor_frame_max_decoded_bytes,
            "proctorFrameMaxPixels": SETTINGS.proctor_frame_max_pixels,
            "maxFrameWidth": SETTINGS.max_frame_width,
        },
    }


@app.get("/ready", response_model=None, tags=["health"])
async def ready() -> dict | JSONResponse:
    payload = _readiness_payload()
    if not payload.get("ready"):
        return JSONResponse(status_code=503, content=payload)
    return payload


@app.get("/health/detailed", tags=["health"])
async def health_detailed(_api_key: str = Depends(verify_api_key)) -> dict:
    return _readiness_payload()


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
) -> FrameAnalysisResponse | JSONResponse:
    started_at = time.perf_counter()
    metadata = dict(req.metadata or {})
    request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
    raw_image = req.image_base64 or ""
    validation_error = _validate_frame_base64(raw_image)
    if validation_error:
        result = _invalid_frame_response(validation_error, metadata, len(raw_image))
        result["processingTimeMs"] = int((time.perf_counter() - started_at) * 1000)
        logger.info(
            "ai_frame requestId=%s status=INVALID_FRAME latencyMs=%s payloadChars=%s reason=%s",
            request_id,
            result["processingTimeMs"],
            len(raw_image),
            validation_error,
        )
        return FrameAnalysisResponse.model_validate(result)
    acquired = False
    try:
        try:
            await asyncio.wait_for(FRAME_ANALYSIS_SEMAPHORE.acquire(), timeout=0.001)
            acquired = True
        except asyncio.TimeoutError:
            result = _busy_frame_response(metadata, len(raw_image), "max in-flight frame analyses reached")
            result["processingTimeMs"] = int((time.perf_counter() - started_at) * 1000)
            logger.info(
                "ai_frame requestId=%s status=AI_BUSY latencyMs=%s payloadChars=%s reason=max_in_flight",
                request_id,
                result["processingTimeMs"],
                len(raw_image),
            )
            return JSONResponse(status_code=503, content=result)
        if req.attempt_id is not None:
            metadata.setdefault("attempt_id", req.attempt_id)
            metadata.setdefault("attemptId", req.attempt_id)
        if req.student_id is not None:
            metadata.setdefault("student_id", req.student_id)
            metadata.setdefault("studentId", req.student_id)
        if req.captured_at:
            metadata.setdefault("captured_at", req.captured_at)
            metadata.setdefault("capturedAt", req.captured_at)
        result = await asyncio.wait_for(
            run_in_threadpool(
                get_proctor_analyzer().analyze_frame,
                req.image_base64,
                metadata,
            ),
            timeout=max(0.1, SETTINGS.proctor_frame_timeout_ms / 1000),
        )
        processing_time_ms = int((time.perf_counter() - started_at) * 1000)
        result["processingTimeMs"] = processing_time_ms
        detector = result.get("diagnostics", {}).get("face_detector", {})
        result.setdefault("diagnostics", {})
        result["diagnostics"]["detectorBackend"] = detector.get("backend") or result["diagnostics"].get("detection_method")
        logger.info(
            "ai_frame requestId=%s attemptId=%s frameId=%s status=%s latencyMs=%s payloadChars=%s detectorBackend=%s signalCount=%s",
            request_id,
            metadata.get("attemptId") or metadata.get("attempt_id"),
            metadata.get("frameId") or metadata.get("frame_id"),
            result.get("status"),
            processing_time_ms,
            len(raw_image),
            detector.get("backend"),
            len(result.get("signals") or []),
        )
        return FrameAnalysisResponse.model_validate(result)
    except asyncio.TimeoutError:
        result = _busy_frame_response(metadata, len(raw_image), "frame analysis timed out")
        result["processingTimeMs"] = int((time.perf_counter() - started_at) * 1000)
        logger.warning(
            "ai_frame requestId=%s status=AI_BUSY latencyMs=%s payloadChars=%s reason=timeout",
            request_id,
            result["processingTimeMs"],
            len(raw_image),
        )
        return JSONResponse(status_code=503, content=result)
    except Exception as exc:
        logger.error("Frame analysis failed", exc_info=True)
        raise HTTPException(status_code=400, detail="Frame analysis failed") from exc
    finally:
        if acquired:
            FRAME_ANALYSIS_SEMAPHORE.release()


def _validate_frame_base64(image_base64: str) -> str | None:
    if not image_base64 or not image_base64.strip():
        return "empty image payload"
    if SETTINGS.proctor_frame_max_base64_chars > 0 and len(image_base64) > SETTINGS.proctor_frame_max_base64_chars:
        return "image payload exceeds configured limit"
    raw = image_base64.split(",", 1)[1] if "," in image_base64 else image_base64
    try:
        image_bytes = base64.b64decode(raw, validate=True)
    except (binascii.Error, ValueError):
        return "image payload is not valid base64"
    if SETTINGS.proctor_frame_max_decoded_bytes > 0 and len(image_bytes) > SETTINGS.proctor_frame_max_decoded_bytes:
        return "image bytes exceed configured limit"
    try:
        with Image.open(io.BytesIO(image_bytes)) as image:
            width, height = image.size
            if SETTINGS.proctor_frame_max_pixels > 0 and width * height > SETTINGS.proctor_frame_max_pixels:
                return "image pixel count exceeds configured limit"
            image.verify()
    except (UnidentifiedImageError, OSError, ValueError):
        return "image payload is not a valid image"
    return None


def _invalid_frame_response(reason: str, metadata: dict, payload_chars: int) -> dict:
    return {
        "status": "INVALID_FRAME",
        "face_count": 0,
        "eye_count": 0,
        "face_detected": False,
        "multiple_faces": False,
        "face_quality": "INVALID_FRAME",
        "frame_quality": "INVALID_FRAME",
        "average_brightness": 0.0,
        "signals": [],
        "warnings": [reason],
        "eye_valid": False,
        "eye_state": "UNKNOWN",
        "eye_aspect_ratio": 0.0,
        "blink_rate": 0,
        "eye_tracking_confidence": 0.0,
        "closure_duration_ms": 0,
        "gaze_valid": False,
        "gaze_direction": "UNKNOWN",
        "gaze_off_screen": False,
        "gaze_confidence": 0.0,
        "off_screen_duration_ms": 0,
        "attention_score": 0.0,
        "deepfake_valid": False,
        "deepfake_score": 0.0,
        "liveness_score": 0.0,
        "spoofing_label": "INVALID_FRAME",
        "identity_confidence": 0.0,
        "visual_overlay": {
            "status": "INVALID_FRAME",
            "label": "Invalid frame",
            "tone": "warning",
            "imageWidth": 0,
            "imageHeight": 0,
            "faceBoxes": [],
            "eyeBoxes": [],
            "pupilPoints": [],
        },
        "diagnostics": {
            "reason": reason,
            "payloadChars": payload_chars,
            "degradedReason": reason,
            "detectorBackend": "NOT_RUN",
            "metadata": metadata or {},
        },
    }


def _busy_frame_response(metadata: dict, payload_chars: int, reason: str) -> dict:
    response = _invalid_frame_response(reason, metadata, payload_chars)
    response["status"] = "AI_BUSY"
    response["face_quality"] = "AI_BUSY"
    response["frame_quality"] = "AI_BUSY"
    response["spoofing_label"] = "AI_BUSY"
    response["warnings"] = []
    response["message"] = "AI frame analysis is busy. Retry later."
    response["retryAfterMs"] = SETTINGS.proctor_frame_retry_after_ms
    response["visual_overlay"]["status"] = "AI_BUSY"
    response["visual_overlay"]["label"] = "AI busy"
    response["diagnostics"]["degradedReason"] = reason
    response["diagnostics"]["maxInFlight"] = SETTINGS.proctor_frame_max_in_flight
    response["diagnostics"]["timeoutMs"] = SETTINGS.proctor_frame_timeout_ms
    return response


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


@app.get("/ml/risk/status", response_model=MLRiskStatusResponse, response_model_by_alias=True, tags=["ml-risk"])
@app.get("/ml-risk/status", response_model=MLRiskStatusResponse, response_model_by_alias=True, tags=["ml-risk"])
@limiter.limit("120/minute")
async def ml_risk_status(
    request: Request,
    _api_key: str = Depends(verify_api_key),
) -> MLRiskStatusResponse:
    return get_ml_risk_engine().status()


@app.post("/ml/risk/predict", response_model=MLRiskPredictionResponse, response_model_by_alias=True, tags=["ml-risk"])
@app.post("/ml-risk/predict", response_model=MLRiskPredictionResponse, response_model_by_alias=True, tags=["ml-risk"])
@limiter.limit("120/minute")
async def predict_ml_risk(
    request: Request,
    req: MLRiskPredictionRequest,
    _api_key: str = Depends(verify_api_key),
) -> MLRiskPredictionResponse:
    try:
        return await run_in_threadpool(get_ml_risk_engine().predict, req)
    except Exception as exc:
        logger.error("ML risk prediction failed", exc_info=True)
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
@app.post("/identity/recheck", response_model=IdentityVerifyResponse, tags=["identity"])
@limiter.limit("120/minute")
async def recheck_identity(
    request: Request,
    req: IdentityRecheckRequest,
    _api_key: str = Depends(verify_api_key),
) -> IdentityVerifyResponse:
    try:
        result = await run_in_threadpool(
            get_identity_verifier().recheck,
            image_base64=req.image_base64,
            reference_face_base64=req.reference_face_base64,
            metadata={
                **(req.metadata or {}),
                "attemptId": req.attempt_id,
                "studentId": req.student_id,
                "capturedAt": req.captured_at,
            },
        )
        return IdentityVerifyResponse.model_validate(result)
    except Exception as exc:
        logger.error("Identity recheck failed", exc_info=True)
        raise HTTPException(status_code=400, detail=str(exc)) from exc


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
