"""向量嵌入模块 — 将文本转换为向量"""

from sentence_transformers import SentenceTransformer


class EmbeddingService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.model = SentenceTransformer(
                "BAAI/bge-small-zh-v1.5",
                device="cpu",
            )
        return cls._instance

    def embed(self, texts: list[str]) -> list[list[float]]:
        """将文本列表转换为向量列表"""
        embeddings = self.model.encode(texts, normalize_embeddings=True)
        return embeddings.tolist()

    def embed_single(self, text: str) -> list[float]:
        """将单条文本转换为向量"""
        return self.embed([text])[0]
