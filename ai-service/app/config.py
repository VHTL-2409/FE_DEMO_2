from __future__ import annotations

import os
from dataclasses import dataclass


TRUE_VALUES = {"1", "true", "yes", "on"}


def env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in TRUE_VALUES


def env_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        return float(raw)
    except ValueError:
        return default


def env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        return int(raw)
    except ValueError:
        return default


@dataclass(frozen=True)
class AiServiceSettings:
    app_env: str
    cors_allowed_origins: list[str]
    api_key_configured: bool
    warmup_on_startup: bool
    proctor_frame_rate_limit: str
    proctor_frame_max_base64_chars: int
    proctor_frame_max_decoded_bytes: int
    proctor_frame_max_pixels: int
    proctor_frame_timeout_ms: int
    proctor_frame_max_in_flight: int
    proctor_frame_retry_after_ms: int
    max_frame_width: int

    @property
    def is_production(self) -> bool:
        return self.app_env in {"prod", "production"}


def load_settings() -> AiServiceSettings:
    app_env = os.getenv("APP_ENV", os.getenv("ENV", "development")).strip().lower()
    origins = [
        origin.strip()
        for origin in os.getenv("APP_CORS_ALLOWED_ORIGINS", "").split(",")
        if origin.strip()
    ]
    return AiServiceSettings(
        app_env=app_env,
        cors_allowed_origins=origins,
        api_key_configured=bool(os.getenv("AI_SERVICE_API_KEY", "").strip()),
        warmup_on_startup=env_bool("AI_SERVICE_WARMUP_ON_STARTUP", False),
        proctor_frame_rate_limit=os.getenv("PROCTOR_FRAME_RATE_LIMIT", "180/minute"),
        proctor_frame_max_base64_chars=env_int("AI_SERVICE_MAX_FRAME_BASE64_CHARS", 2_500_000),
        proctor_frame_max_decoded_bytes=env_int("AI_SERVICE_MAX_FRAME_DECODED_BYTES", 2_000_000),
        proctor_frame_max_pixels=env_int("AI_SERVICE_MAX_FRAME_PIXELS", 1_200_000),
        proctor_frame_timeout_ms=env_int("AI_SERVICE_PROCTOR_FRAME_TIMEOUT_MS", 3_500),
        proctor_frame_max_in_flight=max(1, env_int("AI_SERVICE_PROCTOR_MAX_IN_FLIGHT", 2)),
        proctor_frame_retry_after_ms=max(250, env_int("AI_SERVICE_PROCTOR_RETRY_AFTER_MS", 2_000)),
        max_frame_width=env_int("AI_SERVICE_MAX_FRAME_WIDTH", 640),
    )
