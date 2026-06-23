"""核心路由 — Chat / RAG / Embedding（真实 LLM 调用 + Agent/Model 动态注入）"""

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
    conversationId: str = Field(default="", description="会话 ID")
    message: str = Field(..., description="用户输入消息")
    agent: dict = Field(default_factory=dict, description="Agent 配置")
    model: dict = Field(default_factory=dict, description="模型配置")


class RagRequest(BaseModel):
    query: str = Field(..., description="检索查询")
    documents: list[dict] = Field(default_factory=list, description="文档列表")
    model: dict = Field(default_factory=dict, description="模型配置")


class EmbedRequest(BaseModel):
    text: str = Field(..., description="待向量化文本")


# ── 路由 ──

@router.get("/health")
async def health_check():
    return {"status": "ok"}


@router.post("/chat")
async def chat(req: ChatRequest):
    model = req.model
    if not model or not model.get("provider") or not model.get("modelName") or not model.get("baseUrl"):
        return {"success": True, "content": _mock_reply(req.message)}

    try:
        client = LLMClient(
            provider=model["provider"],
            model_name=model["modelName"],
            base_url=model["baseUrl"],
            api_key=model.get("apiKey", ""),
            temperature=model.get("temperature", 0.7),
            max_tokens=model.get("maxTokens", 4096),
        )

        messages = _build_messages(req)
        result = await client.chat(messages)

        logger.info(
            "LLM 调用成功: model=%s, tokens=%s",
            result.get("model", "unknown"),
            result.get("usage", {}),
        )
        return {"success": True, "content": result["content"]}

    except LLMException as e:
        logger.error("LLM 调用失败: %s", e)
        return {"success": False, "message": f"LLM 调用失败: {e}"}


def _build_messages(req: ChatRequest) -> list[dict]:
    messages = []
    agent = req.agent or {}
    system_prompt = agent.get("systemPrompt", "")
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": req.message})
    return messages


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    model = req.model
    if not model or not model.get("provider") or not model.get("modelName") or not model.get("baseUrl"):
        return StreamingResponse(_mock_stream(req), media_type="text/event-stream")

    async def generate():
        try:
            client = LLMClient(
                provider=model["provider"],
                model_name=model["modelName"],
                base_url=model["baseUrl"],
                api_key=model.get("apiKey", ""),
                temperature=model.get("temperature", 0.7),
                max_tokens=model.get("maxTokens", 4096),
            )
            messages = _build_messages(req)
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
    model = req.model
    if not model or not model.get("provider") or not model.get("modelName") or not model.get("baseUrl"):
        return _mock_rag_result(req)

    try:
        client = LLMClient(
            provider=model["provider"],
            model_name=model["modelName"],
            base_url=model["baseUrl"],
            api_key=model.get("apiKey", ""),
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

def _mock_reply(message: str) -> str:
    return f"[Mock LLM] 收到您的消息：「{message}」。这是模拟回复，实际部署时将调用真实大模型。"


async def _mock_stream(req: ChatRequest):
    reply = _mock_reply(req.message)
    for i, ch in enumerate(reply):
        delta = {"type": "delta", "index": i, "content": ch}
        yield f"data: {json.dumps(delta, ensure_ascii=False)}\n\n"
        await asyncio.sleep(0.05)
    done = {"type": "done", "content": reply}
    yield f"data: {json.dumps(done, ensure_ascii=False)}\n\n"


def _mock_rag_result(req: RagRequest) -> dict:
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