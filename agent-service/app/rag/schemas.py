"""知识库索引 Pydantic Schema"""

from pydantic import BaseModel, Field


class IndexRequest(BaseModel):
    doc_id: int = Field(..., description="文档ID")
    kb_id: int = Field(..., description="知识库ID")
    content: str = Field(..., description="文档全文内容", min_length=1)


class IndexResponse(BaseModel):
    success: bool = Field(..., description="是否成功")
    doc_id: int = Field(..., description="文档ID")
    chunk_count: int = Field(default=0, description="分块数量")


class DeleteDocumentRequest(BaseModel):
    kb_id: int = Field(..., description="知识库ID")


class StatsResponse(BaseModel):
    collection: str = Field(..., description="Collection名称")
    total_vectors: int = Field(default=0, description="总向量数")