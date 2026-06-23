# AI 聊天流式架构重构 — 设计方案

**日期**: 2026-06-23
**状态**: 已确认

---

## 1. 背景与目标

### 当前问题

```
Vue → Spring Boot (POST /ai/chat/send, AjaxResult) → FastAPI (POST /api/v1/chat) → LLM
```

- `AiChatController.send()` 返回 `AjaxResult`，本质是同步 JSON 接口
- 虽然 FastAPI 有 `/chat/stream` 端点，但是**假流式**（同步获取完整回复 → 逐字符 sleep 发送）
- Spring Boot 在中间层破坏了 SSE 协议，Vue 前端 AI 气泡内容为空
- `FastApiClient` 虽然有 `streamPost()` 方法但从未被调用

### 目标

- Vue 直接连接 FastAPI 进行 SSE 流式聊天
- Spring Boot 保留所有 CRUD 能力（Agent/Model/Knowledge/会话/消息/用户权限）
- FastAPI 负责 SSE 流式输出、Agent 执行、Prompt 组装、Tool Calling、RAG 检索、模型调用
- FastAPI 不直接操作数据库，通过 Spring Boot 内部接口获取配置和保存消息
- 保留 RuoYi 框架不变

---

## 2. 最终架构

```
┌─────────────────────────────────────────────────────────┐
│                       Vue 前端                           │
│                                                         │
│   CRUD 操作 ────────► /dev-api/* ──────► Spring Boot    │
│   (Agent/Model/会话/消息/知识库)                          │
│                                                         │
│   AI 聊天(SSE) ────► /api/v1/chat/stream ──► FastAPI    │
└─────────────────────────────────────────────────────────┘

聊天流（新）：
Vue ──SSE──► FastAPI ──stream:true──► LLM
                  │
                  ├── 启动时 ──► POST /ai/internal/auth/token     → 获取内部 JWT
                  ├── 请求时 ──► GET /ai/internal/conversation/{id} → 获取 Agent/Model/历史
                  └── 完成后 ──► POST /ai/internal/message/save   → 保存用户消息 + AI 回复

CRUD（不变）：
Vue ──HTTP──► Spring Boot ──► MySQL
```

Vite 代理配置：
- `/dev-api` → `http://localhost:8080`（Spring Boot）
- `/api/v1` → `http://localhost:8000`（FastAPI）

生产环境由 Nginx 负责路由。

---

## 3. 数据流（详细步骤）

```
1. Vue 发起 SSE 连接
   POST /api/v1/chat/stream
   Body: { conversationId, message, agentId, modelId }
   Header: Authorization: Bearer <用户JWT>

2. FastAPI 验证用户 JWT（共享 secret HS512）
   → 解析出 userId

3. FastAPI 获取内部 JWT（如已过期则刷新，缓存复用）
   POST /ai/internal/auth/token
   Header: X-Internal-Secret: <预共享密钥>
   ← { token: "<短期JWT>", expiresIn: 1800 }

4. FastAPI 获取会话配置
   GET /ai/internal/conversation/{conversationId}
   Header: X-Internal-Token: <内部JWT>
   ← { conversationId, title, agent: {...}, model: {...}, history: [...] }

5. FastAPI 组装 Prompt + 执行 Agent
   → 根据 agent.type 选择 Agent 策略

6. FastAPI 调用 LLM 流式输出
   → POST /v1/chat/completions { stream: true }
   → StreamingResponse(media_type="text/event-stream")

7. SSE 事件推送
   event: token
   data: {"content": "你"}

   event: token
   data: {"content": "好"}

   event: done
   data: [DONE]

8. FastAPI 异步回调保存消息（流结束后）
   POST /ai/internal/message/save
   Header: X-Internal-Token: <内部JWT>
   Body: { conversationId, userId, userMessage, aiMessage, tokenUsage }
```

---

## 4. Spring Boot 变更

### 4.1 新增 `AiInternalController`

位置：`ruoyi-admin/src/main/java/com/aiplatform/web/controller/ai/AiInternalController.java`

| 方法 | 路径 | 认证方式 | 说明 |
|------|------|----------|------|
| POST | `/ai/internal/auth/token` | `X-Internal-Secret` | 签发短期内部 JWT（30 分钟） |
| GET | `/ai/internal/conversation/{id}` | `X-Internal-Token` | 返回会话配置 + Agent + Model + 历史消息 |
| POST | `/ai/internal/message/save` | `X-Internal-Token` | 保存用户消息和 AI 回复 |

