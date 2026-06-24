"""Forge Agent API 端点 — /api/v1/forge/*

提供 forge-agent 的 HTTP 接口，支持：
- POST /forge/stream — 流式执行（SSE），前端直连
"""

import base64 as _b64
import json
import logging
from typing import Optional

import jwt as pyjwt
from fastapi import APIRouter, Header
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app import settings
from app.forge_client import ForgeClient
from app.forge_config import ForgeConfig

logger = logging.getLogger(__name__)
router = APIRouter()
forge_client = ForgeClient()

# JWT 密钥（与 routes.py 保持一致的 jjwt 兼容逻辑）
_JWT_SECRET_RAW = settings.jwt_secret
_usable_len = (len(_JWT_SECRET_RAW) // 4) * 4
if _usable_len > 0:
    JWT_SECRET = _b64.b64decode(_JWT_SECRET_RAW[:_usable_len])
else:
    JWT_SECRET = _JWT_SECRET_RAW.encode("utf-8")


class ForgeStreamRequest(BaseModel):
    """Forge 流式执行请求"""
    message: str = Field(..., min_length=1, max_length=4000, description="任务描述")
    agentId: Optional[int] = Field(default=None, description="Agent ID")
    modelId: Optional[int] = Field(default=None, description="模型 ID")
    conversationId: Optional[int] = Field(default=None, description="会话 ID")
    repoPath: str = Field(default=".", description="工作目录（仓库路径）")


def _verify_jwt(authorization: str) -> dict:
    """验证用户 JWT，返回 payload"""
    if not authorization:
        raise ValueError("缺少 Authorization 头")
    token = authorization.replace("Bearer ", "")
    if not token or token == authorization:
        raise ValueError("Authorization 格式错误，需要 Bearer token")
    return pyjwt.decode(token, JWT_SECRET, algorithms=["HS512"])


def _sse_auth_error(message: str) -> StreamingResponse:
    """SSE 格式的认证错误响应"""
    async def gen():
        err = {"type": "error", "code": 401, "message": message}
        yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"
    return StreamingResponse(gen(), media_type="text/event-stream")


@router.post("/forge/stream")
async def forge_stream(
    req: ForgeStreamRequest,
    authorization: str = Header(default=""),
):
    """Forge Agent 流式执行 — SSE 事件流

    流程：
    1. 验证用户 JWT
    2. 从内部 API 获取 Agent + Model 配置
    3. 构建 ForgeConfig → ForgeClient.execute_stream()
    4. StreamingResponse 输出 SSE 事件（step / done / error）
    """
    # 1. 验证 JWT
    try:
        user_payload = _verify_jwt(authorization)
    except ValueError as e:
        return _sse_auth_error(str(e))
    except pyjwt.ExpiredSignatureError:
        return _sse_auth_error("Token 已过期")
    except pyjwt.PyJWTError as e:
        return _sse_auth_error(f"Token 无效: {e}")

    username = user_payload.get("sub", "unknown")
    logger.info("用户 %s 发起 Forge 流式执行: %s", username, req.message[:50])

    # 2. 获取 Agent + Model 配置
    agent_config = {}
    model_config = {}

    if req.agentId and req.modelId:
        try:
            from app.internal_client import InternalClient
            internal = InternalClient()
            if req.conversationId:
                config = await internal.get_conversation(req.conversationId)
            else:
                config = await internal.create_conversation({
                    "userId": user_payload.get("userId") or username,
                    "title": req.message[:20].replace("\n", " ").strip() or "Forge任务",
                    "agentId": req.agentId,
                    "modelId": req.modelId,
                })
            agent_config = config.get("agent") or {}
            model_config = config.get("model") or {}
        except Exception as e:
            logger.error("获取 Agent/Model 配置失败: %s", e)

    # 3. 构建 ForgeConfig
    forge_config = ForgeConfig.from_platform_config(
        task=req.message,
        agent_config=agent_config,
        model_config=model_config,
        repo_path=req.repoPath,
    )

    logger.info(
        "Forge 配置: provider=%s, model=%s, maxSteps=%d, repo=%s",
        forge_config.provider, forge_config.model_name,
        forge_config.max_steps, forge_config.repo_path,
    )

    # 4. 流式执行
    async def event_stream():
        try:
            async for sse_event in forge_client.execute_stream(forge_config):
                yield sse_event
        except Exception as e:
            logger.error("Forge 执行异常: %s", e)
            err = {"type": "error", "code": 500, "message": f"Forge 执行失败: {e}"}
            yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
