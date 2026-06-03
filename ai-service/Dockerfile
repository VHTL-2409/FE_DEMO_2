FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1
ENV AI_SERVICE_MAX_FRAME_WIDTH=640
ENV AI_SERVICE_MAX_FRAME_PIXELS=1200000
ENV AI_SERVICE_MAX_FRAME_DECODED_BYTES=2000000
ENV AI_SERVICE_PROCTOR_MAX_IN_FLIGHT=2
ENV AI_SERVICE_PROCTOR_FRAME_TIMEOUT_MS=3500
ENV AI_SERVICE_PROCTOR_RETRY_AFTER_MS=2000
ENV AI_SERVICE_FACE_MODEL_AUTO_DOWNLOAD=false
ENV PROCTOR_FRAME_RATE_LIMIT=180/minute

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        tesseract-ocr tesseract-ocr-vie tesseract-ocr-eng \
        libgl1 libglib2.0-0 \
        curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

EXPOSE 8090

# Healthcheck: curl is installed above so this works
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=20s \
    CMD curl -f http://localhost:8090/ready || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090", "--workers", "1"]
