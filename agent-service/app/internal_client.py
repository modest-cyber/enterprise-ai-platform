"""Spring Boot 内部接口客户端 — FastAPI 调用 Spring Boot 获取配置、保存消息"""

import logging
import time

import httpx

from app import settings

logger = logging.getLogger(__name__)


class InternalClient:
    """访问 Spring Boot 内部接口的 HTTP 客户端，带内部 JWT 缓存"""

    def __init__(self):
        self.base_url = settings.spring_boot_url.rstrip("/")
        self.shared_secret = settings.internal_secret
        self.jwt_secret = settings.jwt_secret
        self._token: str | None = None
        self._token_expires_at: float = 0  # Unix timestamp

    async def _ensure_token(self) -> str:
        """确保有效的内部 JWT（过期前 60 秒提前刷新）"""
        now = time.time()
        if self._token and now < self._token_expires_at - 60:
            return self._token

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                f"{self.base_url}/ai/internal/auth/token",
                headers={"X-Internal-Secret": self.shared_secret},
                timeout=10,
            )
            resp.raise_for_status()
            await resp.aread()
            data = resp.json()

        if data.get("code") != 200:
            raise RuntimeError(f"获取内部 Token 失败: {data}")

        token_data = data.get("data", {})
        self._token = token_data.get("token")
        expires_in = token_data.get("expiresIn", 1800)
        self._token_expires_at = now + expires_in

        logger.info("已获取新的内部 JWT，有效期 %s 秒", expires_in)
        return self._token

    async def get_token(self) -> str:
        """获取内部 JWT（公开方法）"""
        return await self._ensure_token()

    async def get_conversation(self, conversation_id: int) -> dict:
        """获取会话配置（Agent + Model + 历史消息）"""
        token = await self._ensure_token()

        async with httpx.AsyncClient() as client:
            resp = await client.get(
                f"{self.base_url}/ai/internal/conversation/{conversation_id}",
                headers={"X-Internal-Token": token},
                timeout=10,
            )
            resp.raise_for_status()
            await resp.aread()
            data = resp.json()

        if data.get("code") != 200:
            raise RuntimeError(f"获取会话配置失败: {data}")

        return data.get("data", {})

    async def create_conversation(self, body: dict) -> dict:
        """创建新会话（conversationId=null 时调用）"""
        token = await self._ensure_token()

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                f"{self.base_url}/ai/internal/conversation",
                json=body,
                headers={"X-Internal-Token": token},
                timeout=10,
            )
            resp.raise_for_status()
            await resp.aread()
            data = resp.json()

        if data.get("code") != 200:
            raise RuntimeError(f"创建会话失败: {data}")

        logger.info("已创建新会话: conversationId=%s", data.get("data", {}).get("conversationId"))
        return data.get("data", {})

    async def get_documents_batch(self, doc_ids: list[int]) -> list[dict]:
        """批量获取文档信息（用于RAG来源展示）"""
        token = await self._ensure_token()
        ids_str = ",".join(str(d) for d in doc_ids)

        async with httpx.AsyncClient() as client:
            resp = await client.get(
                f"{self.base_url}/ai/internal/documents?ids={ids_str}",
                headers={"X-Internal-Token": token},
                timeout=10,
            )
            resp.raise_for_status()
            await resp.aread()
            data = resp.json()

        if data.get("code") != 200:
            raise RuntimeError(f"获取文档信息失败: {data}")

        return data.get("data", [])

    async def save_message(self, body: dict) -> None:
        """保存消息（用户消息 + AI 回复）"""
        token = await self._ensure_token()

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                f"{self.base_url}/ai/internal/message/save",
                json=body,
                headers={"X-Internal-Token": token},
                timeout=10,
            )
            resp.raise_for_status()
            await resp.aread()
            data = resp.json()

        if data.get("code") != 200:
            raise RuntimeError(f"保存消息失败: {data}")

        logger.info("消息已保存: conversationId=%s", body.get("conversationId"))
