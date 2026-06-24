# Forge Agent 集成设计 Spec

> 日期：2026-06-24
> 状态：待实现
> 关联：[[2026-06-22-m9-agent-model-tool-pages-design]] [[2026-06-23-ai-chat-streaming-refactor-design]]

---

## 1. 背景

### 1.1 现状诊断

经过完整的代码审计，当前系统的 Agent 体系属于 **配置型 Fake Agent**：

| 组件 | 真实能力 | 缺失能力 |
|------|---------|---------|
| AgentConfig / AgentService / AgentController | CRUD 管理 | 无 Agent Runtime |
| CodeAgent / RAGAgent / ReviewAgent (agent-service) | Prompt + LLM 单次调用 | 无 ReAct 循环、无 Tool Calling |
| PlannerAgent (agent-service) | LLM 意图路由 | 非真正编排，子 Agent 不在路由中使用 |
| AiTool / ToolService / ToolController | 工具元数据 CRUD | invokeTool() 硬编码返回 Mock JSON |
| AiModel / ModelService | 模型连接配置 CRUD | 仅用于传参给 LLMClient |
| Chat 流 (`/api/v1/chat/stream`) | SSE 流式 LLM 文本生成 | 无 Agent 循环，工具仅作文本注入 Prompt |

**核心判断：**
```
用户 → 前端 → Spring Boot → FastAPI → LLM API → 返回文本

AgentConfig  = 7 个配置字段的数据库行
Tool         = 元数据 + Mock 返回
Chat         = LLM 文本完成（无 tool_use）
```

### 1.2 目标

引入 `forge-agent` 作为平台第一个 **Real Agent Runtime**，使系统具备：

```
用户 → 选择 Agent → Agent 执行任务 → 调用真实工具 → 修改文件 → 执行命令 → 返回结果
```

---

## 2. Forge Agent 现状

`forge-agent/` 是项目中已存在的独立 Python 自主编码 Agent，具有完整的 ReAct 架构：

| 能力 | 文件 | 行数 | 状态 |
|------|------|------|------|
| ReAct 主循环 | `agent/core.py` | ~400 | 完整 |
| 5 类真实工具 | `tools/{file,shell,search,test,git}_tool.py` | ~600 | 完整 |
| 4 层安全模型 | `shell_tool.py` | ~300 | 完整（黑名单/白名单/确认/timeout） |
| 多 LLM 后端 | `llm/{anthropic_backend,openai_compat,router}.py` | ~400 | 5 Provider |
| 上下文管理 | `context/{token_budget,history,repo_map}.py` | ~300 | Token 预算/历史裁剪/仓库映射 |
| 事件日志 | `agent/event_log.py` | ~120 | JSONL 可回放 |
| 死循环检测 | `agent/core.py:_is_looping()` | ~20 | 连续相同 action |
| Reflection | `agent/core.py` | ~30 | 测试失败/无编辑两种触发 |
| CLI 入口 | `entry/{cli,chat}.py` | ~400 | `agent run` / `agent chat` |
| GitHub 集成 | `entry/github_issue.py` | ~120 | Issue→PR 自动化 |
| 配置管理 | `config/{schema,default.yaml}` | ~80 | YAML + 环境变量 |
| 项目定义 | `pyproject.toml` | ~40 | Python >=3.11 |

---

## 3. 架构设计

### 3.1 目标 Agent Runtime 体系

```
AgentDefinition (配置层)
├── agentId, agentName, agentType
├── systemPrompt, modelRef, toolRefs
└── runtimeConfig (maxSteps, temperature, budget, ...)

         │ 1:1
         ▼

AgentRuntime (运行时层 — 接口)
├── interface AgentRuntime {
│       AgentResult execute(AgentContext context)
│       Stream<AgentEvent> executeStream(AgentContext context)
│       void cancel(String executionId)
│       AgentCapability getCapability()
│   }
├── ForgeAgentRuntime implements AgentRuntime   ← 本期实现
├── BrowserAgentRuntime implements AgentRuntime ← 后续
├── SqlAgentRuntime implements AgentRuntime     ← 后续
└── RagAgentRuntime implements AgentRuntime     ← 后续

         │ has-a
         ▼

Tool (真实可执行工具 — 接口)
├── interface Tool {
│       ToolResult execute(Map<String,Object> params)
│       ToolSchema getSchema()
│       ToolCategory getCategory()
│       SecurityLevel getSecurityLevel()
│   }
├── FileTool, ShellTool, SearchTool, TestTool, GitTool  ← forge-agent 自带
├── McpToolAdapter (适配现有 AiTool 配置)               ← 后续
└── BrowserTool, SqlTool                                 ← 后续

         │ has-a
         ▼

Environment (执行环境)
├── LocalEnvironment     ← 本期使用
└── DockerEnvironment    ← 后续
```

