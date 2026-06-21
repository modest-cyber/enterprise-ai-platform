import json
import asyncio
from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

router = APIRouter()


class ChatRequest(BaseModel):
    message: str = Field(..., description="用户输入消息")
    sessionId: str = Field(default="default", description="会话 ID")


class ChatResponse(BaseModel):
    content: str
    agent: str
    sessionId: str
    tokenUsage: dict = Field(default_factory=dict)


class RagRequest(BaseModel):
    query: str = Field(..., description="检索查询")
    documents: list[dict] = Field(default_factory=list, description="文档列表")


class EmbedRequest(BaseModel):
    text: str = Field(..., description="待向量化文本")


def mock_llm_reply(message: str) -> str:
    return f"[Mock LLM] 收到您的消息：「{message}」。这是模拟回复，实际部署时将调用真实大模型。"


@router.get("/health")
async def health_check():
    return {"status": "ok"}


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    content = mock_llm_reply(req.message)
    return ChatResponse(
        content=content,
        agent="planner",
        sessionId=req.sessionId,
        tokenUsage={
            "prompt_tokens": len(req.message),
            "completion_tokens": len(content),
            "total_tokens": len(req.message) + len(content),
        },
    )


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    async def generate():
        reply = mock_llm_reply(req.message)
        for i, ch in enumerate(reply):
            delta = {"type": "delta", "index": i, "content": ch}
            yield f"data: {json.dumps(delta, ensure_ascii=False)}\n\n"
            await asyncio.sleep(0.05)
        done = {"type": "done", "content": reply}
        yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")


@router.post("/rag")
async def rag(req: RagRequest):
    doc_texts = "\n\n".join([
        d.get("content", d.get("text", str(d))) for d in req.documents
    ])
    answer = f"[Mock RAG] 基于 {len(req.documents)} 篇文档检索结果，对问题「{req.query}」的回答：\n\n{doc_texts[:500]}"
    return {
        "code": 200,
        "msg": "操作成功",
        "data": {
            "answer": answer,
            "sources": len(req.documents),
            "query": req.query,
        },
    }


@router.post("/embed")
async def embed(req: EmbedRequest):
    import numpy as np
    np.random.seed(hash(req.text) % (2 ** 32 - 1))
    vector = np.random.randn(768).tolist()
    return {
        "code": 200,
        "msg": "操作成功",
        "data": {
            "dimension": 768,
            "vector": vector,
        },
    }