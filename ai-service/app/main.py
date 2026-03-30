from __future__ import annotations

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from .ocr import OcrEngine
from .proctor import ProctorAnalyzer
from .generator import QuestionGenerator
from .evaluator import EssayEvaluator
from .analytics import PerformancePredictor, QuestionQualityAnalyzer
from .chat import AiChatAssistant
from .model_key_resolver import available_models, has_any_key
from .schemas import (
    BehaviorAnalysisRequest,
    BehaviorAnalysisResponse,
    FrameAnalysisRequest,
    FrameAnalysisResponse,
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

app = FastAPI(title="FE_DEMO AI Service", version="0.2.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ocr_engine = OcrEngine()
proctor_analyzer = ProctorAnalyzer()
question_generator = QuestionGenerator()
essay_evaluator = EssayEvaluator()
performance_predictor = PerformancePredictor()
quality_analyzer = QuestionQualityAnalyzer()
chat_assistant = AiChatAssistant()


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        ocr_ready=ocr_engine.is_ready(),
        cv_ready=proctor_analyzer.cv_ready(),
        tesseract_ready=ocr_engine.is_ready(),
    )


@app.get("/health/detailed")
def health_detailed() -> dict:
    return {
        "status": "ok",
        "services": {
            "ocr": ocr_engine.is_ready(),
            "proctor": proctor_analyzer.cv_ready(),
            "question_generator": question_generator.is_available(),
            "essay_evaluator": essay_evaluator.is_available(),
            "performance_predictor": performance_predictor.is_available(),
            "quality_analyzer": quality_analyzer.is_available(),
            "chat": chat_assistant.is_available(),
            "chat_models": available_models(),
        },
        "version": "0.2.0",
    }


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


@app.post("/ai/generate/questions", response_model=AiGenerateQuestionsResponse)
def generate_questions(request: GenerateQuestionsRequest) -> AiGenerateQuestionsResponse:
    try:
        if not request.topic and not request.text:
            raise HTTPException(
                status_code=400,
                detail="Either 'topic' or 'text' must be provided"
            )

        if request.topic:
            result = question_generator.generate_from_topic(
                topic=request.topic,
                count=request.count,
                difficulty=request.difficulty,
                language=request.language,
            )
        else:
            result = question_generator.generate_from_text(
                text=request.text or "",
                count=request.count,
                difficulty=request.difficulty,
                language=request.language,
            )

        return AiGenerateQuestionsResponse.model_validate(result)
    except HTTPException:
        raise
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/evaluate/essay", response_model=AiEssayEvaluationResponse)
def evaluate_essay(request: EssayEvaluationRequest) -> AiEssayEvaluationResponse:
    try:
        if not request.question or not request.answer:
            raise HTTPException(
                status_code=400,
                detail="Both 'question' and 'answer' must be provided"
            )

        result = essay_evaluator.evaluate(
            question=request.question,
            answer=request.answer,
            rubric=request.rubric,
            max_score=request.max_score,
            language="vi",
        )

        return AiEssayEvaluationResponse.model_validate(result)
    except HTTPException:
        raise
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/analytics/predict", response_model=PerformancePredictionResponse)
def predict_performance(request: PerformancePredictionRequest) -> PerformancePredictionResponse:
    try:
        result = performance_predictor.predict_next_score(
            student_id=request.student_id,
            history=request.history,
            exam_id=request.exam_id,
        )

        return PerformancePredictionResponse.model_validate(result)
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/ai/analytics/recommendations/{student_id}")
def get_recommendations(student_id: int, history: str = "") -> dict:
    try:
        history_list = []
        if history:
            import json as _json
            history_list = _json.loads(history)

        result = performance_predictor.get_study_recommendations(
            student_id=student_id,
            history=history_list,
        )

        return result
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/analytics/question-quality", response_model=QuestionQualityResponse)
def analyze_question_quality(request: QuestionQualityRequest) -> QuestionQualityResponse:
    try:
        result = quality_analyzer.analyze_question(
            question_content=request.question_content,
            options=request.options,
            correct_answer=request.correct_answer,
            difficulty=request.difficulty,
        )

        return QuestionQualityResponse.model_validate(result)
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/ai/chat/models", response_model=ChatModelsResponse)
def list_chat_models() -> ChatModelsResponse:
    return {
        "models": available_models(),
        "available": has_any_key(),
    }


@app.post("/ai/chat", response_model=ChatResponse)
def chat_completion(request: ChatRequest) -> ChatResponse:
    try:
        payload = [m.model_dump() for m in request.messages]
        result = chat_assistant.chat(messages=payload, model=request.model)
        return ChatResponse.model_validate(result)
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ai/analytics/difficulty-distribution")
def analyze_difficulty_distribution(questions: list[dict]) -> dict:
    try:
        result = quality_analyzer.analyze_difficulty_distribution(questions=questions)
        return result
    except Exception as exc:  # pragma: no cover - runtime safeguard
        raise HTTPException(status_code=500, detail=str(exc)) from exc


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8090)