### 3.2 调用链

```
Vue (选择 Forge Agent, 输入任务)
 │
 │  POST /api/v1/chat/stream   { agentId: X, message: "帮我给用户管理加分页" }
 ▼
FastAPI (api/routes.py :: chat_stream)
 │
 │  agentType == "forge" → 路由到 Forge Runtime
 ▼
ForgeClient (app/forge_client.py)
 │
 │  subprocess: python -m forge-agent.entry.cli run --task "..." --repo-path "..."
 │  解析 JSONL EventLog → SSE 事件流
 ▼
Forge Agent (forge-agent/)
 │
 ├── Step 1: LLM → "先搜索用户管理代码" → Action: search_text → 找到 UserController.vue
 ├── Step 2: LLM → "读取文件" → Action: file_read → 读取 120 行
 ├── Step 3: LLM → "增加分页" → Action: file_write → 写入 156 行
 ├── Step 4: LLM → "运行测试" → Action: shell(pytest) → 测试通过
 └── Step 5: LLM → "完成" → Action: FINISH → 返回 diff + summary
```

### 3.3 模块边界

```
┌─────────────────────────────────────────────────────────┐
│                     agent-service                        │
│                                                         │
│  api/routes.py        ← 现有，增加 forge 路由分支        │
│  api/forge_routes.py  ← 🆕 独立 /forge/* 端点            │
│  app/forge_client.py  ← 🆕 Forge 子进程管理 + 输出解析    │
│  agents/              ← 现有，不动                       │
│  workflows/           ← 现有，不动                       │
└─────────────────────────────────────────────────────────┘
                           │
                           │ subprocess / import
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     forge-agent                          │
│                                                         │
│  agent/core.py        ← ReAct 主循环（直接复用）         │
│  tools/               ← 5 类工具（直接复用）             │
│  llm/                 ← LLM 后端（复用，参数由平台传入）  │
│  context/             ← 上下文管理（直接复用）            │
│  agent/event_log.py   ← EventLog → SSE 事件映射          │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 领域模型

### 4.1 AgentRuntime 接口（Java）

```java
// backend/.../runtime/AgentRuntime.java
public interface AgentRuntime {
    // 同步执行（阻塞，返回完整结果）
    AgentResult execute(AgentContext context);

    // 流式执行（SSE 事件流）
    Flux<AgentEvent> executeStream(AgentContext context);

    // 取消执行
    void cancel(String executionId);

    // 能力声明
    AgentCapability getCapability();
}
```

### 4.2 AgentContext（Java）

```java
// backend/.../runtime/AgentContext.java
public class AgentContext {
    String task;              // 任务描述
    String workspace;         // 工作目录（repo path）
    Long agentId;             // Agent 配置 ID
    String agentType;         // "forge"
    Map<String, Object> agentConfig;  // systemPrompt, maxSteps, temperature, budget
    Map<String, Object> modelConfig;  // provider, modelName, apiKey, baseUrl, maxTokens
    List<Map<String, Object>> toolConfigs;  // 工具列表
    List<Map<String, String>> history;       // 历史消息
    Map<String, Object> callbacks;           // 回调配置
}
```

### 4.3 AgentResult / AgentEvent（Java）

```java
public class AgentResult {
    String status;          // SUCCESS | FAILED | MAX_STEPS | GAVE_UP | CANCELLED
    String summary;         // 最终输出
    int stepsTaken;         // 执行步数
    int totalTokens;        // Token 消耗
    String patch;           // Git diff
    long duration;          // 执行时长 ms
    List<StepRecord> steps; // 每步详情
}

