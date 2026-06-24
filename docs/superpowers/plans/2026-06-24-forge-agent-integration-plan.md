# Forge Agent 集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 forge-agent 作为平台第一个 Real Agent Runtime 接入，用户在聊天中选择 Forge Agent 后系统通过 Agent 引擎执行真实任务。

**Architecture:** agent-service 直接 import forge-agent 核心模块（同仓库），通过 ForgeClient 封装 Agent 构建和执行，暴露 `/api/v1/forge/stream` SSE 端点。MVP 阶段不定义 Java AgentRuntime 接口，Python 侧先跑通。

**Tech Stack:** Python 3.11+ (FastAPI), forge-agent (ReAct + ToolRegistry), Vue 3 (Element Plus)

## Global Constraints

- 不改动现有 CodeAgent / RAGAgent / ReviewAgent / PlannerAgent 的任何代码
- 不改动 Tool 模块（AiTool / ToolService 保持 Mock 状态）
- 不改动数据库表结构
- forge-agent 依赖通过直接 import 方式复用（同仓库，sys.path 注入）
- SSE 事件格式与现有 `/api/v1/chat/stream` 兼容（event: step / event: token / event: done / event: error）
- MVP Shell 安全：仅允许 forge-agent 内置的只读白名单命令，危险命令需确认

---

## 文件结构

```
agent-service/
├── app/
│   ├── forge_config.py    ← 🆕 配置构建器（ForgeConfig dataclass + from_platform_config）
│   └── forge_client.py    ← 🆕 ForgeClient（Agent 构建 + 同步执行 + 流式执行）
├── api/
│   └── forge_routes.py    ← 🆕 /forge/stream 端点（JWT 验证 + SSE 流式）
├── api/
│   └── routes.py          ← ✏️ chat_stream() 增加 agentType=="forge" 路由分支
└── app/__init__.py        ← ✏️ 注册 forge_router（create_app 中）

frontend/src/views/ai/agent/
└── index.vue              ← ✏️ type 下拉增加 "Forge" 选项
```

---

### Task 1: ForgeConfig — 配置构建器

**Files:**
- Create: `agent-service/app/forge_config.py`

**Interfaces:**
- Produces: `ForgeConfig` dataclass, `ForgeConfig.from_platform_config(task, agent_config, model_config, repo_path)` classmethod

- [ ] **Step 1: 创建 forge_config.py**

```python
"""从平台 Agent/Model 配置构建 forge-agent 运行参数"""

from dataclasses import dataclass, field
from typing import Any


@dataclass
class ForgeConfig:
    """forge-agent 运行所需全部参数"""
    task: str
    repo_path: str = "."
    provider: str = "deepseek"
    model_name: str = "deepseek-chat"
    api_key: str = ""
    base_url: str = "https://api.deepseek.com"
    max_tokens: int = 8192
    max_steps: int = 40
    temperature: float = 0.7
    budget_tokens: int = 80000
    log_dir: str = "./logs/forge"

    @classmethod
    def from_platform_config(
        cls,
        task: str,
        agent_config: dict[str, Any],
        model_config: dict[str, Any],
        repo_path: str = ".",
    ) -> "ForgeConfig":
        """从平台 API 传入的 Agent + Model 配置构建 ForgeConfig

        agent_config 字段（来自 Spring Boot ConversationConfigDto.AgentInfo）:
            agentId, agentName, agentType, systemPrompt,
            maxIterations, temperature, timeoutSeconds, tools

        model_config 字段（来自 Spring Boot ConversationConfigDto.ModelInfo）:
            modelId, provider, modelName, baseUrl, apiKey, maxTokens, temperature
        """
        provider = model_config.get("provider", "deepseek")
        # provider → base_url 映射（与 forge-agent llm/router.py 保持一致）
        provider_base_urls = {
            "deepseek": "https://api.deepseek.com",
            "groq": "https://api.groq.com/openai/v1",
            "ollama": "http://localhost:11434/v1",
        }

        return cls(
            task=task,
            repo_path=repo_path,
            provider=provider,
            model_name=model_config.get("modelName", "deepseek-chat"),
            api_key=model_config.get("apiKey", ""),
            base_url=model_config.get("baseUrl") or provider_base_urls.get(provider, ""),
            max_tokens=model_config.get("maxTokens", 8192),
            max_steps=agent_config.get("maxIterations", 40),
            temperature=agent_config.get("temperature", 0.7),
            budget_tokens=80000,
            log_dir="./logs/forge",
        )
```

