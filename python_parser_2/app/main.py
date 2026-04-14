"""FastAPI — generic exam ingestion (default pipeline)."""

from __future__ import annotations

import os
import tempfile
import uuid
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, Query, UploadFile
from pydantic import BaseModel

from .schemas import ParseResponse
from .services.exam_ingestion_service import ExamIngestionService

app = FastAPI(
    title='Generic Exam Ingestion API',
    description='Layout-driven PDF + structured DOCX; generic-first, not template-first.',
    version='1.0.0',
)

TEMP_DIR = tempfile.gettempdir()


class HealthOut(BaseModel):
    status: str
    version: str
    pipeline: str


def _save_upload(upload: UploadFile) -> str:
    suffix = Path(upload.filename or '.bin').suffix.lower() or '.pdf'
    if suffix not in ('.pdf', '.docx'):
        suffix = '.pdf'
    fd, path = tempfile.mkstemp(suffix=suffix, dir=TEMP_DIR)
    os.close(fd)
    with open(path, 'wb') as f:
        f.write(upload.file.read())
    return path


@app.get('/health')
async def health() -> HealthOut:
    return HealthOut(status='ok', version='1.0.0', pipeline='generic_ingestion_v1')


@app.post('/parse-exam', response_model=ParseResponse)
async def parse_exam(
    file: UploadFile = File(...),
    sessionId: str | None = None,
    debugIngestion: bool = Query(False),
) -> ParseResponse:
    _ = sessionId or str(uuid.uuid4())
    name = (file.filename or '').lower()
    if not (name.endswith('.pdf') or name.endswith('.docx')):
        raise HTTPException(400, 'Only PDF and DOCX are supported')

    path = _save_upload(file)
    prev = os.environ.get('EXAM_INGESTION_DEBUG')
    try:
        if debugIngestion:
            os.environ['EXAM_INGESTION_DEBUG'] = '1'
        return ExamIngestionService().parse_file(path)
    except ValueError as e:
        raise HTTPException(400, str(e)) from e
    except Exception as e:
        raise HTTPException(500, f'Parse failed: {e}') from e
    finally:
        if debugIngestion:
            if prev is None:
                os.environ.pop('EXAM_INGESTION_DEBUG', None)
            else:
                os.environ['EXAM_INGESTION_DEBUG'] = prev
        try:
            os.unlink(path)
        except OSError:
            pass


class FilePathBody(BaseModel):
    filePath: str


@app.post('/parse-exam/file', response_model=ParseResponse)
async def parse_exam_file(body: FilePathBody) -> ParseResponse:
    if not os.path.isfile(body.filePath):
        raise HTTPException(404, f'File not found: {body.filePath}')
    try:
        return ExamIngestionService().parse_file(body.filePath)
    except ValueError as e:
        raise HTTPException(400, str(e)) from e
    except Exception as e:
        raise HTTPException(500, f'Parse failed: {e}') from e


def run() -> None:
    import uvicorn

    uvicorn.run('app.main:app', host='0.0.0.0', port=8010, reload=False)


if __name__ == '__main__':
    run()
