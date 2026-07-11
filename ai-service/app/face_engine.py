"""
Face Engine - Advanced face detection and matching using InsightFace.
Replaces Haar Cascade with deep learning-based detection and embedding similarity.
"""
from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

from .config import env_bool, env_float

logger = logging.getLogger(__name__)

INSIGHTFACE_AVAILABLE = False

try:
    import insightface
    from insightface.app import FaceAnalysis
    from insightface.utils import face_align
    INSIGHTFACE_AVAILABLE = True
    logger.info("InsightFace loaded successfully - using deep learning face analysis")
except ImportError as exc:
    logger.warning(f"InsightFace not available: {exc}. Will use fallback Haar Cascade.")


class FaceEngine:
    """Advanced face detection and matching using InsightFace (Buffalo-L / ArcFace).

    Provides:
    - State-of-the-art face detection with face alignment
    - High-quality 512-dim face embeddings for similarity matching
    - Robust to occlusion, pose variation, and lighting changes
    """

    def __init__(self) -> None:
        self._use_deep = INSIGHTFACE_AVAILABLE and env_bool("AI_SERVICE_USE_INSIGHTFACE", True)
        self._face_analyzer: FaceAnalysis | None = None
        self._embedding_model_name: str | None = None
        self._haar_cascade = None
        self._cosine_threshold = env_float("AI_SERVICE_FACE_MATCH_THRESHOLD", 0.50)
        self._detection_confidence_threshold = env_float("AI_SERVICE_FACE_DETECTION_CONF_THRESHOLD", 0.5)
        self._backend = "UNAVAILABLE"

        self._init_deep_engine()
        self._init_haar_fallback()

    def _init_deep_engine(self) -> None:
        if not self._use_deep or not INSIGHTFACE_AVAILABLE:
            return

        try:
            model_name = os.getenv("AI_SERVICE_INSIGHTFACE_MODEL", "buffalo_l")
            providers = ["CPUExecutionProvider"]

            self._face_analyzer = FaceAnalysis(
                name=model_name,
                providers=providers,
                allowed_modules=None,
            )
            self._face_analyzer.prepare(
                ctx_id=0,
                det_size=(640, 640),
                det_thresh=self._detection_confidence_threshold,
            )
            self._embedding_model_name = model_name
            self._backend = f"INSIGHTFACE_{model_name.upper()}"
            logger.info(f"InsightFace initialized with model '{model_name}'")

        except ImportError as exc:
            logger.warning(f"InsightFace initialization failed: {exc}. Falling back to Haar.")
            self._use_deep = False
            self._face_analyzer = None
        except Exception as exc:
            logger.warning(f"InsightFace prepare failed: {exc}. Falling back to Haar.")
            self._use_deep = False
            self._face_analyzer = None

    def _init_haar_fallback(self) -> None:
        if self._use_deep and self._face_analyzer is not None:
            return
        try:
            import cv2
            if cv2 is not None:
                cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
                self._haar_cascade = cv2.CascadeClassifier(cascade_path)
                self._backend = "HAAR_CASCADE"
                logger.info("Using Haar Cascade fallback for face detection")
        except Exception as exc:
            logger.warning(f"Haar cascade init failed: {exc}")
            self._backend = "UNAVAILABLE"

    @property
    def backend(self) -> str:
        return self._backend

    @property
    def is_deep_learning(self) -> bool:
        return self._use_deep and self._face_analyzer is not None

    def detect_faces(
        self,
        image: Image.Image,
        max_faces: int = 10,
    ) -> dict[str, Any]:
        """Detect faces in an image and return bounding boxes and landmarks."""
        img_np = np.array(image.convert("RGB"))

        if self._use_deep and self._face_analyzer is not None:
            return self._detect_deep(img_np, max_faces)
        else:
            return self._detect_haar(img_np, max_faces)

    def _detect_deep(self, img_np: np.ndarray, max_faces: int) -> dict[str, Any]:
        try:
            faces = self._face_analyzer.get(img_np, max_faces=max_faces)
            boxes = []
            landmarks = []
            for face in faces:
                bbox = face.bbox.astype(int).tolist()
                boxes.append(bbox)
                landmarks.append({
                    "left_eye": face.kps[0].tolist() if len(face.kps) > 0 else None,
                    "right_eye": face.kps[1].tolist() if len(face.kps) > 1 else None,
                    "nose": face.kps[2].tolist() if len(face.kps) > 2 else None,
                    "left_mouth": face.kps[3].tolist() if len(face.kps) > 3 else None,
                    "right_mouth": face.kps[4].tolist() if len(face.kps) > 4 else None,
                })

            return {
                "count": len(faces),
                "boxes": boxes,
                "landmarks": landmarks,
                "confidence": [float(f.det_score) for f in faces],
                "method": self._backend,
                "embedding_dims": 512 if faces and hasattr(faces[0], "embedding") else 0,
            }
        except Exception as exc:
            logger.error(f"Deep face detection error: {exc}")
            return {"count": 0, "boxes": [], "landmarks": [], "confidence": [], "method": "INSIGHTFACE_ERROR", "error": str(exc)}

    def _detect_haar(self, img_np: np.ndarray, max_faces: int) -> dict[str, Any]:
        try:
            import cv2
            gray = cv2.cvtColor(img_np, cv2.COLOR_RGB2GRAY)
            faces = self._haar_cascade.detectMultiScale(
                gray, scaleFactor=1.1, minNeighbors=5, minSize=(40, 40)
            )
            boxes = [[int(x), int(y), int(w), int(h)] for (x, y, w, h) in faces]
            boxes = self._filter_haar_boxes(boxes)

            return {
                "count": len(boxes),
                "boxes": boxes,
                "landmarks": [None] * len(boxes),
                "confidence": [1.0] * len(boxes),
                "method": "HAAR_CASCADE",
            }
        except Exception as exc:
            logger.error(f"Haar face detection error: {exc}")
            return {"count": 0, "boxes": [], "landmarks": [], "confidence": [], "method": "HAAR_ERROR", "error": str(exc)}

    def _filter_haar_boxes(self, boxes: list[list[int]]) -> list[list[int]]:
        if len(boxes) <= 1:
            return boxes
        largest_area = max(w * h for _, _, w, h in boxes)
        if largest_area <= 0:
            return boxes
        return [
            box for box in boxes
            if (box[2] * box[3]) / largest_area >= 0.25
        ] or boxes

    def extract_embedding(self, image: Image.Image, box: list[int] | None = None, landmark: dict | None = None) -> np.ndarray | None:
        """Extract 512-dim face embedding from an image crop or detected face.

        Args:
            image: PIL Image
            box: [x, y, w, h] bounding box (optional, will detect if not provided)
            landmark: dict with eye positions for alignment (optional)

        Returns:
            512-dim embedding vector or None if extraction fails
        """
        if not self.is_deep_learning:
            return None

        img_np = np.array(image.convert("RGB"))

        if box is not None:
            x, y, w, h = [int(v) for v in box]
            face_crop = img_np[max(0, y):y+h, max(0, x):x+w]

            if landmark is not None:
                normed = self._normalize_landmark(landmark, box)
                try:
                    aligned = face_align.norm_crop(face_crop, landmark=normed)
                    embedding = self._face_analyzer.get(aligned)[0].embedding
                    return embedding
                except Exception:
                    pass
            embedding = self._face_analyzer.get(face_crop)[0].embedding
            return embedding
        else:
            faces = self._face_analyzer.get(img_np)
            if faces:
                return faces[0].embedding
            return None

    def _normalize_landmark(self, landmark: dict, box: list[int]) -> np.ndarray:
        x, y, w, h = box
        normed = np.zeros((5, 2), dtype=np.float32)
        if landmark.get("left_eye"):
            normed[0] = [(landmark["left_eye"][0] - x) / w, (landmark["left_eye"][1] - y) / h]
        if landmark.get("right_eye"):
            normed[1] = [(landmark["right_eye"][0] - x) / w, (landmark["right_eye"][1] - y) / h]
        if landmark.get("nose"):
            normed[2] = [(landmark["nose"][0] - x) / w, (landmark["nose"][1] - y) / h]
        if landmark.get("left_mouth"):
            normed[3] = [(landmark["left_mouth"][0] - x) / w, (landmark["left_mouth"][1] - y) / h]
        if landmark.get("right_mouth"):
            normed[4] = [(landmark["right_mouth"][0] - x) / w, (landmark["right_mouth"][1] - y) / h]
        return normed

    def compute_similarity(self, embedding1: np.ndarray, embedding2: np.ndarray) -> float:
        """Compute cosine similarity between two face embeddings."""
        dot = float(np.dot(embedding1, embedding2))
        norm1 = float(np.linalg.norm(embedding1))
        norm2 = float(np.linalg.norm(embedding2))
        if norm1 == 0 or norm2 == 0:
            return 0.0
        return max(0.0, min(1.0, (dot / (norm1 * norm2) + 1.0) / 2.0))

    def match_faces(
        self,
        image1: Image.Image,
        image2: Image.Image,
        detection1: dict[str, Any],
        detection2: dict[str, Any],
    ) -> dict[str, Any]:
        """Match faces between two images using deep embeddings (InsightFace)
        or composite similarity (fallback)."""

        if not self.is_deep_learning:
            return None

        try:
            if detection1.get("count", 0) != 1 or detection2.get("count", 0) != 1:
                return None

            embedding1 = self.extract_embedding(
                image1,
                box=detection1["boxes"][0],
                landmark=detection1.get("landmarks", [None])[0] if detection1.get("landmarks") else None,
            )
            embedding2 = self.extract_embedding(
                image2,
                box=detection2["boxes"][0],
                landmark=detection2.get("landmarks", [None])[0] if detection2.get("landmarks") else None,
            )

            if embedding1 is None or embedding2 is None:
                return None

            confidence = self.compute_similarity(embedding1, embedding2)

            if confidence < self._cosine_threshold:
                return {
                    "available": True,
                    "matched": False,
                    "confidence": round(confidence, 4),
                    "method": "INSIGHTFACE_ARCFACE",
                    "embedding_dims": 512,
                    "cosine_threshold": self._cosine_threshold,
                    "reason": f"Similarity {confidence:.3f} below threshold {self._cosine_threshold}",
                }

            return {
                "available": True,
                "matched": True,
                "confidence": round(confidence, 4),
                "method": "INSIGHTFACE_ARCFACE",
                "embedding_dims": 512,
                "cosine_threshold": self._cosine_threshold,
            }

        except Exception as exc:
            logger.error(f"Face matching error: {exc}")
            return None

    def crop_face(self, image: Image.Image, box: list[int], padding: float = 0.2) -> Image.Image:
        """Crop a face from the image with optional padding."""
        x, y, w, h = [int(v) for v in box]
        pad_x = int(w * padding)
        pad_y = int(h * padding)
        left = max(0, x - pad_x)
        top = max(0, y - pad_y)
        right = min(image.width, x + w + pad_x)
        bottom = min(image.height, y + h + pad_y)
        crop = image.crop((left, top, right, bottom))
        return crop.resize((160, 160), Image.LANCZOS)