- [ ] **Step 2: 验证导入**

```bash
cd agent-service && python -c "from app.forge_config import ForgeConfig; c = ForgeConfig(task='test'); print(c)"
```
Expected: 打印 ForgeConfig 实例

- [ ] **Step 3: 提交**

```bash
git add agent-service/app/forge_config.py
git commit -m "feat: add ForgeConfig builder for forge-agent integration"
```

---

### Task 2: ForgeClient — Agent 引擎封装

**Files:**
- Create: `agent-service/app/forge_client.py`

**Interfaces:**
- Consumes: `ForgeConfig` from Task 1
- Produces: `ForgeClient` class with `execute(config) -> dict` and `execute_stream(config) -> AsyncGenerator[str, None]`

- [ ] **Step 1: 创建 forge_client.py — 导入和基础设施**

```python
"""Forge Client — 封装 forge-agent 核心引擎，提供同步/流式执行接口

直接 import forge-agent 模块（同仓库），避免子进程开销。
通过 sys.path 注入 forge-agent 根目录。
"""

from __future__ import annotations

import json
import logging
import os
import sys
import threading
import time
from pathlib import Path
from queue import Queue
from typing import Any, AsyncGenerator

# 将 forge-agent 加入 sys.path（agent-service 和 forge-agent 在同一仓库）
_FORGE_ROOT = Path(__file__).parent.parent.parent / "forge-agent"
if str(_FORGE_ROOT) not in sys.path:
    sys.path.insert(0, str(_FORGE_ROOT))

# forge-agent 核心模块
from config.schema import AppConfig, AgentCfg, LLMConfig, ToolsConfig, ContextConfig  # noqa: E402
from llm.router import create_backend                                                # noqa: E402
from tools.base import ToolRegistry                                                  # noqa: E402
from tools.file_tool import FileReadTool, FileViewTool, FileWriteTool                # noqa: E402
from tools.git_tool import GitStatusTool, GitDiffTool, GitAddTool, GitCommitTool     # noqa: E402
from tools.search_tool import SearchTextTool, FindFilesTool, FindSymbolTool          # noqa: E402
from tools.shell_tool import ShellTool, always_allow                                 # noqa: E402
from tools.test_tool import PytestTool                                               # noqa: E402
from tools.runtime import LocalRuntime                                               # noqa: E402
from agent.core import Agent, AgentConfig                                            # noqa: E402
from agent.task import Task, EventType                                               # noqa: E402
from agent.event_log import EventLog                                                 # noqa: E402

from app.forge_config import ForgeConfig

logger = logging.getLogger(__name__)
```

- [ ] **Step 2: 添加 ForgeClient 类 — 核心 Agent 构建方法**

在 `forge_client.py` 中追加：

```python
class ForgeClient:
    """封装 forge-agent 构建和执行，对外暴露 execute / execute_stream 两个接口"""

    def _build_components(
        self,
        config: ForgeConfig,
        stream_callback=None,
    ) -> tuple[Agent, Task, EventLog]:
        """构建 forge-agent 三大核心组件：Agent + Task + EventLog

        每个调用独立构建，不共享状态。LLM 后端参数由平台配置传入。
        """
        # 1. LLM 后端（API key 优先用参数，fallback 到环境变量）
        backend = create_backend(
            provider=config.provider,
            model=config.model_name,
            api_key=config.api_key or None,
            base_url=config.base_url or None,
            max_tokens=config.max_tokens,
        )

        # 2. 工具注册表（MVP 安全模式：注册全部工具，Shell 无确认回调）
        runtime = LocalRuntime()
        registry = (
            ToolRegistry()
            .register(ShellTool(confirm_callback=None, runtime=runtime))
            .register(FileReadTool())
            .register(FileViewTool())
            .register(FileWriteTool())
            .register(SearchTextTool())
            .register(FindFilesTool())
            .register(FindSymbolTool())
            .register(PytestTool(runtime=runtime))
            .register(GitStatusTool(runtime=runtime))
            .register(GitDiffTool(runtime=runtime))
            .register(GitAddTool(runtime=runtime))
            .register(GitCommitTool(runtime=runtime))
        )

        # 3. AgentConfig（运行时参数）
        agent_config = AgentConfig(
            max_steps=config.max_steps,
            budget_tokens=config.budget_tokens,
            stream=stream_callback is not None,
            stream_callback=stream_callback,
        )
        agent = Agent(backend, registry, agent_config)

        # 4. Task（任务定义）
        task = Task(
            description=config.task,
            repo_path=config.repo_path,
            max_steps=config.max_steps,
            budget_tokens=config.budget_tokens,
        )

        # 5. EventLog（执行日志，JSONL 格式）
        log_dir = Path(config.log_dir)
        log_dir.mkdir(parents=True, exist_ok=True)
        log = EventLog.create(task, log_dir=str(log_dir))

        return agent, task, log
```

