"""工具注册表 — 内存注册，可扩展数据库加载"""

import logging
from typing import Optional

from app.mcp.exceptions import ToolNotFoundError, ToolDisabledError
from app.mcp.models import ToolDefinition

logger = logging.getLogger(__name__)


class ToolRegistry:

    def __init__(self):
        self._tools: dict[str, ToolDefinition] = {}

    def register_tool(self, tool: ToolDefinition) -> None:
        self._tools[tool.name] = tool
        logger.info("注册工具: %s (server=%s, enabled=%s)", tool.name, tool.server_url, tool.enabled)

    def unregister_tool(self, name: str) -> None:
        self._tools.pop(name, None)
        logger.info("注销工具: %s", name)

    def get_tool(self, name: str) -> ToolDefinition:
        tool = self._tools.get(name)
        if tool is None:
            raise ToolNotFoundError(f"工具 '{name}' 未注册")
        if not tool.enabled:
            raise ToolDisabledError(f"工具 '{name}' 已禁用")
        return tool

    def list_tools(self) -> list[ToolDefinition]:
        return list(self._tools.values())

    def get_all_names(self) -> list[str]:
        return list(self._tools.keys())

    def is_registered(self, name: str) -> bool:
        return name in self._tools