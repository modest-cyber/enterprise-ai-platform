"""MCP 工具执行器 — Tool Discovery → 校验 → 调用 → 重试"""

import asyncio
import logging
import time
import uuid
from typing import Any

import httpx

from app.mcp.connection_manager import MCPConnectionManager
from app.mcp.exceptions import (
    MCPConnectionError,
    MCPTimeoutError,
    RetryExhaustedError,
    ToolDisabledError,
    ToolExecutionError,
    ToolNotFoundError,
)
from app.mcp.models import MCPJsonRpcRequest, ToolCallResponse, ToolDefinition
from app.mcp.registry import ToolRegistry
from app.mcp.schema_validator import SchemaValidator

logger = logging.getLogger(__name__)


class ToolExecutor:

    def __init__(
        self,
        registry: ToolRegistry,
        connection_manager: MCPConnectionManager,
        schema_validator: SchemaValidator,
    ):
        self._registry = registry
        self._connection_manager = connection_manager
        self._schema_validator = schema_validator

    async def invoke(self, tool_name: str, arguments: dict[str, Any]) -> ToolCallResponse:
        start = time.monotonic()

        try:
            tool = self._registry.get_tool(tool_name)
            if not tool.enabled:
                raise ToolDisabledError(f"工具 '{tool_name}' 已禁用")

            self._validate_arguments(tool, arguments)

            last_error: Exception | None = None
            for attempt in range(tool.retry_count + 1):
                try:
                    result = await self._execute_rpc(tool, arguments)
                    elapsed = time.monotonic() - start
                    logger.info(
                        "工具调用成功: tool=%s, attempt=%d/%d, elapsed=%.3fs",
                        tool_name, attempt + 1, tool.retry_count + 1, elapsed,
                    )
                    return ToolCallResponse(
                        success=True,
                        result=result,
                        execution_time=round(elapsed, 3),
                    )

                except (httpx.TimeoutException, MCPTimeoutError) as e:
                    last_error = e
                    if attempt < tool.retry_count:
                        backoff = min(2 ** attempt, 10)
                        logger.warning(
                            "工具调用超时: tool=%s, attempt=%d/%d, retry_in=%ds",
                            tool_name, attempt + 1, tool.retry_count + 1, backoff,
                        )
                        await asyncio.sleep(backoff)
                    continue

                except (httpx.ConnectError, httpx.RemoteProtocolError, MCPConnectionError) as e:
                    last_error = e
                    if attempt < tool.retry_count:
                        backoff = min(2 ** attempt, 10)
                        logger.warning(
                            "连接失败: tool=%s, attempt=%d/%d, retry_in=%ds",
                            tool_name, attempt + 1, tool.retry_count + 1, backoff,
                        )
                        await asyncio.sleep(backoff)
                    continue

            elapsed = time.monotonic() - start
            raise RetryExhaustedError(
                f"工具 '{tool_name}' 重试 {tool.retry_count} 次后仍失败: {last_error}"
            )

        except Exception as e:
            elapsed = time.monotonic() - start
            error_msg = str(e)
            logger.error(
                "工具调用失败: tool=%s, error=%s, elapsed=%.3fs",
                tool_name, error_msg, elapsed,
            )
            return ToolCallResponse(
                success=False,
                error=error_msg,
                execution_time=round(elapsed, 3),
            )

    def _validate_arguments(self, tool: ToolDefinition, arguments: dict[str, Any]) -> None:
        schema = tool.input_schema
        if not schema:
            return
        self._schema_validator.validate(schema, arguments)

    async def _execute_rpc(self, tool: ToolDefinition, arguments: dict[str, Any]) -> Any:
        request_id = uuid.uuid4().hex

        rpc_request = MCPJsonRpcRequest(
            jsonrpc="2.0",
            method="tools/call",
            params={"name": tool.name, "arguments": arguments},
            id=request_id,
        )

        client = await self._connection_manager.get_client(tool.server_url, tool.timeout)

        try:
            resp = await client.post(
                "/jsonrpc",
                json=rpc_request.model_dump(),
                timeout=tool.timeout,
            )
            resp.raise_for_status()
            body = resp.json()
        except httpx.TimeoutException:
            raise MCPTimeoutError(f"请求超时: {tool.server_url}/jsonrpc (timeout={tool.timeout}s)")
        except httpx.ConnectError as e:
            raise MCPConnectionError(f"连接失败: {tool.server_url}: {e}")
        except httpx.RemoteProtocolError as e:
            raise MCPConnectionError(f"协议错误: {tool.server_url}: {e}")
        except httpx.HTTPStatusError as e:
            raise ToolExecutionError(
                f"HTTP {e.response.status_code}: {tool.server_url}/jsonrpc: {e.response.text}"
            )

        if "error" in body and body["error"] is not None:
            err = body["error"]
            err_msg = err.get("message", str(err)) if isinstance(err, dict) else str(err)
            raise ToolExecutionError(f"JSON-RPC 错误: code={err.get('code', -1)} message={err_msg}")

        return body.get("result")