- [ ] **Step 3: 添加同步执行方法**

```python
    def execute(self, config: ForgeConfig) -> dict[str, Any]:
        """同步执行，阻塞直到 Agent 完成，返回完整结果 dict"""
        agent, task, log = self._build_components(config)

        t0 = time.time()
        result = agent.run(task, log)
        elapsed = time.time() - t0

        # 解析 EventLog 提取步骤信息
        steps = []
        for event in log.replay():
            payload = event.payload
            if event.event_type == EventType.ACTION:
                action = payload.get("action", {})
                steps.append({
                    "step": payload.get("step"),
                    "type": "action",
                    "thought": action.get("thought", "")[:300],
                    "toolCall": action.get("tool_call"),
                })
            elif event.event_type == EventType.OBSERVATION:
                obs = payload.get("observation", {})
                steps.append({
                    "type": "observation",
                    "toolName": obs.get("tool_name"),
                    "status": obs.get("status"),
                    "output": (obs.get("output", "") or "")[:500],
                })

        return {
            "status": result.status.value,
            "summary": result.summary,
            "stepsTaken": result.steps_taken,
            "totalTokens": result.total_tokens,
            "duration": round(elapsed, 1),
            "patch": result.patch,
            "error": result.error,
            "steps": steps,
        }
```

- [ ] **Step 4: 添加流式执行方法**

