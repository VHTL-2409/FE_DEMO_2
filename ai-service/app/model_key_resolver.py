"""API key resolver per model.

Loads model → API key mapping from:
1. ``APP_AI_MODEL_KEYS_FILE``  — path to a JSON file inside the container
   (e.g. /run/secrets/model-keys.json)
2. ``APP_AI_MODEL_KEYS_JSON``  — inline JSON string (dev / one-liner)

Falls back to the shared ``APP_AI_API_KEY`` / ``OPENAI_API_KEY`` when
no per-model key is configured for the requested model.
"""

from __future__ import annotations

import json
import os
from functools import lru_cache

_MODEL_KEYS: dict[str, str] = {}
_DEFAULT_KEY: str | None = None
_BASE_URL: str = ""
_LOADED: bool = False


def _load() -> None:
    global _MODEL_KEYS, _DEFAULT_KEY, _BASE_URL, _LOADED
    if _LOADED:
        return
    _LOADED = True

    _BASE_URL = (os.environ.get("OPENAI_API_BASE_URL") or "").strip().rstrip("/")

    _DEFAULT_KEY = (
        os.environ.get("OPENAI_API_KEY")
        or os.environ.get("APP_AI_API_KEY")
        or ""
    ).strip()

    # 1) File takes priority
    file_path = os.environ.get("APP_AI_MODEL_KEYS_FILE", "").strip()
    if file_path and os.path.isfile(file_path):
        try:
            with open(file_path, encoding="utf-8") as f:
                _MODEL_KEYS = json.load(f)
            return
        except Exception:
            pass  # fall through to inline JSON

    # 2) Inline JSON
    inline = os.environ.get("APP_AI_MODEL_KEYS_JSON", "").strip()
    if inline:
        try:
            _MODEL_KEYS = json.loads(inline)
            return
        except Exception:
            pass  # ignore malformed JSON


def resolve_key_for_model(model: str | None) -> tuple[str, str]:
    """Return (api_key, base_url) for the given model.

    ``api_key`` is the per-model key if configured, otherwise the shared key.
    ``base_url`` is always the proxy base URL (or empty string for OpenAI).
    """
    _load()

    key = ""
    if model and model in _MODEL_KEYS:
        key = _MODEL_KEYS[model]
    if not key and _DEFAULT_KEY:
        key = _DEFAULT_KEY

    return key, _BASE_URL


def available_models() -> list[str]:
    """List of models with explicit keys plus the default model from APP_AI_MODEL."""
    _load()

    models = list(_MODEL_KEYS.keys())

    default = os.environ.get("APP_AI_MODEL", "").strip()
    if default and default not in models:
        models.insert(0, default)

    return sorted(models)


def has_any_key() -> bool:
    """True when at least one API key is configured."""
    _load()
    return bool(_DEFAULT_KEY or _MODEL_KEYS)
