"""核心路由 — Chat / RAG / Embedding（真实 LLM 调用 + Agent/Model 动态注入）"""

import asyncio
import base64
import json
import logging
import os
from logging.handlers import RotatingFileHandler
from typing import Optional

import jwt as pyjwt
from fastapi import APIRouter, Header
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app import settings
from app.internal_client import InternalClient
from app.llm.client import LLMClient
from app.llm.exceptions import LLMException

# ── 文件日志配置（与 Java 后端日志同目录，方便统一查看）──
_LOG_DIR = os.environ.get("LOG_PATH", "enterprise-ai-platform/logs")
os.makedirs(_LOG_DIR, exist_ok=True)
_LOG_FILE = os.path.join(_LOG_DIR, "agent-service.log")

_file_handler = RotatingFileHandler(_LOG_FILE, maxBytes=10 * 1024 * 1024, backupCount=5, encoding="utf-8")
_file_handler.setFormatter(logging.Formatter(
    "%(asctime)s [%(threadName)s] %(levelname)-5s %(name)s - %(message)s",
    datefmt="%H:%M:%S",
))
_file_handler.setLevel(logging.INFO)

_root_logger = logging.getLogger()
_root_logger.addHandler(_file_handler)
_root_logger.setLevel(logging.INFO)

logger = logging.getLogger(__name__)
router = APIRouter()
internal_client = InternalClient()