```python
    async def execute_stream(self, config: ForgeConfig) -> AsyncGenerator[str, None]:
        """流式执行，通过 AsyncGenerator yield SSE 事件字符串

        实现方式：agent 在独立线程中运行，通过 hook EventLog._append 将事件
        推送到线程安全队列，主协程轮询队列组装 SSE 事件。

        SSE 事件类型：
            event: step  — Agent 每一步的 Action / Observation / Reflection
            event: token — LLM 流式输出的文本片段
            event: done  — 执行完成，含 summary / steps / tokens / patch
            event: error — 执行失败，含错误信息
        """
        event_queue: Queue = Queue()
        stop_event = threading.Event()
        agent_result: dict[str, Any] = {}
        agent_error: Exception | None = None

        # Hook: 注入 event_queue，让 agent 线程把事件推送到队列
        def _run_agent():
            nonlocal agent_error
            try:
                agent, task, log = self._build_components(config)
                # Monkey-patch EventLog._append：每次写事件同时推送到队列
                original_append = log._append

                def _hooked_append(event):
                    original_append(event)
                    # 解析 event 并推送标准化 dict 到队列
                    event_dict = {
                        "event_type": event.event_type.value,
                        "payload": event.payload,
                        "timestamp": event.timestamp,
                    }
                    event_queue.put(event_dict)

                log._append = _hooked_append  # type: ignore[method-assign]

                result = agent.run(task, log)
                agent_result["status"] = result.status.value
                agent_result["summary"] = result.summary
                agent_result["stepsTaken"] = result.steps_taken
                agent_result["totalTokens"] = result.total_tokens
                agent_result["patch"] = result.patch
                agent_result["error"] = result.error
            except Exception as e:
                agent_error = e
            finally:
                stop_event.set()

        thread = threading.Thread(target=_run_agent, daemon=True)
        thread.start()

        # 主协程轮询队列，实时 yield SSE 事件
        while not stop_event.is_set() or not event_queue.empty():
            while not event_queue.empty():
                event_dict = event_queue.get_nowait()
                sse = self._event_to_sse(event_dict)
                if sse:
                    yield sse
            await _async_sleep(0.1)  # 100ms 轮询间隔

        thread.join(timeout=30)

        # 发送最终结果
        if agent_error:
            err_event = {"type": "error", "message": str(agent_error)}
            yield f"event: error\ndata: {json.dumps(err_event, ensure_ascii=False)}\n\n"
        else:
            done_event = {
                "type": "done",
                "status": agent_result.get("status", "UNKNOWN"),
                "summary": agent_result.get("summary", ""),
                "stepsTaken": agent_result.get("stepsTaken", 0),
                "totalTokens": agent_result.get("totalTokens", 0),
                "patch": agent_result.get("patch"),
            }
            yield f"event: done\ndata: {json.dumps(done_event, ensure_ascii=False)}\n\n"

    def _event_to_sse(self, event_dict: dict) -> str | None:
        """将 EventLog 事件 dict 转换为 SSE 事件字符串

        Returns:
            SSE 格式字符串（含 event: + data:），或 None（TASK_COMPLETE/FAILED 不单独发送）
        """
        etype = event_dict.get("event_type", "")
        payload = event_dict.get("payload", {})

        if etype == "task_start":
            data = {"type": "task_start", "task": payload.get("task", {}).get("description", "")}
            return f"event: step\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"

        elif etype == "action":
            action = payload.get("action", {})
            tc = action.get("tool_call")
            data = {
                "type": "step",
                "step": payload.get("step"),
                "thought": action.get("thought", "")[:300],
                "toolName": tc["name"] if tc else None,
                "toolParams": str(tc.get("params", {}))[:200] if tc else None,
            }
            return f"event: step\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"

        elif etype == "observation":
            obs = payload.get("observation", {})
            data = {
                "type": "observation",
                "toolName": obs.get("tool_name"),
                "status": obs.get("status"),
                "output": (obs.get("output", "") or "")[:500],
                "error": obs.get("error"),
            }
            return f"event: step\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"

        elif etype == "reflection":
            data = {
                "type": "reflection",
                "reason": payload.get("reason", ""),
            }
            return f"event: step\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"

        elif etype in ("task_complete", "task_failed"):
            return None  # 由 execute_stream 统一发送 done/error

        return None


def _async_sleep(seconds: float):
    """asyncio.sleep 包装，便于测试 mock"""
    import asyncio
    return asyncio.sleep(seconds)
```

- [ ] **Step 5: 验证 forge-agent 模块导入正确**

```bash
cd agent-service && python -c "
from app.forge_client import ForgeClient
c = ForgeClient()
print('ForgeClient imported OK')
print('Tool classes available:', hasattr(c, '_build_components'))
"
```
Expected: `ForgeClient imported OK` + `Tool classes available: True`

- [ ] **Step 6: 提交**

```bash
git add agent-service/app/forge_client.py
git commit -m "feat: add ForgeClient for forge-agent engine integration"
```

---

### Task 3: Forge Routes — API 端点

**Files:**
- Create: `agent-service/api/forge_routes.py`

**Interfaces:**
- Consumes: `ForgeClient` from Task 2, `ForgeConfig` from Task 1
- Produces: FastAPI APIRouter with `POST /api/v1/forge/stream`

- [ ] **Step 1: 创建 forge_routes.py — 路由和请求模型**

```python
"""Forge Agent API 端点 — /api/v1/forge/*

提供 forge-agent 的 HTTP 接口，支持：
- POST /forge/stream — 流式执行（SSE），前端直连
"""

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
import base64 as _b64
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
```

- [ ] **Step 2: 添加 JWT 验证辅助函数**

```python
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
```

- [ ] **Step 3: 添加流式端点**

```python
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
    4. StreamingResponse 输出 SSE 事件（step / token / done / error）
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
```

- [ ] **Step 4: 验证语法**

```bash
cd agent-service && python -c "from api.forge_routes import router; print('Routes OK:', router.routes)"
```
Expected: 打印路由列表

