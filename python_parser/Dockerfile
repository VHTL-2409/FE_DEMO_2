# python_parser/Dockerfile
FROM python:3.12-slim

WORKDIR /app

# Install system dependencies for PDF/OCR processing
RUN apt-get update && apt-get install -y --no-install-recommends \
    poppler-utils \
    tesseract-ocr \
    tesseract-ocr-vie \
    tesseract-ocr-eng \
    libpoppler-cpp-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ ./app/

# Create temp directory for uploads
RUN mkdir -p /tmp/exam_parser_output

# Expose port
EXPOSE 8000

# Environment variables
ENV PYTHONUNBUFFERED=1
ENV TEMP_DIR=/tmp

# Run the application
CMD ["python", "-m", "app.main"]