public class AgentEvent {
    String type;            // step | token | tool_call | tool_result | reflection | done | error
    Object data;            // 事件负载
}
```

### 4.4 AgentCapability

```java
public class AgentCapability {
    boolean canEditFiles;       // 是否能修改文件
    boolean canExecuteCommands; // 是否能执行命令
    boolean canSearchCode;      // 是否能搜索代码
    boolean canRunTests;        // 是否能运行测试
    boolean canUseGit;          // 是否能操作 Git
    List<String> supportedLanguages; // 支持的编程语言
}
```

---

## 5. MVP 实施方案

### 5.1 MVP 范围

**做：**
- 用户在前端聊天界面选择 Forge Agent → 输入任务 → 系统执行 → 返回结果
- 新增 `/api/v1/forge/stream` SSE 端点
- 新增 `ForgeClient` 封装 forge-agent 子进程
- 前端 Agent 类型下拉增加 "Forge" 选项
- 后端 agentType 枚举增加 "forge"

**不做：**
- 不重构现有 CodeAgent / RAGAgent / ReviewAgent
- 不重构 Tool 模块（MVP 阶段 Forge 用自带工具）
- 不实现 TaskQueue
- 不改数据库表结构
- 不实现 Docker 沙箱
- 不定义 Java 侧 AgentRuntime 接口（Python 侧先跑通）

### 5.2 文件变更清单

```
🆕 新增文件 (3个):
  agent-service/app/forge_client.py
    — ForgeClient 类：subprocess 管理 + EventLog JSONL 解析 + SSE 事件生成
    — ForgeConfig 数据类：封装所有 forge-agent 运行参数

  agent-service/api/forge_routes.py
    — POST /api/v1/forge/execute  — 同步执行（非流式）
    — POST /api/v1/forge/stream   — 流式执行（SSE 事件流）
    — JWT 验证 + 模型查找 + Agent 配置解析

  agent-service/app/forge_config.py
    — ForgeConfigBuilder：从平台 AgentConfig + ModelConfig 构建 forge-agent 配置
    — 工具注册映射：平台工具配置 → forge-agent ToolRegistry

📝 修改文件 (4个):
  agent-service/main.py
    — 注册 forge_routes 路由: app.include_router(forge_router, prefix="/api/v1")

  agent-service/api/routes.py
    — chat_stream() 中增加 agentType=="forge" 的路由分支
    — 当 agentType==forge 时，委托给 forge_client

  frontend/src/views/ai/agent/index.vue
    — type 下拉增加 <el-option label="Forge" value="forge" />
    — 搜索条件增加 "Forge" 选项

  backend/.../web/controller/ai/AgentController.java
    — agentType 枚举注释更新，确认 "forge" 为有效值
```

### 5.3 ForgeClient 设计

```python
# agent-service/app/forge_client.py

class ForgeConfig:
    """从平台配置构建的 forge-agent 运行参数"""
    task: str
    repo_path: str
    provider: str           # deepseek / openai / anthropic
    model_name: str
    api_key: str
    base_url: str
    max_tokens: int
    max_steps: int
    temperature: float
    budget_tokens: int
    stream: bool

class ForgeClient:
    """forge-agent 子进程包装器"""

    async def execute(self, config: ForgeConfig) -> dict:
        """同步执行，返回完整 AgentResult"""
        ...

    async def execute_stream(self, config: ForgeConfig) -> AsyncGenerator[str, None]:
        """流式执行，yield SSE 事件字符串"""
        ...

    def _build_command(self, config: ForgeConfig) -> list[str]:
        """构建 forge-agent CLI 命令"""
        return [
            sys.executable, "-m", "entry.cli", "run",
            "--task", config.task,
            "--repo-path", config.repo_path,
            "--provider", config.provider,
            "--model", config.model_name,
            "--api-key", config.api_key,
            "--base-url", config.base_url,
            "--max-steps", str(config.max_steps),
        ]

    def _parse_eventlog_line(self, line: str) -> dict:
        """解析单行 JSONL → SSE 事件"""
        event = json.loads(line)
        event_type = event.get("event_type", "")
        # TASK_START → pending
        # ACTION → step {tool_name, thought, params}
        # OBSERVATION → tool_result {status, output}
        # REFLECTION → reflection {reason, prompt}
        # TASK_COMPLETE → done {summary, steps, tokens}
        # TASK_FAILED → error {reason, steps}
        return self._event_to_sse(event_type, event.get("payload", {}))
