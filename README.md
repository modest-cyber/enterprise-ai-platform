# Enterprise AI Platform

企业级 AI 知识中台与研发管理系统 — 基于 RuoYi 框架，集成大模型对话、RAG 知识检索、Agent 编排、MCP 工具协议的综合性 AI 平台。

## 项目概述

本项目构建了一个完整的企业 AI 应用中台，核心能力包括：

- **AI 对话管理**：多模型、多 Agent 的流式对话（SSE），支持会话历史管理
- **知识库 RAG**：文档解析（PDF/Word/Excel/PPT/Markdown）→ 分块 → Embedding → Milvus 向量检索 → LLM 增强生成
- **Agent 编排**：Planner 路由、Code→Review 串联、RAG 检索增强等 LangGraph 工作流
- **MCP 工具协议**：标准化工具注册、Schema 校验、JSON-RPC 调用、重试机制
- **统一管理后台**：用户/角色/菜单权限体系、代码生成器、定时任务、监控面板

## 技术架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        前端 (Vue3 + TS)                             │
│                Element Plus · Pinia · Vite · Axios                  │
│                        Port: 5173 (dev)                             │
└──────┬──────────────────────────────┬───────────────────────────────┘
       │  /dev-api/* (REST)           │  /api/v1/* (SSE Stream)
       ▼                              ▼
┌──────────────────┐    ┌──────────────────────────────────────────────┐
│   Spring Boot    │    │          Python FastAPI Agent Service        │
│   (Java 17)      │◄───│               (Python 3.12)                  │
│   Port: 8080     │HTTP│               Port: 8000                     │
│                  │    │                                              │
│  · RBAC 权限      │    │  · LLM Client (OpenAI/Ollama 兼容)           │
│  · 会话/消息 CRUD  │    │  · RAG Pipeline (Milvus + Embedding)        │
│  · Agent/模型管理  │    │  · MCP Tool Protocol                       │
│  · 知识库管理      │    │  · LangGraph Workflows                     │
│  · 内部接口       │    │  · Document Loaders (7 种格式)               │
└──────┬───────────┘    └────────────────┬─────────────────────────────┘
       │                                 │
       ▼                                 ▼
┌──────────┐  ┌──────────┐  ┌──────────────────────────────┐
│  MySQL   │  │  Redis   │  │  LLM Providers               │
│  (Druid) │  │ (Cache)  │  │  OpenAI · DeepSeek · Qwen    │
│          │  │          │  │  Ollama · SiliconFlow         │
└──────────┘  └──────────┘  └──────────────────────────────┘
                              ┌──────────────┐
                              │  Milvus Lite │
                              │  (Vector DB) │
                              └──────────────┘
```

### 技术栈汇总

| 层次 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot + RuoYi | 4.0.6 / 3.9.2 |
| **语言** | Java | 17 |
| **ORM** | MyBatis | 4.0.1 |
| **数据库** | MySQL (Druid 连接池) | 8.0 |
| **缓存** | Redis (Lettuce) | - |
| **认证** | Spring Security + JWT (HS512) | - |
| **API 文档** | SpringDoc OpenAPI 3 | - |
| **前端框架** | Vue 3 + TypeScript | 3.5 / 5.6 |
| **UI 组件库** | Element Plus | 2.13 |
| **构建工具** | Vite | 6.4 |
| **状态管理** | Pinia | 3.0 |
| **Agent 服务** | FastAPI + LangChain + LangGraph | 0.115 / 1.2.5 |
| **向量数据库** | Milvus Lite / Milvus | 2.4+ |
| **Embedding** | BGE-small-zh-v1.5 / OpenAI / Ollama | - |
| **文档解析** | PyMuPDF, python-docx, openpyxl, python-pptx | - |
| **进程通信** | HTTP (Spring WebClient ↔ httpx) + Internal JWT | - |

## 项目结构

```
enterprise-ai-platform/
├── backend/                          # Java Spring Boot 后端
│   ├── pom.xml                       # Maven 父 POM（7 个模块）
│   ├── ruoyi-admin/                  # 【入口】应用启动 + 所有 Controller
│   │   └── src/main/java/.../web/controller/
│   │       ├── ai/                   # AI 业务 Controller（9 个）
│   │       ├── system/               # 系统管理 Controller（13 个）
│   │       ├── monitor/              # 监控 Controller（5 个）
│   │       └── common/               # 公共 Controller（2 个）
│   ├── ruoyi-ai/                     # 【核心】AI 模块
│   │   └── src/main/java/.../ai/
│   │       ├── client/               # FastApiClient / ChatAgentClient
│   │       ├── config/               # AgentProperties 配置
│   │       ├── domain/               # 8 个实体 + 8 个 DTO
│   │       ├── mapper/               # 9 个 MyBatis Mapper
│   │       └── service/              # 9 个 Service 接口 + 实现
│   ├── ruoyi-framework/              # 【框架】Security / JWT / AOP / Filter
│   ├── ruoyi-system/                 # 【系统】用户/角色/菜单/部门 CRUD
│   ├── ruoyi-common/                 # 【公共】工具类 / 注解 / 异常 / BaseEntity
│   ├── ruoyi-quartz/                 # 【定时任务】动态 Cron 管理
│   ├── ruoyi-generator/              # 【代码生成】Velocity 模板引擎
│   └── sql/                          # DDL 脚本（5 个）
│       ├── ry_20260417.sql           # 若依系统基础表（20 张）
│       ├── ai_init.sql               # AI 模块表（7 张 + 种子数据）
│       └── migration_*.sql           # 增量迁移脚本
│
├── agent-service/                    # Python FastAPI Agent 服务
│   ├── main.py                       # Uvicorn 启动入口
│   ├── requirements.txt              # Python 依赖（42 个包）
│   ├── .env / .env.example           # 环境配置模板
│   ├── app/
│   │   ├── __init__.py               # Settings + create_app() 工厂方法
│   │   ├── health.py                 # 启动自检（依赖/Embedding/Milvus/RAG）
│   │   ├── internal_client.py        # Spring Boot 内部接口客户端
│   │   ├── llm/
│   │   │   ├── client.py             # LLMClient（OpenAI + Ollama，流式+非流式）
│   │   │   └── exceptions.py         # LLMException 异常体系
│   │   ├── rag/                      # RAG 子系统
│   │   │   ├── embedding_service.py  # Embedding（sentence-transformers / OpenAI / Ollama）
│   │   │   ├── chunk_service.py      # 文本分块（段落→句子→重叠合并）
│   │   │   ├── milvus_service.py     # Milvus 向量库（docker / lite 模式）
│   │   │   ├── index_service.py      # 索引编排（分块→向量化→入库）
│   │   │   └── schemas.py            # Pydantic 模型
│   │   ├── mcp/                      # MCP 工具协议
│   │   │   ├── registry.py           # 工具注册表
│   │   │   ├── tool_executor.py      # 工具执行器（指数退避重试）
│   │   │   ├── schema_validator.py   # JSON Schema 校验
│   │   │   ├── connection_manager.py # HTTP 连接池
│   │   │   └── models.py             # JSON-RPC 2.0 协议模型
│   │   └── loaders/                  # 文档加载器（7 种格式）
│   │       ├── pdf_loader.py         # PDF (PyMuPDF)
│   │       ├── docx_loader.py        # Word (python-docx + lxml 降级)
│   │       ├── doc_loader.py         # .doc (textract → antiword → LibreOffice)
│   │       ├── excel_loader.py       # Excel (openpyxl)
│   │       ├── ppt_loader.py         # PPT (python-pptx)
│   │       ├── markdown_loader.py    # Markdown
│   │       └── txt_loader.py         # 纯文本（自动编码检测）
│   ├── api/
│   │   ├── routes.py                 # 核心路由：/chat, /chat/stream, /rag, /embed
│   │   ├── knowledge.py              # 知识库路由：index, delete, stats
│   │   ├── preview.py                # 文档预览路由
│   │   └── tools.py                  # MCP 工具调用路由
│   ├── agents/                       # Agent 实现（LangChain）
│   │   ├── planner.py                # 意图路由 Agent
│   │   ├── code_agent.py             # 代码生成 Agent
│   │   ├── rag_agent.py              # RAG 检索 Agent
│   │   └── review_agent.py           # 代码审查 Agent
│   ├── workflows/
│   │   └── graph.py                  # LangGraph 工作流定义
│   ├── tools/                        # 工具实现
│   │   ├── file_tool.py              # 文件系统工具
│   │   ├── github_tool.py            # GitHub API 工具
│   │   └── mysql_tool.py             # MySQL 查询工具
│   └── rag/                          # RAG 组件
│       ├── embedding.py              # 旧版 Embedding（已废弃）
│       ├── retriever.py              # ChromaDB 检索器
│       └── reranker.py               # BM25 重排序
│
├── frontend/                         # Vue3 前端
│   ├── vite.config.ts                # Vite 配置（代理到 8080/8000）
│   ├── package.json                  # 前端依赖
│   └── src/
│       ├── views/ai/                 # AI 功能页面
│       │   ├── chat/index.vue        # 对话页面（SSE 流式聊天）
│       │   ├── agent/index.vue       # Agent 管理
│       │   ├── model/index.vue       # 模型配置
│       │   ├── tool/index.vue        # 工具管理
│       │   └── knowledge/            # 知识库管理/检索
│       ├── api/ai/                   # AI API 封装
│       ├── components/               # 21 个通用组件
│       ├── store/modules/            # Pinia 状态管理
│       └── router/                   # 动态路由 + 权限控制
│
├── docs/                             # 项目文档
│   ├── 毕业答辩PPT.md                 # 毕业答辩演示文稿
│   ├── phases/
│   │   ├── 功能模块开发任务拆解.md      # 10 个模块任务拆解
│   │   └── 若依框架开发规范.md          # 编码规范
│   └── superpowers/plans+specs/      # 开发计划与设计文档
│
└── data/                             # 运行时数据目录
    └── milvus.db                     # Milvus Lite 本地向量库
```

## 数据库设计

### AI 模块表（7 张）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `ai_conversation` | 对话会话 | conversation_id, user_id, title, agent_id, model_id, status |
| `ai_message` | 对话消息 | message_id, conversation_id, role, content(LONGTEXT), token_count, model_name |
| `agent_config` | Agent 配置 | agent_id, agent_name, agent_type, system_prompt, tools_json, temperature |
| `ai_model` | AI 模型配置 | model_id, model_name, provider, api_key, base_url, max_tokens |
| `ai_tool` | AI 工具注册 | tool_id, tool_name, tool_type, server_url, input_schema(JSON) |
| `ai_tool_template` | 工具模板 | 7 个预置模板（file_reader, mysql_query, kb_search 等） |
| `kb_knowledge` | 知识库 | kb_id, name, description, kb_type, embedding_model |
| `kb_document` | 知识库文档 | doc_id, kb_id, file_name, content_text(LONGTEXT), vector_ids(JSON) |

### 系统管理表（20 张）

sys_user, sys_role, sys_menu, sys_dept, sys_post, sys_dict_type, sys_dict_data, sys_config, sys_notice, sys_logininfor, sys_oper_log 等标准 RBAC 表。

## API 接口

### 对话聊天

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai/chat/conversations` | 分页查询会话列表 |
| `GET` | `/ai/chat/conversations/{id}` | 获取会话详情 |
| `POST` | `/ai/chat/conversations` | 创建会话 |
| `PUT` | `/ai/chat/conversations` | 更新会话 |
| `DELETE` | `/ai/chat/conversations/{id}` | 删除会话及其消息 |
| `PUT` | `/ai/chat/conversations/{id}/rename` | 重命名会话 |
| `POST` | `/ai/chat/conversations/{id}/generate-title` | 自动生成标题 |
| `GET` | `/ai/chat/conversations/{id}/messages` | 获取会话消息列表 |

### 流式聊天（FastAPI）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/chat/stream` | **SSE 流式聊天**（需 JWT），逐 Token 输出 |
| `POST` | `/api/v1/chat` | 非流式聊天 |
| `POST` | `/api/v1/rag` | RAG 检索增强问答 |
| `POST` | `/api/v1/embed` | 文本向量化 |

### Agent 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai/agent/list` | 分页查询 Agent 列表 |
| `GET` | `/ai/agent/{id}` | 获取 Agent 详情 |
| `POST` | `/ai/agent` | 创建 Agent |
| `PUT` | `/ai/agent` | 更新 Agent |
| `DELETE` | `/ai/agent/{ids}` | 删除 Agent |
| `POST` | `/ai/agent/execute` | 同步执行 Agent 任务 |
| `POST` | `/ai/agent/submit` | 提交异步 Agent 任务 |
| `GET` | `/ai/agent/status/{taskId}` | 查询任务状态 |
| `GET` | `/ai/agent/enabled` | 获取已启用 Agent 列表 |

### 模型管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai/model/list` | 分页查询模型列表 |
| `GET` | `/ai/model/{id}` | 获取模型详情 |
| `POST` | `/ai/model` | 创建模型配置 |
| `PUT` | `/ai/model` | 更新模型配置 |
| `DELETE` | `/ai/model/{ids}` | 删除模型 |
| `GET` | `/ai/model/enabled` | 获取已启用模型列表 |
| `GET` | `/ai/model/test/{id}` | 测试模型连接 |

### 工具管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai/tool/list` | 分页查询工具列表 |
| `GET` | `/ai/tool/{id}` | 获取工具详情 |
| `POST` | `/ai/tool` | 创建工具 |
| `PUT` | `/ai/tool` | 更新工具 |
| `DELETE` | `/ai/tool/{ids}` | 删除工具 |
| `POST` | `/ai/tool/invoke/{id}` | 调用工具 |
| `POST` | `/ai/tool/test/{id}` | 测试工具连接 |
| `GET` | `/ai/tool/template/list` | 获取工具模板列表 |

### 知识库管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai/kb/list` | 分页查询知识库列表 |
| `POST` | `/ai/kb` | 创建知识库 |
| `PUT` | `/ai/kb` | 更新知识库 |
| `DELETE` | `/ai/kb/{ids}` | 删除知识库 |
| `POST` | `/ai/kb/upload` | 上传文档到知识库 |
| `GET` | `/ai/kb/{id}/doc/list` | 查询知识库文档列表 |
| `DELETE` | `/ai/kb/doc/{docId}` | 删除文档 |
| `POST` | `/ai/kb/search` | 知识库检索 |
| `GET` | `/ai/document/{docId}/file` | 下载文档文件 |
| `POST` | `/api/v1/knowledge/process` | 文档处理入库（FastAPI） |
| `POST` | `/api/v1/knowledge/index` | 文本内容索引（FastAPI） |

### 内部接口（FastAPI ↔ Spring Boot）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| `POST` | `/ai/internal/auth/token` | X-Internal-Secret | 获取内部 JWT |
| `GET` | `/ai/internal/conversation/{id}` | X-Internal-Token | 获取会话配置 |
| `POST` | `/ai/internal/conversation` | X-Internal-Token | 创建会话 |
| `POST` | `/ai/internal/message/save` | X-Internal-Token | 保存消息 |

## 快速开始

### 前置要求

- **Java** 17+
- **Python** 3.12+
- **Node.js** 20+ / pnpm
- **MySQL** 8.0+
- **Redis** 7.0+

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4;"

# 导入基础表 + 种子数据
mysql -u root -p ry-vue < backend/sql/ry_20260417.sql

# 导入 AI 模块表
mysql -u root -p ry-vue < backend/sql/ai_init.sql

# 执行增量迁移
mysql -u root -p ry-vue < backend/sql/migration_add_conversation_agent_model.sql
mysql -u root -p ry-vue < backend/sql/migration_add_kb_document_process_fields.sql
```

### 2. 后端启动

```bash
cd backend

# 修改 application-druid.yml 中的数据库连接信息

# 启动
mvn clean install -DskipTests
cd ruoyi-admin
mvn spring-boot:run
```

Spring Boot 默认启动在 **8080** 端口。  
管理后台账号：`admin` / `admin123`  
Swagger 文档：`http://localhost:8080/swagger-ui.html`

### 3. Agent 服务启动

```bash
cd agent-service

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填写 LLM API Key
# 关键配置：
#   OPENAI_API_KEY=sk-xxx
#   SPRING_BOOT_URL=http://localhost:8080
#   JWT_SECRET=abcdefghijklmnopqrstuvwxyz  # 与 Spring Boot token.secret 一致

# 启动
python main.py
```

FastAPI 服务默认启动在 **8000** 端口。  
API 文档：`http://localhost:8000/docs`

### 4. 前端启动

```bash
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

Vite 开发服务器默认启动在 **5173** 端口，代理配置：
- `/api/v1` → `localhost:8000`（FastAPI Agent）
- `/dev-api` → `localhost:8080`（Spring Boot）

访问 `http://localhost:5173` 即可使用。

## 流式聊天请求全流程

```
1. 用户输入消息 → Vue chat/index.vue handleSend()
   │
2. 若无会话 → POST /dev-api/ai/chat/conversations (Spring Boot 创建会话)
   │
3. fetch POST /api/v1/chat/stream (直连 FastAPI 8000)
   │  Body: { message, conversationId, agentId, modelId }
   │  Header: Authorization: Bearer <user_jwt>
   │
4. FastAPI routes.py → chat_stream()
   │  · 验证用户 JWT（HS512，共享密钥）
   │  · 获取会话配置 → GET /ai/internal/conversation/{id} (Spring Boot)
   │    返回: { agent: {systemPrompt, tools[], ...}, model: {provider, modelName, baseUrl, apiKey, ...}, history: [...] }
   │  · 构建增强 System Prompt（Agent提示词 + 工具定义 + RAG上下文）
   │  · 组装消息列表: [system, ...history, user_message]
   │
5. LLMClient.chat_stream(messages)
   │  · OpenAI: POST {base_url}/v1/chat/completions { model, messages, temperature, max_tokens, stream: true }
   │  · Ollama:  POST {base_url}/api/chat { model, messages, stream: true, options: {...} }
   │
6. LLM 返回 SSE Token → FastAPI 封装为:
   │  event: token / data: {"type":"token","content":"你"}
   │  event: done  / data: {"type":"done","content":"你好！..."}
   │
7. 前端 ReadableStream 读取 → 实时更新气泡内容
   │
8. 流结束后 → POST /ai/internal/message/save (保存用户消息 + AI 回复到 ai_message 表)
```

## 模块依赖关系

```
ruoyi-admin（应用入口）
├── ruoyi-framework（安全/认证/中间件）
│   └── ruoyi-system（用户/角色/菜单 CRUD）
│       └── ruoyi-common（工具类/注解/基础实体）
├── ruoyi-ai（AI 业务逻辑）
│   └── ruoyi-common
├── ruoyi-quartz（定时任务）
│   └── ruoyi-common
└── ruoyi-generator（代码生成）
    └── ruoyi-common
```

## 配置说明

### 关键配置项

| 配置 | 位置 | 说明 |
|------|------|------|
| 数据库连接 | `application-druid.yml` | MySQL 连接、Druid 连接池参数 |
| Redis 连接 | `application.yml` | Redis 地址、端口、密码 |
| JWT 密钥 | `application.yml` → `token.secret` | 需与 Agent `.env` 中的 `JWT_SECRET` 一致 |
| Agent 地址 | `application.yml` → `ai.agent.base-url` | FastAPI 服务地址 |
| 内部密钥 | `application.yml` → `ai.internal.secret` | FastAPI 调用 Spring Boot 的预共享密钥 |
| LLM API Key | `agent-service/.env` → `OPENAI_API_KEY` | 大模型 API 密钥 |
| Embedding | `agent-service/.env` → `EMBEDDING_PROVIDER` | sentence-transformers / openai / ollama |
| Milvus | `agent-service/.env` → `MILVUS_MODE` | docker（远程）/ lite（本地文件） |
| Vite 代理 | `frontend/vite.config.ts` | 前端开发代理到 8080/8000 |

## 支持的 LLM 提供商

通过 OpenAI 兼容 API 支持以下模型提供商：

- **OpenAI** — `base_url: https://api.openai.com`
- **DeepSeek** — `base_url: https://api.deepseek.com`
- **通义千问 (Qwen)** — `base_url: https://dashscope.aliyuncs.com/compatible-mode/v1`
- **硅基流动 (SiliconFlow)** — `base_url: https://api.siliconflow.cn`
- **Ollama** — `base_url: http://localhost:11434`（本地部署）

只需在管理后台配置 `provider`、`base_url`、`api_key`、`model_name` 四个字段即可接入。

## 支持的文档格式

知识库文档上传支持 7 种格式：

| 格式 | 扩展名 | 解析方式 |
|------|--------|---------|
| PDF | `.pdf` | PyMuPDF (fitz) |
| Word | `.docx` | python-docx + lxml XML 降级 |
| Word 97 | `.doc` | textract → antiword → LibreOffice 多级回退 |
| Excel | `.xlsx`, `.xls` | openpyxl |
| PowerPoint | `.pptx` | python-pptx（含备注页） |
| Markdown | `.md` | 去格式化标记，保留纯文本 |
| 纯文本 | `.txt`, `.csv`, `.log` | 自动编码检测 (UTF-8/GBK/GB2312) |

## 开发规范

本项目遵循若依框架开发规范，详见 `docs/phases/若依框架开发规范.md`。核心约定：

- **分层架构**：Controller → Service → Mapper，不可跨层调用
- **参数校验**：Controller 层 DTO + `@Valid`，Service 层不做基础入参校验
- **统一响应**：`AjaxResult` / `TableDataInfo`，不直接返回实体
- **Controller 位置**：`ruoyi-admin/web/controller` 包下
- **DTO 位置**：对应模块的 `domain.dto` 包下
- **SQL 规范**：表名 `ai_` 前缀，字段下划线命名，必备 `create_by/create_time/update_by/update_time`

## 待优化项

- [ ] Agent 工作流（LangGraph）接入实际聊天流程（当前仅作为 Prompt 增强）
- [ ] 工具调用从文字描述升级为原生 Function Calling
- [x] SSE 流式聊天 ~~（已从 Spring Boot 旧 `/ai/chat/send` 接口迁移）~~
- [ ] 异步 Agent 任务队列（当前为简单 Thread，待接入消息队列）
- [ ] 容器化部署（Docker Compose）
- [ ] 前端国际化 (i18n)

## License

基于 RuoYi-Vue 框架构建，遵循 MIT 协议。
