"""统一文档预览 API — 支持 PDF/DOCX/DOC/TXT/MD/XLSX/PPTX"""

import logging
from pathlib import Path

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.loaders import DocumentLoaderFactory

logger = logging.getLogger(__name__)

router = APIRouter()


class PreviewRequest(BaseModel):
    filePath: str = Field(..., description="文件完整路径")
    documentId: int = Field(default=0, description="文档ID (可选)")


class PreviewResponse(BaseModel):
    success: bool
    fileType: str = ""
    fileName: str = ""
    content: str = ""
    metadata: dict = Field(default_factory=dict)


@router.post("/preview", response_model=PreviewResponse)
async def preview_document(req: PreviewRequest):
    try:
        logger.info("[Preview] 预览请求: docId=%d, path=%s", req.documentId, req.filePath)

        path = Path(req.filePath)
        if not path.exists():
            raise HTTPException(status_code=404, detail=f"文件不存在: {req.filePath}")

        suffix = path.suffix.lower().lstrip(".")
        if not DocumentLoaderFactory.is_supported(suffix):
            supported = ", ".join(DocumentLoaderFactory.get_supported_extensions())
            raise HTTPException(status_code=400, detail=f"不支持预览: .{suffix}，支持: {supported}")

        content = DocumentLoaderFactory.load(req.filePath)
        loader = DocumentLoaderFactory.get_loader(req.filePath)

        return PreviewResponse(
            success=True,
            fileType=suffix,
            fileName=path.name,
            content=content,
            metadata={
                "fileType": suffix,
                "fileName": path.name,
                "fileSize": path.stat().st_size,
                "loaderType": loader.__class__.__name__,
            },
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error("[Preview] 预览异常: docId=%d, error=%s", req.documentId, str(e))
        return PreviewResponse(
            success=False,
            fileType="unknown",
            fileName="",
            content=f"预览失败: {str(e)}",
            metadata={"error": str(e)},
        )


@router.get("/formats")
async def supported_formats():
    return {
        "supported": DocumentLoaderFactory.get_supported_extensions(),
        "count": len(DocumentLoaderFactory.get_supported_extensions()),
    }