```

### 5.4 安全策略

**MVP 阶段安全措施：**

1. **仅允许只读命令**：MVP 阶段 Forge Shell 配置为只读模式，禁止 write 命令
2. **工作目录隔离**：repo_path 限定为项目工作目录，不可修改系统文件
3. **超时保护**：单个 Agent 执行超时 5 分钟，单步超时 30 秒
4. **Token 预算**：默认 80,000 tokens，防止 LLM 无限调用
5. **用户确认**：危险命令（git commit/push, 文件写入）发送确认事件到前端

**完整版安全（V2）：**
1. Docker 沙箱（`--network none`）
2. 文件路径 confine 到 workspace
3. 完整 4 层 shell 安全

### 5.5 风险评估

| 风险 | 级别 | 应对 |
|------|------|------|
| forge-agent 子进程挂起 | 🟡 中 | 设置 timeout，超时 SIGKILL |
| 大文件输出撑爆内存 | 🟡 中 | EventLog 行级流式读取，限制缓冲区 |
| Agent 执行危险命令 | 🔴 高 | MVP 只允许只读模式 + 确认回调 |
| forge-agent 依赖冲突 | 🟢 低 | 子进程隔离，独立 Python 环境 |
| 与现有 chat/stream 逻辑冲突 | 🟢 低 | 独立端点 `/api/v1/forge/*`，不碰现有代码 |
| 用户等待时间过长 | 🟡 中 | SSE 实时推送每步进度，非黑盒等待 |

---

## 6. 测试策略

### 6.1 单元测试

- `ForgeClient._parse_eventlog_line()` — 各事件类型解析
- `ForgeClient._build_command()` — CLI 参数构建
- `ForgeConfigBuilder.from_platform_config()` — 配置转换

### 6.2 集成测试

- ForgeClient 使用 MockBackend（不调真实 LLM）验证端到端流程
- forge_routes 端点 JWT 验证
- SSE 事件格式验证

### 6.3 手动验证

- 前端选择 Forge Agent → 输入 "在 UserController 中加一个 hello 接口"
- 观察 EventLog 中是否记录了完整的 ReAct 循环
- 验证文件确实被修改

---

## 7. 后续演进路线

```
MVP (本期)
  └── Forge Agent 作为独立 Runtime 接入
      └── 前端选择 → 后端路由 → subprocess 调用 → SSE 返回

V2 — AgentRuntime 接口
  ├── 定义 Java AgentRuntime 接口
  ├── ForgeAgentRuntime 实现
  ├── 重构现有 Agent 为 Runtime 实现
  └── Tool 接口 + ToolRegistry

V3 — 多 Agent 体系
  ├── BrowserAgent (Playwright)
  ├── SqlAgent (JDBC)
  ├── RagAgent (Milvus + LLM)
  └── Multi-Agent 编排（LangGraph）

V4 — 平台化
  ├── Agent 市场（可插拔）
  ├── Task Queue（持久化异步任务）
  ├── Agent 执行沙箱（Docker/K8s）
  └── 执行监控 & 审计
```

---

## 8. 附录：关键文件路径

### 涉及修改的文件

| 文件 | 作用 |
|------|------|
| `agent-service/app/forge_client.py` | 🆕 Forge 子进程管理 |
| `agent-service/app/forge_config.py` | 🆕 配置构建器 |
| `agent-service/api/forge_routes.py` | 🆕 /forge/* 端点 |
| `agent-service/main.py` | 注册路由 |
| `agent-service/api/routes.py` | 增加 forge 分支 |
| `frontend/src/views/ai/agent/index.vue` | type 下拉加 Forge |

### 不修改但需了解的文件

| 文件 | 作用 |
|------|------|
| `forge-agent/agent/core.py` | ReAct 主循环 |
| `forge-agent/tools/base.py` | ToolRegistry |
| `forge-agent/llm/router.py` | LLM 后端工厂 |
| `forge-agent/config/schema.py` | 配置加载 |
| `agent-service/app/internal_client.py` | Spring Boot 内部 API 客户端 |
| `agent-service/app/llm/client.py` | LLM API 调用 |
| `backend/.../domain/AgentConfig.java` | Agent 配置实体 |
| `backend/.../service/impl/AgentServiceImpl.java` | Agent 服务 |
