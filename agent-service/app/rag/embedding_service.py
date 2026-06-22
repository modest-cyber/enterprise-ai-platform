"""Embedding 服务 — 统一封装向量化，支持 sentence-transformers / OpenAI / Ollama"""

import logging
import os
from typing import Optional

logger = logging.getLogger(__name__)


class EmbeddingService:

    def __init__(self):
        self._model: Optional[object] = None
        self._provider: Optional[str] = None

    @property
    def provider(self) -> str:
        if self._provider is None:
            self._provider = os.getenv("EMBEDDING_PROVIDER", "sentence-transformers")
        return self._provider

    def _get_model(self):
        if self._model is not None:
            return self._model

        provider = self.provider

        if provider == "sentence-transformers":
            from sentence_transformers import SentenceTransformer
            model_name = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
            device = os.getenv("EMBEDDING_DEVICE", "cpu")
            self._model = SentenceTransformer(model_name, device=device)
            logger.info("Embedding 模型加载完成, model=%s, device=%s", model_name, device)

        elif provider == "openai":
            self._model = "openai"
            logger.info("Embedding provider=openai, model=%s", os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"))

        elif provider == "ollama":
            self._model = "ollama"
            logger.info("Embedding provider=ollama, base_url=%s, model=%s",
                        os.getenv("OLLAMA_BASE_URL", "http://localhost:11434"),
                        os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text"))

        else:
            raise ValueError(f"不支持的 Embedding Provider: {provider}")

        return self._model

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        provider = self.provider

        if provider == "sentence-transformers":
            return self._embed_st(texts)

        elif provider == "openai":
            return self._embed_openai(texts)

        elif provider == "ollama":
            return self._embed_ollama(texts)

        raise ValueError(f"不支持的 Embedding Provider: {provider}")

    def embed_single(self, text: str) -> list[float]:
        return self.embed_batch([text])[0]

    def _embed_st(self, texts: list[str]) -> list[list[float]]:
        import time
        start = time.time()
        model = self._get_model()
        embeddings = model.encode(texts, normalize_embeddings=True)
        elapsed = time.time() - start
        logger.info("Embedding (ST) 完成, count=%d, dim=%d, 耗时 %.2fs", len(texts), embeddings.shape[1], elapsed)
        return embeddings.tolist()

    def _embed_openai(self, texts: list[str]) -> list[list[float]]:
        import time
        from openai import OpenAI
        start = time.time()
        client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        model = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small")
        resp = client.embeddings.create(input=texts, model=model)
        elapsed = time.time() - start
        embeddings = [d.embedding for d in resp.data]
        logger.info("Embedding (OpenAI) 完成, count=%d, 耗时 %.2fs", len(texts), elapsed)
        return embeddings

    def _embed_ollama(self, texts: list[str]) -> list[list[float]]:
        import time
        import requests
        start = time.time()
        base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        model = os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text")
        embeddings = []
        for text in texts:
            resp = requests.post(
                f"{base_url}/api/embeddings",
                json={"model": model, "prompt": text},
                timeout=120,
            )
            resp.raise_for_status()
            embeddings.append(resp.json()["embedding"])
        elapsed = time.time() - start
        logger.info("Embedding (Ollama) 完成, count=%d, 耗时 %.2fs", len(texts), elapsed)
        return embeddings