### 4.2 新增 `InternalTokenFilter`

- 拦截路径：`/ai/internal/**`
- 逻辑：
  - 若请求头有 `X-Internal-Secret` 且匹配配置值 → 放行（仅用于 `/auth/token`）
  - 若请求头有 `X-Internal-Token` → 以 JWT 方式验证（同一 HS512 secret，使用不同 token 前缀 `internal:` 区分用户 token）
  - 其他情况 → 401

### 4.3 新增 `InternalTokenService`

- `generateToken()`: 签发短期 JWT（30 分钟有效期）
- `validateToken(token)`: 验证 JWT 有效性

### 4.4 `AiChatController` 变更

保留（不变）：
- `GET /ai/chat/conversations` — 会话列表
- `POST /ai/chat/conversations` — 创建会话
- `PUT /ai/chat/conversations` — 编辑会话
- `DELETE /ai/chat/conversations/{id}` — 删除会话
- `GET /ai/chat/conversations/{id}/messages` — 历史消息
- `PUT /ai/chat/conversations/{id}/rename` — 重命名
- `POST /ai/chat/conversations/{id}/generate-title` — 生成标题

废弃/删除：
- `POST /ai/chat/send` — 流式聊天已迁移至 FastAPI，不再使用

### 4.5 `SecurityConfig` 变更

- 新增 `InternalTokenFilter` 在 JWT Filter 之前，对 `/ai/internal/**` 路径生效
- `/ai/internal/**` 路径不经过用户 JWT 认证

### 4.6 配置新增

```yaml
# application.yml
ai:
  internal:
    secret: ${AI_INTERNAL_SECRET:internal-shared-secret}
    token-expire-time: 30   # 分钟
```

---

## 5. FastAPI 变更

### 5.1 重写 `POST /api/v1/chat/stream`

请求体：
```json
{
  "conversationId": 1,
  "message": "你好",
  "agentId": 1,
  "modelId": 1
}
```

请求头：`Authorization: Bearer <用户JWT>`

返回：`StreamingResponse(media_type="text/event-stream")`

实现流程：
1. 验证用户 JWT（用 `pyjwt` + HS512 + 共享 secret）
2. 获取内部 JWT（`InternalClient.get_token()`，带 TTL 缓存）
3. 调用 `InternalClient.get_conversation(conversationId)` 获取配置 + 历史
4. 组装 Prompt（系统 prompt + 历史消息 + 用户消息）
5. 调用 `LLMClient.chat_stream(messages, model_config)` 获取 AsyncGenerator
6. `StreamingResponse` 逐块输出 SSE 事件
7. 流结束后异步调用 `InternalClient.save_message()` 保存消息

### 5.2 新增 `app/internal_client.py`

```python
class InternalClient:
    def __init__(self, base_url: str, shared_secret: str, jwt_secret: str):
        ...
    
    async def get_token(self) -> str:
        """POST /ai/internal/auth/token → 返回内部 JWT，TTL 缓存"""
    
    async def get_conversation(self, id: int) -> dict:
        """GET /ai/internal/conversation/{id} → 返回会话配置"""
    
    async def save_message(self, data: dict) -> None:
        """POST /ai/internal/message/save → 保存消息"""
```

### 5.3 LLM 客户端改造

```python
# app/llm/client.py — 新增真正的流式方法
async def chat_stream(self, messages: list, model_config: dict) -> AsyncGenerator[str, None]:
    """调用 LLM API (stream: true)，逐 token yield"""
```

关键变更：`_call_openai_compatible()` 增加 `stream` 参数，`stream=True` 时逐行解析 SSE 返回。

### 5.4 配置新增

```env
# agent-service/.env
JWT_SECRET=abcdefghijklmnopqrstuvwxyz
INTERNAL_SECRET=internal-shared-secret
SPRING_BOOT_URL=http://localhost:8080
```

---

## 6. Vue 前端变更

### 6.1 聊天视图 `views/ai/chat/index.vue`

`handleSend()` 改为：
- 使用 `fetch()` + `ReadableStream` 调用 `POST /api/v1/chat/stream`
- 携带 `Authorization: Bearer <token>` 请求头
- 创建 AI 消息空占位气泡，逐字追加 `content`
- 收到 `event: done` 后标记完成

