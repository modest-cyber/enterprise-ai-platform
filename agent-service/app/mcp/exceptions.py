"""MCP 异常定义"""


class MCPException(Exception):
    """MCP 通用异常"""


class ToolNotFoundError(MCPException):
    """工具未找到"""


class ToolDisabledError(MCPException):
    """工具已禁用"""


class SchemaValidationError(MCPException):
    """Schema 校验失败"""


class MCPConnectionError(MCPException):
    """MCP 连接异常"""


class MCPTimeoutError(MCPException):
    """MCP 调用超时"""


class RetryExhaustedError(MCPException):
    """重试耗尽"""


class ToolExecutionError(MCPException):
    """工具执行失败"""