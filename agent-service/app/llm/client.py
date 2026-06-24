"""LLM Client — 统一调用 OpenAI 兼容 API 和 Ollama"""

import json as json_module
import logging
from typing import AsyncGenerator, Optional

import httpx

from app.llm.exceptions import LLMCallError, LLMTimeoutError, LLMConnectionError

logger = logging.getLogger(__name__)


class LLMClient:

    def __init__(
        self,
        provider: str,
        model_name: str,
        base_url: str,
        api_key: str = "",
        temperature: float = 0.7,
        max_tokens: int = 4096,
        timeout: int = 60,
    ):
        self.provider = provider.lower()
        self.model_name = model_name
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.timeout = timeout

    async def chat(self, messages: list[dict]) -> dict:
        if self.provider == "ollama":
            return await self._call_ollama(messages)
        else:
            return await self._call_openai_compatible(messages)

    async def chat_str(self, messages: list[dict]) -> str:
        result = await self.chat(messages)
        content = result.get("content", "")
        if not content:
            choices = result.get("choices", [])
            if choices:
                msg = choices[0].get("message", {})
                content = msg.get("content", "")
        if not content:
            raise LLMCallError("LLM 返回空内容")
        return content

    async def chat_stream(self, messages: list[dict]) -> AsyncGenerator[str, None]:
        """
        真正的流式调用 — 设置 stream: true，逐 token yield
        """
        if self.provider == "ollama":
            async for chunk in self._call_ollama_stream(messages):
                yield chunk
        else:
            async for chunk in self._call_openai_stream(messages):
                yield chunk

    async def _call_openai_stream(self, messages: list[dict]) -> AsyncGenerator[str, None]:
        url = f"{self.base_url}/v1/chat/completions"
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }
        body = {
            "model": self.model_name,
            "messages": messages,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
            "stream": True,
        }
        logger.info("流式调用 OpenAI 兼容 API: url=%s model=%s", url, self.model_name)

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                async with client.stream("POST", url, json=body, headers=headers) as resp:
                    resp.raise_for_status()
                    async for line in resp.aiter_lines():
                        line = line.strip()
                        if not line or not line.startswith("data: "):
                            continue
                        data_str = line[6:]
                        if data_str == "[DONE]":
                            break
                        try:
                            data = json_module.loads(data_str)
                        except json_module.JSONDecodeError:
                            continue
                        choices = data.get("choices", [])
                        if choices:
                            delta = choices[0].get("delta", {})
                            content = delta.get("content", "")
                            if content:
                                yield content
            except httpx.TimeoutException:
                raise LLMTimeoutError(f"LLM 流式调用超时: {url}")
            except httpx.ConnectError as e:
                raise LLMConnectionError(f"LLM 流式连接失败: {url}: {e}")
            except httpx.HTTPStatusError as e:
                # 流式响应不能直接访问 .text，需要先 read
                try:
                    await e.response.aread()
                    err_body = e.response.text[:500]
                except Exception:
                    err_body = "(无法读取响应体)"
                raise LLMCallError(f"LLM 流式 HTTP {e.response.status_code}: {err_body}")

    async def _call_ollama_stream(self, messages: list[dict]) -> AsyncGenerator[str, None]:
        url = f"{self.base_url}/api/chat"
        body = {
            "model": self.model_name,
            "messages": messages,
            "stream": True,
            "options": {
                "temperature": self.temperature,
                "num_predict": self.max_tokens,
            },
        }
        logger.info("流式调用 Ollama API: url=%s model=%s", url, self.model_name)

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                async with client.stream("POST", url, json=body) as resp:
                    resp.raise_for_status()
                    async for line in resp.aiter_lines():
                        line = line.strip()
                        if not line:
                            continue
                        try:
                            data = json_module.loads(line)
                        except json_module.JSONDecodeError:
                            continue
                        message = data.get("message", {})
                        content = message.get("content", "")
                        if content:
                            yield content
                        if data.get("done", False):
                            break
            except httpx.TimeoutException:
                raise LLMTimeoutError(f"Ollama 流式调用超时: {url}")
            except httpx.ConnectError as e:
                raise LLMConnectionError(f"Ollama 流式连接失败: {url}: {e}")
            except httpx.HTTPStatusError as e:
                try:
                    await e.response.aread()
                    err_body = e.response.text[:500]
                except Exception:
                    err_body = "(无法读取响应体)"
                raise LLMCallError(f"Ollama 流式 HTTP {e.response.status_code}: {err_body}")

    async def _call_openai_compatible(self, messages: list[dict]) -> dict:
        url = f"{self.base_url}/v1/chat/completions"
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }
        body = {
            "model": self.model_name,
            "messages": messages,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
        }
        logger.info("调用 OpenAI 兼容 API: url=%s model=%s", url, self.model_name)

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                resp = await client.post(url, json=body, headers=headers)
                resp.raise_for_status()
            except httpx.TimeoutException:
                raise LLMTimeoutError(f"LLM 调用超时: {url} (timeout={self.timeout}s)")
            except httpx.ConnectError as e:
                raise LLMConnectionError(f"LLM 连接失败: {url}: {e}")
            except httpx.HTTPStatusError as e:
                raise LLMCallError(f"LLM HTTP {e.response.status_code}: {e.response.text[:500]}")

        await resp.aread()
        data = resp.json()
        usage = data.get("usage", {})
        choices = data.get("choices", [])

        content = ""
        if choices:
            content = choices[0].get("message", {}).get("content", "")

        return {
            "content": content,
            "model": data.get("model", self.model_name),
            "usage": {
                "prompt_tokens": usage.get("prompt_tokens", 0),
                "completion_tokens": usage.get("completion_tokens", 0),
                "total_tokens": usage.get("total_tokens", 0),
            },
            "raw": data,
        }

    async def _call_ollama(self, messages: list[dict]) -> dict:
        url = f"{self.base_url}/api/chat"
        body = {
            "model": self.model_name,
            "messages": messages,
            "stream": False,
            "options": {
                "temperature": self.temperature,
                "num_predict": self.max_tokens,
            },
        }
        logger.info("调用 Ollama API: url=%s model=%s", url, self.model_name)

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                resp = await client.post(url, json=body)
                resp.raise_for_status()
            except httpx.TimeoutException:
                raise LLMTimeoutError(f"Ollama 调用超时: {url}")
            except httpx.ConnectError as e:
                raise LLMConnectionError(f"Ollama 连接失败: {url}: {e}")
            except httpx.HTTPStatusError as e:
                raise LLMCallError(f"Ollama HTTP {e.response.status_code}: {e.response.text[:500]}")

        await resp.aread()
        data = resp.json()
        message = data.get("message", {})
        content = message.get("content", "")

        eval_count = data.get("eval_count", 0)
        prompt_eval_count = data.get("prompt_eval_count", 0)

        return {
            "content": content,
            "model": data.get("model", self.model_name),
            "usage": {
                "prompt_tokens": prompt_eval_count,
                "completion_tokens": eval_count,
                "total_tokens": prompt_eval_count + eval_count,
            },
            "raw": data,
        }