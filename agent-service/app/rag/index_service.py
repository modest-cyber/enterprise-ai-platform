"""索引服务 — 编排 document → chunk → embedding → milvus insert 流程"""

import logging
import uuid

from .chunk_service import split_text
from .embedding_service import EmbeddingService
from .milvus_service import MilvusService
from .exceptions import IndexException

logger = logging.getLogger(__name__)


class IndexService:

    def __init__(self):
        self.milvus = MilvusService()
        self.embedding = EmbeddingService()

    def index_document(self, doc_id: int, kb_id: int, content: str, chunk_size: int = 800, chunk_overlap: int = 100) -> dict:
        try:
            chunks = split_text(content, chunk_size=chunk_size, chunk_overlap=chunk_overlap)
            if not chunks:
                raise IndexException(f"文档 {doc_id} 分块结果为空")

            vectors = self.embedding.embed_batch(chunks)

            records = []
            for idx, (chunk, vector) in enumerate(zip(chunks, vectors)):
                records.append({
                    "id": str(uuid.uuid4()),
                    "doc_id": doc_id,
                    "kb_id": kb_id,
                    "chunk_index": idx,
                    "content": chunk,
                    "vector": vector,
                })

            self.milvus.insert(records)

            logger.info("索引完成, doc_id=%d, kb_id=%d, chunk_count=%d", doc_id, kb_id, len(chunks))
            return {"doc_id": doc_id, "chunk_count": len(chunks)}

        except IndexException:
            raise
        except Exception as e:
            raise IndexException(f"索引文档失败, doc_id={doc_id}: {e}") from e

    def delete_document(self, doc_id: int, kb_id: int):
        try:
            self.milvus.delete_by_doc_id(doc_id, kb_id)
            logger.info("文档索引已删除, doc_id=%d, kb_id=%d", doc_id, kb_id)
        except Exception as e:
            raise IndexException(f"删除文档索引失败, doc_id={doc_id}: {e}") from e

    def get_stats(self) -> dict:
        try:
            count = self.milvus.count()
            return {"collection": "ai_knowledge", "total_vectors": count}
        except Exception as e:
            raise IndexException(f"获取索引状态失败: {e}") from e