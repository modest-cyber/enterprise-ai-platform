"""核心路由 — Chat / RAG / Embedding（真实 LLM 调用）"""

import asyncio
import json
import logging
from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.llm.client import LLMClient
from app.llm.exceptions import LLMException

logger = logging.getLogger(__name__)
router = APIRouter()


# ── 请求 / 响应模型 ──

class ChatRequest(BaseModel):
    message: str = Field(..., description="用户输入消息")
    sessionId: str = Field(default="default", description="会话 ID")
    provider: str = Field(default="", description="模型 Provider: openai/deepseek/qwen/ollama")
    modelName: str = Field(default="", description="模型名称")
    baseUrl: str = Field(default="", description="API Base URL")
    apiKey: str = Field(default="", description="API Key")
    temperature: float = Field(default=0.7, description="温度参数")
    maxTokens: int = Field(default=4096, description="最大输出 Token 数")


class ChatResponse(BaseModel):
    content: str
    agent: str
    sessionId: str
    tokenUsage: dict = Field(default_factory=dict)


class RagRequest(BaseModel):
    query: str = Field(..., description="检索查询")
    documents: list[dict] = Field(default_factory=list, description="文档列表")
    provider: str = Field(default="", description="模型 Provider")
    modelName: str = Field(default="", description="模型名称")
    baseUrl: str = Field(default="", description="API Base URL")
    apiKey: str = Field(default="", description="API Key")


class EmbedRequest(BaseModel):
    text: str = Field(..., description="待向量化文本")


# ── 路由 ──

@router.get("/health")
async def health_check():
    return {"status": "ok"}


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    if not req.provider or not req.modelName or not req.baseUrl:
        return _mock_chat(req)

    try:
        client = LLMClient(
            provider=req.provider,
            model_name=req.modelName,
            base_url=req.baseUrl,
            api_key=req.apiKey,
            temperature=req.temperature,
            max_tokens=req.maxTokens,
        )
        messages = [{"role": "user", "content": req.message}]
        result = await client.chat(messages)

        return ChatResponse(
            content=result["content"],
            agent=result.get("model", req.modelName),
            sessionId=req.sessionId,
            tokenUsage=result.get("usage", {}),
        )
    except LLMException as e:
        logger.error("LLM 调用失败: %s", e)
        return ChatResponse(
            content=f"LLM 调用失败: {e}",
            agent="error",
            sessionId=req.sessionId,
            tokenUsage={},
        )


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    if not req.provider or not req.modelName or not req.baseUrl:
        return StreamingResponse(_mock_stream(req), media_type="text/event-stream")

    async def generate():
        try:
            client = LLMClient(
                provider=req.provider,
                model_name=req.modelName,
                base_url=req.baseUrl,
                api_key=req.apiKey,
                temperature=req.temperature,
                max_tokens=req.maxTokens,
            )
            messages = [{"role": "user", "content": req.message}]
            reply = await client.chat_str(messages)

            for i, ch in enumerate(reply):
                delta = {"type": "delta", "index": i, "content": ch}
                yield f"data: {json.dumps(delta, ensure_ascii=False)}\n\n"
                await asyncio.sleep(0.02)
            done = {"type": "done", "content": reply}
            yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"
        except LLMException as e:
            logger.error("流式 LLM 调用失败: %s", e)
            err = {"type": "error", "message": str(e)}
            yield f"data: {json.dumps(err, ensure_ascii=False)}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")


@router.post("/rag")
async def rag(req: RagRequest):
    if not req.provider or not req.modelName or not req.baseUrl:
        return _mock_rag(req)

    try:
        client = LLMClient(
            provider=req.provider,
            model_name=req.modelName,
            base_url=req.baseUrl,
            api_key=req.apiKey,
        )
        doc_texts = "\n\n".join([
            d.get("content", d.get("text", str(d))) for d in req.documents
        ])
        messages = [
            {"role": "system", "content": "你是一个知识库问答助手，请根据提供的文档内容回答问题。"},
            {"role": "user", "content": f"参考文档：\n{doc_texts}\n\n问题：{req.query}\n\n请基于上面的文档内容回答。"},
        ]
        result = await client.chat(messages)
        return {
            "code": 200,
            "msg": "操作成功",
            "data": {
                "answer": result["content"],
                "sources": len(req.documents),
                "query": req.query,
            },
        }
    except LLMException as e:
        logger.error("RAG LLM 调用失败: %s", e)
        return {"code": 500, "msg": f"LLM 调用失败: {e}", "data": None}


@router.post("/embed")
async def embed(req: EmbedRequest):
    try:
        from app.rag.embedding_service import EmbeddingService
        service = EmbeddingService()
        vector = service.embed_single(req.text)
        return {
            "code": 200,
            "msg": "操作成功",
            "data": {
                "dimension": len(vector),
                "vector": vector,
            },
        }
    except Exception as e:
        logger.warning("Embedding 失败, 回退 mock: %s", e)
        import numpy as np
        np.random.seed(hash(req.text) % (2 ** 32 - 1))
        vector = np.random.randn(768).tolist()
        return {
            "code": 200,
            "msg": "操作成功 (mock)",
            "data": {"dimension": 768, "vector": vector},
        }


# ── Mock 兜底 ──

def _mock_chat(req: ChatRequest) -> ChatResponse:
    content = f"[Mock LLM] 收到您的消息：「{req.message}」。这是模拟回复，实际部署时将调用真实大模型。"
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


async def _mock_stream(req: ChatRequest):
    reply = _mock_chat(req).content
    for i, ch in enumerate(reply):
        delta = {"type": "delta", "index": i, "content": ch}
        yield f"data: {json.dumps(delta, ensure_ascii=False)}\n\n"
        await asyncio.sleep(0.05)
    done = {"type": "done", "content": reply}
    yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"


def _mock_rag(req: RagRequest) -> dict:
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