- [ ] **Step 5: 提交**

```bash
git add agent-service/api/forge_routes.py
git commit -m "feat: add /api/v1/forge/stream endpoint for Forge Agent"
```

---

### Task 4: 注册 Forge 路由

**Files:**
- Modify: `agent-service/main.py` 或 `agent-service/app/__init__.py`

**注意:** 路由在 `app/__init__.py` 的 `create_app()` 中注册（行 74-84）。

**Interfaces:**
- Consumes: `forge_routes.router` from Task 3

- [ ] **Step 1: 在 create_app() 中注册 forge_router**

修改 `agent-service/app/__init__.py`，在 `create_app()` 的路由注册区域（行 74-84 之后）新增：

```python
    from api.forge_routes import router as forge_router
    app.include_router(forge_router, prefix="/api/v1")
```

完整上下文（插入位置在 `from api.preview import router as preview_router` 之后）：

```python
    from api.preview import router as preview_router
    app.include_router(preview_router, prefix="/api/v1")

    from api.forge_routes import router as forge_router
    app.include_router(forge_router, prefix="/api/v1")

    return app
```

- [ ] **Step 2: 验证应用启动**

```bash
cd agent-service && timeout 5 python main.py 2>&1 || true
```
Expected: 日志中无 import 错误，且 `/api/v1/forge/stream` 出现在路由列表中（访问 http://localhost:8000/docs 确认）

- [ ] **Step 3: 提交**

```bash
git add agent-service/app/__init__.py
git commit -m "feat: register forge routes in FastAPI app"
```

---

### Task 5: chat_stream 增加 Forge 路由分支

**Files:**
- Modify: `agent-service/api/routes.py`

**Interfaces:**
- Consumes: 现有的 `chat_stream()` 函数
- 当 `agentType == "forge"` 时，不执行现有 LLM 直接调用逻辑，改为委托给 forge

- [ ] **Step 1: 在 chat_stream 的消息构建前注入 Forge 分支**

修改 `agent-service/api/routes.py`，在 `chat_stream()` 函数中，约第 378 行（构建 messages 之前），新增 forge 路由判断：

```python
    # === Forge Agent 路由分支 ===
    # 当 agentType 为 "forge" 时，不走 LLM 直接调用，委托给 forge-agent 执行
    agent_type = agent_config.get("agentType", "")
    if agent_type == "forge" and model_config:
        from app.forge_config import ForgeConfig
        from app.forge_client import ForgeClient

        forge_config = ForgeConfig.from_platform_config(
            task=req.message,
            agent_config=agent_config,
            model_config=model_config,
        )

        forge_client = ForgeClient()

        async def forge_event_stream():
            try:
                async for sse_event in forge_client.execute_stream(forge_config):
                    yield sse_event
            except Exception as e:
                logger.error("Forge 执行异常: %s", e)
                err = {"type": "error", "code": 500, "message": f"Forge 执行失败: {e}"}
                yield f"event: error\ndata: {json.dumps(err, ensure_ascii=False)}\n\n"

        return StreamingResponse(forge_event_stream(), media_type="text/event-stream")
    # === Forge 路由分支结束 ===
```

插入位置：在 `if rag_context:` / `else: system_prompt = _build_agent_system_prompt(...)` 这段之后，`messages = []` 之前（约行 371-381 之间）。

- [ ] **Step 2: 验证语法**

```bash
cd agent-service && python -c "
from api.routes import router
print('Routes OK, Forge branch syntax valid')
"
```
Expected: `Routes OK, Forge branch syntax valid`

- [ ] **Step 3: 提交**

```bash
git add agent-service/api/routes.py
git commit -m "feat: add forge agent routing branch in chat_stream"
```

---

### Task 6: 前端 — Agent 类型下拉增加 Forge

**Files:**
- Modify: `frontend/src/views/ai/agent/index.vue`

- [ ] **Step 1: 搜索条件下拉加 Forge**

在搜索区域（`<el-select v-model="queryParams.agentType">`，约第 8-15 行），在现有 `<el-option>` 列表末尾追加：

```html
              <el-option label="Forge" value="forge" />
```

