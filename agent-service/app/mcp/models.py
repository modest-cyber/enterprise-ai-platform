"""MCP 数据模型"""

from typing import Any

from pydantic import BaseModel, Field


class ToolDefinition(BaseModel):
    name: str = Field(..., description="工具名称")
    description: str = Field(default="", description="工具描述")
    server_url: str = Field(..., description="MCP 服务器地址")
    input_schema: dict[str, Any] = Field(default_factory=dict, description="输入参数 JSON Schema")
    enabled: bool = Field(default=True, description="是否启用")
    timeout: int = Field(default=30, description="超时时间（秒）")
    retry_count: int = Field(default=3, description="重试次数")


class ToolCallRequest(BaseModel):
    tool_name: str = Field(..., description="工具名称")
    arguments: dict[str, Any] = Field(default_factory=dict, description="调用参数")


class ToolCallResponse(BaseModel):
    success: bool = Field(..., description="是否成功")
    result: Any = Field(default=None, description="执行结果")
    error: str | None = Field(default=None, description="错误信息")
    execution_time: float = Field(default=0.0, description="执行耗时（秒）")


class ValidationResult(BaseModel):
    valid: bool = Field(..., description="校验是否通过")
    errors: list[str] = Field(default_factory=list, description="校验错误列表")


class ToolInfo(BaseModel):
    name: str
    description: str
    server_url: str
    enabled: bool
    timeout: int
    retry_count: int


class MCPJsonRpcRequest(BaseModel):
    jsonrpc: str = Field(default="2.0", description="JSON-RPC 版本")
    method: str = Field(default="tools/call", description="MCP 方法")
    params: dict[str, Any] = Field(..., description="调用参数")
    id: str = Field(..., description="请求 ID")


class MCPJsonRpcResponse(BaseModel):
    jsonrpc: str = Field(default="2.0", description="JSON-RPC 版本")
    id: str = Field(default="", description="请求 ID")
    result: Any = Field(default=None, description="成功结果")
    error: dict[str, Any] | None = Field(default=None, description="错误信息")