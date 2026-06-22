"""MCP 连接管理器 — 连接池复用 + 心跳检测"""

import asyncio
import logging
from typing import Optional

import httpx

from app.mcp.exceptions import MCPConnectionError

logger = logging.getLogger(__name__)


class MCPConnectionManager:

    def __init__(self):
        self._pool: dict[str, httpx.AsyncClient] = {}
        self._lock = asyncio.Lock()

    async def get_client(self, server_url: str, timeout: int = 30) -> httpx.AsyncClient:
        async with self._lock:
            client = self._pool.get(server_url)
            if client is not None:
                alive = await self._heartbeat(client)
                if alive:
                    logger.debug("复用连接: %s", server_url)
                    return client
                else:
                    logger.warning("连接已失效，重新创建: %s", server_url)
                    await self._close_client(client)
                    self._pool.pop(server_url, None)

            client = self._create_client(server_url, timeout)
            self._pool[server_url] = client
            logger.info("新建连接: %s (超时=%ds)", server_url, timeout)
            return client

    def _create_client(self, server_url: str, timeout: int) -> httpx.AsyncClient:
        return httpx.AsyncClient(
            base_url=server_url,
            timeout=httpx.Timeout(timeout, connect=5.0),
            limits=httpx.Limits(max_keepalive_connections=10, max_connections=20),
        )

    async def _heartbeat(self, client: httpx.AsyncClient) -> bool:
        try:
            resp = await client.get("/health", timeout=5)
            return resp.status_code < 500
        except Exception:
            return False

    async def _close_client(self, client: httpx.AsyncClient) -> None:
        try:
            await client.aclose()
        except Exception as e:
            logger.warning("关闭连接异常: %s", e)

    async def close_all(self) -> None:
        async with self._lock:
            for url, client in list(self._pool.items()):
                await self._close_client(client)
                logger.info("已关闭连接: %s", url)
            self._pool.clear()

    async def heartbeat_check(self, server_url: str) -> bool:
        client = self._pool.get(server_url)
        if client is None:
            return False
        return await self._heartbeat(client)