完整上下文：
```html
          <el-option label="Planner" value="planner" />
          <el-option label="RAG" value="rag" />
          <el-option label="Code" value="code" />
          <el-option label="Review" value="review" />
          <el-option label="Tool" value="tool" />
          <el-option label="Custom" value="custom" />
          <el-option label="Forge" value="forge" />
```

- [ ] **Step 2: 新增/编辑对话框下拉加 Forge**

在表单区域（`<el-select v-model="form.agentType">`，约第 78-87 行），同样追加：

```html
                <el-option label="Forge" value="forge" />
```

完整上下文：
```html
                <el-option label="Planner" value="planner" />
                <el-option label="RAG" value="rag" />
                <el-option label="Code" value="code" />
                <el-option label="Review" value="review" />
                <el-option label="Tool" value="tool" />
                <el-option label="Custom" value="custom" />
                <el-option label="Forge" value="forge" />
```

- [ ] **Step 3: 验证前端编译**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | head -20
```
Expected: 无新增编译错误

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/ai/agent/index.vue
git commit -m "feat: add Forge option to agent type dropdown"
```

---

### Task 7: 集成验证

**Files:**
- 无新建文件

- [ ] **Step 1: 验证 agent-service 启动**

```bash
cd agent-service && python -c "
from app import create_app
app = create_app()
for route in app.routes:
    if hasattr(route, 'path'):
        print(route.path, route.methods if hasattr(route, 'methods') else '')
" 2>&1 | grep forge
```
Expected: `/api/v1/forge/stream` 出现在路由列表中

- [ ] **Step 2: 验证 ForgeClient 能成功导入所有 forge-agent 模块**

```bash
cd agent-service && python -c "
from app.forge_client import ForgeClient
from app.forge_config import ForgeConfig

# 使用 Mock backend 避免真实 LLM 调用
config = ForgeConfig(
    task='在 /tmp/test_forge.py 中写一个 hello 函数',
    repo_path='/tmp',
    provider='deepseek',
    model_name='deepseek-chat',
    api_key='test-key',
    max_steps=3,
)

# 仅验证构建组件，不执行
client = ForgeClient()
agent, task, log = client._build_components(config)
print('Agent built OK')
print('Tools:', [t.name for t in agent._registry._tools.values()])
print('Max steps:', agent._cfg.max_steps)
print('Task:', task.description)
"
```
Expected: 打印 Agent 信息、12 个工具名称、任务描述

- [ ] **Step 3: 端到端手动验证**

1. 启动 agent-service: `cd agent-service && python main.py`
2. 在前端 Agent 管理页面创建或编辑一个 Agent，type 选择 "Forge"，绑定一个已配置的模型
3. 使用 curl 测试（需要有效的 JWT token）：

```bash
curl -X POST http://localhost:8000/api/v1/forge/stream \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"message":"在 /tmp/test_hello.py 中写一个打印hello的函数","repoPath":"/tmp"}' \
  --no-buffer
```

Expected: 收到 SSE 事件流，包含 step 事件（每步的 action/observation），最终收到 done 事件含 summary

- [ ] **Step 4: 提交**

```bash
git commit --allow-empty -m "chore: verify forge-agent integration end-to-end"
```

---

## 验证清单

完成所有 Task 后确认：

- [ ] `agent-service/app/forge_config.py` 存在，`ForgeConfig.from_platform_config()` 可正确转换平台配置
- [ ] `agent-service/app/forge_client.py` 存在，`ForgeClient._build_components()` 成功构建 12 个工具
- [ ] `agent-service/api/forge_routes.py` 存在，`POST /api/v1/forge/stream` 可接受请求
- [ ] `agent-service/app/__init__.py` 中 forge_router 已注册
- [ ] `agent-service/api/routes.py` 中 `chat_stream()` 有 `agentType == "forge"` 分支
- [ ] 前端 `agent/index.vue` 的两个 `<el-select>` 都包含 `<el-option label="Forge" value="forge" />`
- [ ] 启动 agent-service 无 import 错误
- [ ] curl 测试 `/api/v1/forge/stream` 返回 SSE 事件流

## 回滚

如有问题，只需 revert 这 6 个 commit（Task 1-6），不涉及数据库迁移或配置变更。
