"""Forge Client — 封装 forge-agent 核心引擎，提供同步/流式执行接口

直接 import forge-agent 模块（同仓库），避免子进程开销。
通过 sys.path 注入 forge-agent 根目录。
"""

from __future__ import annotations

import json
import logging
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
from tools.shell_tool import ShellTool                                               # noqa: E402
from tools.test_tool import PytestTool                                               # noqa: E402
from tools.runtime import LocalRuntime                                               # noqa: E402
from agent.core import Agent, AgentConfig                                            # noqa: E402
from agent.task import Task, EventType                                               # noqa: E402
from agent.event_log import EventLog                                                 # noqa: E402

from app.forge_config import ForgeConfig

logger = logging.getLogger(__name__)


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

        # 2. 工具注册表（注册全部 12 个 forge-agent 内置工具）
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

    async def execute_stream(self, config: ForgeConfig) -> AsyncGenerator[str, None]:
        """流式执行，通过 AsyncGenerator yield SSE 事件字符串

        实现方式：agent 在独立线程中运行，通过 hook EventLog._append 将事件
        推送到线程安全队列，主协程轮询队列组装 SSE 事件。

        SSE 事件类型：
            event: step  — Agent 每一步的 Action / Observation / Reflection
            event: done  — 执行完成，含 summary / steps / tokens / patch
            event: error — 执行失败，含错误信息
        """
        import asyncio

        event_queue: Queue = Queue()
        stop_event = threading.Event()
        agent_result: dict[str, Any] = {}
        agent_error: Exception | None = None

        def _run_agent():
            nonlocal agent_error
            try:
                agent, task, log = self._build_components(config)
                # Monkey-patch EventLog._append：每次写事件同时推送到队列
                original_append = log._append

                def _hooked_append(event):
                    original_append(event)
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
            await asyncio.sleep(0.1)  # 100ms 轮询间隔

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
            SSE 格式字符串，或 None（TASK_COMPLETE/FAILED 不单独发送）
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
