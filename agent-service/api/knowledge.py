"""知识库索引 API — 使用 DocumentLoaderFactory 统一解析"""

import logging
from pathlib import Path

from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.rag.index_service import IndexService
from app.rag.schemas import IndexRequest, IndexResponse, StatsResponse

logger = logging.getLogger(__name__)

router = APIRouter()
index_service = IndexService()


class ProcessRequest(BaseModel):
    documentId: int = Field(..., description="文档ID")
    filePath: str = Field(..., description="文件完整路径")
    knowledgeBaseId: int = Field(..., description="知识库ID")
    chunkSize: int = Field(default=800, description="切块大小")
    chunkOverlap: int = Field(default=100, description="重叠大小")


class ProcessResponse(BaseModel):
    success: bool
    chunkCount: int = 0
    vectorCount: int = 0
    message: str = ""


def _extract_text(file_path: str) -> str:
    from app.loaders import DocumentLoaderFactory
    return DocumentLoaderFactory.load(file_path)


@router.post("/process", response_model=ProcessResponse)
async def process_document(req: ProcessRequest):
    try:
        logger.info("[RAG] 开始处理文档: docId=%d, kbId=%d, path=%s, chunkSize=%d, chunkOverlap=%d",
                    req.documentId, req.knowledgeBaseId, req.filePath,
                    req.chunkSize, req.chunkOverlap)

        text = _extract_text(req.filePath)
        logger.info("[RAG] 文档文本提取完成: docId=%d, textLength=%d", req.documentId, len(text))

        # 先删除旧向量，保证幂等
        try:
            index_service.delete_document(doc_id=req.documentId, kb_id=req.knowledgeBaseId)
            logger.info("[RAG] 已清理旧向量: docId=%d, kbId=%d", req.documentId, req.knowledgeBaseId)
        except Exception as del_err:
            logger.warning("[RAG] 清理旧向量失败(可能无旧数据): %s", del_err)

        result = index_service.index_document(
            doc_id=req.documentId,
            kb_id=req.knowledgeBaseId,
            content=text,
            chunk_size=req.chunkSize,
            chunk_overlap=req.chunkOverlap,
        )

        logger.info("[RAG] 处理完成: docId=%d, chunkSize=%d, chunkOverlap=%d, chunkCount=%d",
                    req.documentId, req.chunkSize, req.chunkOverlap, result["chunk_count"])

        return ProcessResponse(
            success=True,
            chunkCount=result["chunk_count"],
            vectorCount=result["chunk_count"],
            message="处理完成",
        )

    except Exception as e:
        logger.error("[RAG] 处理失败: docId=%d, error=%s", req.documentId, str(e))
        return ProcessResponse(
            success=False,
            chunkCount=0,
            vectorCount=0,
            message=str(e),
        )


@router.post("/index", response_model=IndexResponse)
async def index_document(req: IndexRequest):
    result = index_service.index_document(
        doc_id=req.doc_id,
        kb_id=req.kb_id,
        content=req.content,
    )
    return IndexResponse(success=True, doc_id=result["doc_id"], chunk_count=result["chunk_count"])


@router.delete("/document/{kb_id}/{doc_id}")
async def delete_document(kb_id: int, doc_id: int):
    index_service.delete_document(doc_id=doc_id, kb_id=kb_id)
    return {"success": True, "doc_id": doc_id, "kb_id": kb_id}


@router.get("/stats", response_model=StatsResponse)
async def get_stats():
    stats = index_service.get_stats()
    return StatsResponse(**stats)


@router.get("/formats")
async def supported_formats():
    from app.loaders import DocumentLoaderFactory
    return {
        "supported": DocumentLoaderFactory.get_supported_extensions(),
        "count": len(DocumentLoaderFactory.get_supported_extensions()),
    }