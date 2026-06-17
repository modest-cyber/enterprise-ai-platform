"""重排序模块 — 对召回结果进行精确重排"""

from typing import Literal

from rank_bm25 import BM25Okapi


class Reranker:
    def __init__(self, method: Literal["bm25", "cross-encoder"] = "bm25"):
        self.method = method

    async def rerank(
        self, query: str, documents: list[str], top_k: int = 5
    ) -> list[str]:
        """对检索结果重新排序，返回 top_k 最相关的文档"""
        if not documents:
            return []

        if self.method == "bm25":
            return self._bm25_rerank(query, documents, top_k)

        # 预留 cross-encoder 位置
        return documents[:top_k]

    def _bm25_rerank(self, query: str, documents: list[str], top_k: int) -> list[str]:
        tokenized_docs = [doc.split() for doc in documents]
        tokenized_query = query.split()

        bm25 = BM25Okapi(tokenized_docs)
        scores = bm25.get_scores(tokenized_query)

        ranked = sorted(
            zip(documents, scores), key=lambda x: x[1], reverse=True
        )
        return [doc for doc, _ in ranked[:top_k]]
