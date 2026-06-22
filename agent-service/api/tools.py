"""MCP Tool Protocol API"""

import logging

from fastapi import APIRouter, HTTPException

from app.mcp.connection_manager import MCPConnectionManager
from app.mcp.models import ToolCallRequest, ToolCallResponse, ToolDefinition, ToolInfo
from app.mcp.registry import ToolRegistry
from app.mcp.schema_validator import SchemaValidator
from app.mcp.tool_executor import ToolExecutor

logger = logging.getLogger(__name__)

router = APIRouter()

registry = ToolRegistry()
connection_manager = MCPConnectionManager()
schema_validator = SchemaValidator()
executor = ToolExecutor(
    registry=registry,
    connection_manager=connection_manager,
    schema_validator=schema_validator,
)


def _setup_default_tools():
    search_kb = ToolDefinition(
        name="search_kb",
        description="搜索知识库，根据查询返回相关文档片段",
        server_url="http://localhost:8100",
        input_schema={
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索查询"}
            },
            "required": ["query"],
        },
    )
    summarize_text = ToolDefinition(
        name="summarize_text",
        description="对文本进行摘要",
        server_url="http://localhost:8100",
        input_schema={
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "待摘要文本"}
            },
            "required": ["text"],
        },
    )
    registry.register_tool(search_kb)
    registry.register_tool(summarize_text)
    logger.info("已注册 %d 个默认工具", len(registry.list_tools()))


_setup_default_tools()


@router.post("/call", response_model=ToolCallResponse)
async def call_tool(req: ToolCallRequest):
    logger.info("工具调用请求: tool=%s args=%s", req.tool_name, req.arguments)
    result = await executor.invoke(tool_name=req.tool_name, arguments=req.arguments)
    return result


@router.get("", response_model=list[ToolInfo])
async def list_tools():
    tools = registry.list_tools()
    return [
        ToolInfo(
            name=t.name,
            description=t.description,
            server_url=t.server_url,
            enabled=t.enabled,
            timeout=t.timeout,
            retry_count=t.retry_count,
        )
        for t in tools
    ]