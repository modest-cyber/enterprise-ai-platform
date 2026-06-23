"""知识库索引 API — 真实文档处理 + 索引"""

import logging
import uuid
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


class ProcessResponse(BaseModel):
    success: bool
    chunkCount: int = 0
    vectorCount: int = 0
    message: str = ""


def _extract_text(file_path: str) -> str:
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"文件不存在: {file_path}")

    suffix = path.suffix.lower()
    logger.info("[RAG] 开始读取文件: %s (格式=%s)", file_path, suffix)

    if suffix in (".txt", ".md"):
        text = path.read_text(encoding="utf-8")
    elif suffix == ".pdf":
        import pdfplumber
        with pdfplumber.open(str(path)) as pdf:
            pages = [page.extract_text() or "" for page in pdf.pages]
            text = "\n".join(pages)
    elif suffix == ".docx":
        from docx import Document
        doc = Document(str(path))
        text = "\n".join(p.text for p in doc.paragraphs)
    else:
        raise ValueError(f"不支持的文件格式: {suffix}")

    logger.info("[RAG] 文本提取完成, 长度=%d", len(text))
    return text


@router.post("/process", response_model=ProcessResponse)
async def process_document(req: ProcessRequest):
    try:
        logger.info("[RAG] 开始处理文档: docId=%d, kbId=%d, path=%s",
                    req.documentId, req.knowledgeBaseId, req.filePath)

        text = _extract_text(req.filePath)

        result = index_service.index_document(
            doc_id=req.documentId,
            kb_id=req.knowledgeBaseId,
            content=text,
        )

        logger.info("[RAG] 处理完成: docId=%d, chunkCount=%d, vectorCount=%d",
                    req.documentId, result["chunk_count"], result["chunk_count"])

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