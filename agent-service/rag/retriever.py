"""检索模块 — 从向量数据库中召回相关文档"""

import chromadb
from chromadb.config import Settings as ChromaSettings
from rag.embedding import EmbeddingService
from app import settings


class Retriever:
    def __init__(self):
        self.client = chromadb.PersistentClient(
            path=settings.chroma_persist_dir,
            settings=ChromaSettings(anonymized_telemetry=False),
        )
        self.embedding_service = EmbeddingService()

    async def search(self, query: str, top_k: int = 5) -> list[str]:
        """检索与 query 最相关的文档"""
        try:
            collection = self.client.get_collection("knowledge_base")
        except Exception:
            return []

        query_vec = self.embedding_service.embed_single(query)
        results = collection.query(
            query_embeddings=[query_vec],
            n_results=top_k,
        )

        if results and results["documents"] and results["documents"][0]:
            return results["documents"][0]

        return []

    async def add_documents(self, texts: list[str], metadatas: list[dict] | None = None):
        """向知识库添加文档"""
        try:
            collection = self.client.get_collection("knowledge_base")
        except Exception:
            collection = self.client.create_collection("knowledge_base")

        embeddings = self.embedding_service.embed(texts)
        ids = [f"doc_{hash(t)}" for t in texts]

        collection.add(
            embeddings=embeddings,
            documents=texts,
            metadatas=metadatas,
            ids=ids,
        )