# jjwt 0.9.1 的 signWith(SignatureAlgorithm, String) 会将字符串当作 Base64 解码后作为 HMAC 密钥
# jjwt 内部使用 DatatypeConverter.parseBase64Binary()，该函数会将输入截断到最近 4 的倍数再解码
# 例如 "abcdefghijklmnopqrstuvwxyz" (26 字符) → 实际只用前 24 字符 → 18 字节密钥
# 不能用补全 padding 的方式，因为那会产生 19 字节密钥，导致签名验证失败
_JWT_SECRET_RAW = settings.jwt_secret
_usable_len = (len(_JWT_SECRET_RAW) // 4) * 4
if _usable_len > 0:
    JWT_SECRET = base64.b64decode(_JWT_SECRET_RAW[:_usable_len])
else:
    JWT_SECRET = _JWT_SECRET_RAW.encode("utf-8")


# ── 请求 / 响应模型 ──

class ChatRequest(BaseModel):
    conversationId: str = Field(default="", description="会话 ID")
    message: str = Field(..., description="用户输入消息")
    agent: dict = Field(default_factory=dict, description="Agent 配置")
    model: dict = Field(default_factory=dict, description="模型配置")


class ChatStreamRequest(BaseModel):
    """流式聊天请求（供前端直接调用）"""
    conversationId: Optional[int] = Field(default=None, description="会话 ID，空则自动创建")
    message: str = Field(..., min_length=1, max_length=4000, description="用户输入消息")
    agentId: Optional[int] = Field(default=None, description="Agent ID")
    modelId: Optional[int] = Field(default=None, description="模型 ID")


class RagRequest(BaseModel):
    query: str = Field(..., description="检索查询")
    documents: list[dict] = Field(default_factory=list, description="文档列表")
    model: dict = Field(default_factory=dict, description="模型配置")


class EmbedRequest(BaseModel):
    text: str = Field(..., description="待向量化文本")


# ── 辅助函数 ──

def _verify_user_jwt(authorization: str) -> dict:
    """验证用户 JWT，返回 payload 或抛出异常"""
    if not authorization:
        raise ValueError("缺少 Authorization 头")

    token = authorization.replace("Bearer ", "")
    if not token or token == authorization:
        raise ValueError("Authorization 格式错误，需要 Bearer token")

    logger.info("JWT 验证 — key len=%d bytes, token[:20]=%s",
                len(JWT_SECRET), token[:20])

    return pyjwt.decode(token, JWT_SECRET, algorithms=["HS512"])


def _auth_error(message: str):
    """生成 SSE 格式的认证错误"""
    async def gen():
        err = {"type": "error", "code": 401, "message": message}
        yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"

    return StreamingResponse(gen(), media_type="text/event-stream")


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


def _build_agent_system_prompt(agent_config: dict, user_message: str = "") -> str:
    """
    构建完整的 Agent System Prompt，包含：
    1. Agent 的系统提示词
    2. 工具定义（名称、描述、参数 Schema）
    3. 知识库上下文（RAG 检索结果）

    agent_config 格式（来自 Spring Boot ConversationConfigDto.AgentInfo）:
    {
        "agentId": 1,
        "agentName": "xxx",
        "agentType": "planner",
        "systemPrompt": "...",
        "modelId": 2,
        "maxIterations": 5,
        "temperature": 0.7,
        "timeoutSeconds": 300,
        "tools": [
            {
                "toolId": 1,
                "toolName": "search_knowledge",
                "displayName": "知识库检索",
                "toolType": "http_api",
                "description": "检索企业知识库获取相关文档内容",
                "inputSchema": "{...}"
            }
        ]
    }
    """
    parts = []

    # 1. Agent 系统提示词
    base_prompt = (agent_config.get("systemPrompt") or "").strip()
    if base_prompt:
        parts.append(base_prompt)

    # 2. 工具定义
    tools = agent_config.get("tools") or []
    if tools:
        tool_lines = ["\n## 可用工具\n"]
        tool_lines.append("你可以使用以下工具来完成任务。调用工具时，请在回复中指明工具名称和参数。\n")
        for i, tool in enumerate(tools, 1):
            tool_name = tool.get("toolName", "")
            tool_display = tool.get("displayName", tool_name)
            tool_desc = tool.get("description", "")
            tool_type = tool.get("toolType", "function")
            input_schema = tool.get("inputSchema", "{}")

            tool_lines.append(f"### {i}. {tool_display} (`{tool_name}`)")
            tool_lines.append(f"- **类型**: {tool_type}")
            tool_lines.append(f"- **描述**: {tool_desc}")
            tool_lines.append(f"- **参数**: {_format_tool_schema(input_schema)}")
            tool_lines.append("")

        parts.append("\n".join(tool_lines))

    # 3. 知识库上下文（RAG）
    # 当 agentType 为 "rag" 或 systemPrompt 包含 "知识库" 时，加载 RAG 上下文
    agent_type = agent_config.get("agentType", "")
    if "rag" in agent_type.lower() or "知识库" in (agent_config.get("agentName") or ""):
        kb_context = _load_knowledge_context(user_message)
        if kb_context:
            parts.append("\n## 知识库参考文档\n")
            parts.append("请基于以下文档内容回答问题：\n")
            parts.append(kb_context)

    return "\n".join(parts)


def _format_tool_schema(schema_str: str) -> str:
    """将工具 JSON Schema 格式化为可读文本"""
    if not schema_str:
        return "无"
    try:
        schema = json.loads(schema_str) if isinstance(schema_str, str) else schema_str
        # 取顶层 properties 展示
        props = schema.get("properties", {})
        if not props:
            return str(schema_str)[:200]
        lines = []
        required_fields = schema.get("required", [])
        for name, prop in props.items():
            prop_type = prop.get("type", "string")
            prop_desc = prop.get("description", "")
            required = "（必填）" if name in required_fields else ""
            lines.append(f"  - `{name}`: {prop_type} {required} — {prop_desc}")
        return "\n".join(lines)
    except (json.JSONDecodeError, TypeError):
        return str(schema_str)[:200]


def _load_knowledge_context(user_message: str) -> str:
    """从 RAG 加载与用户消息相关的知识库上下文"""
    try:
        from app.rag.retriever import get_retriever
        retriever = get_retriever()
        if retriever is None:
            return ""
        docs = retriever.retrieve(user_message, top_k=3)
        if not docs:
            return ""
        contexts = []
        for i, doc in enumerate(docs, 1):
            content = doc.get("content") or doc.get("text") or str(doc)
            source = doc.get("source") or doc.get("metadata", {}).get("source", "")
            if len(content) > 500:
                content = content[:500] + "..."
            contexts.append(f"[文档{i}] {content}")
            if source:
                contexts[-1] += f"\n  (来源: {source})"
        return "\n\n".join(contexts)
    except Exception as e:
        logger.warning("加载知识库上下文失败: %s", e)
        return ""


@router.post("/chat/stream")
async def chat_stream(
    req: ChatStreamRequest,
    authorization: str = Header(default=""),
):
    """
    流式聊天 — 前端直接连接，SSE 输出

    流程：
    1. 验证用户 JWT
    2. 获取内部 JWT，调用 Spring Boot 获取/创建会话配置
    3. 组装 messages + 调用 LLM stream:true
    4. StreamingResponse 输出 SSE 事件
    5. 流结束后异步保存消息到 Spring Boot
    """
    # 1. 验证用户 JWT
    try:
        user_payload = _verify_user_jwt(authorization)
    except ValueError as e:
        return _auth_error(str(e))
    except pyjwt.ExpiredSignatureError:
        return _auth_error("Token 已过期")
    except pyjwt.PyJWTError as e:
        return _auth_error(f"Token 无效: {e}")

    username = user_payload.get("sub", "unknown")
    user_id = user_payload.get("userId")  # TokenService 中新增的 claims
    if not user_id:
        user_id = username  # 兜底：如果没 userId，用 username
    logger.info("用户 %s (userId=%s) 发起流式聊天: conversationId=%s, agentId=%s, modelId=%s",
                username, user_id, req.conversationId, req.agentId, req.modelId)

    # 2. 获取或创建会话配置
    config = None
    history = []
    model_config = {}
    agent_config = {}
    conversation_id = req.conversationId

    try:
        if req.conversationId:
            config = await internal_client.get_conversation(req.conversationId)
        else:
            # 新会话：先创建会话再获取配置
            title = req.message[:20].replace("\n", " ").strip() or "新会话"
            create_body = {
                "userId": user_id,
                "title": title,
                "agentId": req.agentId,
                "modelId": req.modelId,
            }
            config = await internal_client.create_conversation(create_body)
            conversation_id = config.get("conversationId")

        if config:
            history = config.get("history", [])
            agent_config = config.get("agent") or {}
            model_config = config.get("model") or {}
    except Exception as e:
        logger.error("获取/创建会话配置失败: %s", e)
        async def config_error_stream():
            err = {"type": "error", "code": 500, "message": f"会话配置获取失败: {e}"}
            yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"
        return StreamingResponse(config_error_stream(), media_type="text/event-stream")

    # 3. 构建增强 System Prompt（系统提示词 + 工具定义 + 知识库上下文）
    system_prompt = _build_agent_system_prompt(agent_config, req.message)
    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    for h in history:
        messages.append({"role": h.get("role", "user"), "content": h.get("content", "")})
    messages.append({"role": "user", "content": req.message})

    # 4. 创建 LLM 客户端 — 使用 Agent 配置的温度/maxTokens 覆盖模型默认值
    if model_config and model_config.get("provider") and model_config.get("modelName"):
        agent_temperature = agent_config.get("temperature")
        agent_max_tokens = model_config.get("maxTokens", 4096)
        agent_timeout = agent_config.get("timeoutSeconds", 300)

        llm_client = LLMClient(
            provider=model_config["provider"],
            model_name=model_config["modelName"],
            base_url=model_config["baseUrl"],
            api_key=model_config.get("apiKey", ""),
            temperature=agent_temperature if agent_temperature is not None else model_config.get("temperature", 0.7),
            max_tokens=agent_max_tokens,
            timeout=agent_timeout,
        )
        logger.info("LLM 客户端创建: provider=%s, model=%s, temperature=%s",
                    model_config["provider"], model_config["modelName"],
                    agent_temperature if agent_temperature is not None else model_config.get("temperature"))
    else:
        llm_client = None
        logger.warning("无有效模型配置，将使用 Mock 回复")

    # 5. SSE 流式响应
    async def event_stream():
        full_reply = ""

        try:
            if llm_client:
                async for chunk in llm_client.chat_stream(messages):
                    full_reply += chunk
                    token_event = {"type": "token", "content": chunk}
                    yield f"event: token\ndata: {json.dumps(token_event, ensure_ascii=False)}\n\n"
            else:
                # Mock 流式（无模型配置时兜底）
                mock_reply = _mock_reply(req.message)
                for ch in mock_reply:
                    full_reply += ch
                    token_event = {"type": "token", "content": ch}
                    yield f"event: token\ndata: {json.dumps(token_event, ensure_ascii=False)}\n\n"
                    await asyncio.sleep(0.03)

            # 6. 异步保存消息
            if conversation_id:
                try:
                    await internal_client.save_message({
                        "conversationId": conversation_id,
                        "userMessage": req.message,
                        "aiMessage": full_reply,
                    })
                except Exception as e:
                    logger.error("保存消息失败（聊天不受影响）: %s", e)

            done_event = {"type": "done", "content": full_reply}
            yield f"event: done\ndata: {json.dumps(done_event, ensure_ascii=False)}\n\n"

        except LLMException as e:
            logger.error("LLM 流式调用失败: %s", e)
            err = {"type": "error", "code": 500, "message": f"LLM 调用失败: {e}"}
            yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error("流式聊天异常: %s", e)
            err = {"type": "error", "code": 500, "message": str(e)}
            yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


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