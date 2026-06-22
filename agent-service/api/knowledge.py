"""知识库索引 API"""

import logging

from fastapi import APIRouter

from app.rag.index_service import IndexService
from app.rag.schemas import IndexRequest, IndexResponse, StatsResponse

logger = logging.getLogger(__name__)

router = APIRouter()
index_service = IndexService()


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