### 6.2 `vite.config.ts`

```typescript
server: {
  proxy: {
    '/dev-api': { target: 'http://localhost:8080', changeOrigin: true },
    '/api/v1':  { target: 'http://localhost:8000', changeOrigin: true }  // 新增
  }
}
```

### 6.3 移除/废弃

- `api/ai/chat.js` 中的 `sendChat()` 不再使用（可保留代码但不调用）

---

## 7. 安全设计

### 7.1 三层认证

| 层级 | 路径 | 认证方式 | 密钥 |
|------|------|----------|------|
| 用户 → Spring Boot CRUD | `/ai/chat/**` 等 | JWT（用户登录） | application.yml `token.secret` |
| 用户 → FastAPI 聊天 | `/api/v1/chat/**` | JWT（直接验证） | 同一 secret |
| FastAPI → Spring Boot 内部 | `/ai/internal/**` | 内部 JWT（30 分钟） | 同一 secret |
| FastAPI → 获取内部 Token | `/ai/internal/auth/token` | `X-Internal-Secret` 预共享密钥 | application.yml `ai.internal.secret` |

### 7.2 Token 区分

- 用户 JWT：`subject` = `loginUserKey`（UUID），有效期 48 小时
- 内部 JWT：`subject` = `internal`，有效期 30 分钟
- 通过 `subject` 区分，防止用户 Token 访问内部接口

---

## 8. 错误处理

| 场景 | FastAPI 行为 | 前端表现 |
|------|-------------|---------|
| 用户 JWT 无效 | `event: error\ndata: {"code": 401, "message": "Unauthorized"}` | 跳转登录 |
| 会话不存在 | `event: error\ndata: {"code": 404, "message": "Conversation not found"}` | 提示用户 |
| 内部 Token 获取失败 | `event: error\ndata: {"code": 500, "message": "Internal service error"}` | 提示稍后重试 |
| LLM 调用超时 | `event: error\ndata: {"code": 504, "message": "LLM timeout"}` | 提示超时 |
| 消息保存失败 | `event: done` 正常结束 + 后台日志告警 | 聊天正常，消息可能丢失 |
| 网络中断 | `EventSource` 自动重连 / `fetch` ReadableStream 断开 | 提示断连 |

---

## 9. 变更文件清单

### Spring Boot（5 个新文件 + 3 个修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `AiInternalController.java` | 内部接口控制器 |
| 新增 | `InternalTokenFilter.java` | 内部 Token 过滤器 |
| 新增 | `InternalTokenService.java` | 内部 Token 签发/验证服务 |
| 新增 | `ConversationConfigDto.java` | 会话配置响应 DTO |
| 新增 | `MessageSaveRequestDto.java` | 消息保存请求 DTO |
| 修改 | `SecurityConfig.java` | 注册 InternalTokenFilter |
| 修改 | `AiChatController.java` | 废弃 `/send` 端点 |
| 修改 | `application.yml` | 新增 `ai.internal` 配置 |

### FastAPI（2 个新文件 + 2 个修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `app/internal_client.py` | Spring Boot 内部 HTTP 客户端 |
| 修改 | `api/routes.py` | 重写 `/chat/stream` |
| 修改 | `app/llm/client.py` | 新增 `chat_stream()` |
| 修改 | `.env` / `app/settings.py` | 新增配置项 |

### Vue 前端（2 个修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `src/views/ai/chat/index.vue` | `handleSend()` 改为 SSE 流式 |
| 修改 | `vite.config.ts` | 新增 `/api/v1` 代理 |

---

## 10. 测试方案

### 10.1 单元测试

- `InternalTokenService`：Token 签发/验证/过期
- `InternalTokenFilter`：认证放行/拒绝场景
- `InternalClient`：Token 缓存/过期刷新

### 10.2 集成测试

- FastAPI `/chat/stream` → 模拟 LLM 响应 → 验证 SSE 格式
- FastAPI → Spring Boot 内部接口调用链
- 消息保存端到端

### 10.3 手动测试

1. 启动 Spring Boot (8080) + FastAPI (8000) + Vue (dev server)
2. 登录 → 选择 Agent/Model → 发送消息
3. 验证 AI 气泡逐字显示
4. 刷新页面 → 验证历史消息从 Spring Boot 加载
5. 验证 Agent CRUD 不受影响
