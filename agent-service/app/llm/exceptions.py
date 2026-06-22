"""LLM 异常"""


class LLMException(Exception):
    """LLM 通用异常"""


class LLMCallError(LLMException):
    """LLM 调用失败"""


class LLMTimeoutError(LLMException):
    """LLM 调用超时"""


class LLMConnectionError(LLMException):
    """LLM 连接失败"""