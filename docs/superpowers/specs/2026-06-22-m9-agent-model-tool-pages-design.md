# M9: Agent/Model/Tool 管理页面 — 设计文档

**日期**: 2026-06-22
**范围**: Agent 管理、模型配置、工具管理 — 3 个 CRUD 页面 + API 层 + 路由
**方案**: A — 完全遵循现有 RuoYi-Vue3 模式 + TypeScript 类型

---

## 产物清单

```
frontend/src/
├── types/api/ai/
│   ├── agent.ts          # AgentConfig, AgentExecuteDto, AgentQueryParams
│   ├── model.ts          # AiModel, ModelConfigDto, ModelQueryParams
│   └── tool.ts           # AiTool, ToolQueryParams
├── api/ai/
│   ├── agent.ts          # 9 个 API 函数
│   ├── model.ts          # 7 个 API 函数
│   └── tool.ts           # 7 个 API 函数
├── views/ai/
│   ├── agent/
│   │   └── index.vue     # Agent 管理 + 执行面板
│   ├── model/
│   │   └── index.vue     # 模型配置 + 测试连接 + 设为默认
│   └── tool/
│       └── index.vue     # 工具管理 + 调用测试
└── router/
    └── index.ts          # 新增 3 条 dynamicRoutes
```

**不涉及现有文件修改**（chat、knowledge 保持不动）。

---

## 技术栈

- Vue 3 + Composition API (`<script setup lang="ts">`)
- Element Plus 2.13.x
- Axios via `@/utils/request`
- TypeScript 类型定义
- 权限指令 `v-hasPermi`

---

## 类型定义

### `types/api/ai/agent.ts`
- `AgentConfig extends BaseEntity` — agentId, agentName, agentType, description, modelId, systemPrompt, toolsJson, workflowJson, maxIterations, temperature, timeoutSeconds, status
- `AgentQueryParams extends PageDomain` — agentName?, agentType?, status?
- `AgentExecuteDto` — agentId, task, input?, async?

### `types/api/ai/model.ts`
- `AiModel extends BaseEntity` — modelId, modelName, displayName, provider, apiKey, baseUrl, modelType, maxTokens, temperature, isDefault, isEnabled
- `ModelConfigDto` — 同上 + remark?，modelId 可选
- `ModelQueryParams extends PageDomain` — modelName?, displayName?, provider?, modelType?, isEnabled?

### `types/api/ai/tool.ts`
- `AiTool extends BaseEntity` — toolId, toolName, displayName, toolType, description, serverUrl, inputSchema, outputSchema, authType, authConfig, timeoutMs, retryCount, isEnabled
- `ToolQueryParams extends PageDomain` — toolName?, toolType?, isEnabled?

---

## API 层

所有函数遵循 `request({ url, method, params/data })` 模式，返回 `Promise<AjaxResult<T>>` 或 `Promise<TableDataInfo<T>>`。

### `api/ai/agent.ts` (9 函数)
| 函数 | Method | URL |
|------|--------|-----|
| listAgent | GET | /ai/agent/list |
| getAgent | GET | /ai/agent/${id} |
| addAgent | POST | /ai/agent |
| updateAgent | PUT | /ai/agent |
| delAgent | DELETE | /ai/agent/${ids} |
| executeAgent | POST | /ai/agent/execute |
| submitAgent | POST | /ai/agent/submit |
| getTaskStatus | GET | /ai/agent/status/${taskId} |
| cancelTask | POST | /ai/agent/cancel/${taskId} |

### `api/ai/model.ts` (7 函数)
| 函数 | Method | URL |
|------|--------|-----|
| listModel | GET | /ai/model/list |
| getModel | GET | /ai/model/${id} |
| addModel | POST | /ai/model |
| updateModel | PUT | /ai/model |
| delModel | DELETE | /ai/model/${ids} |
| testModel | POST | /ai/model/test/${id} |
| setDefaultModel | PUT | /ai/model/set-default/${id} |

### `api/ai/tool.ts` (7 函数)
| 函数 | Method | URL |
|------|--------|-----|
| listTool | GET | /ai/tool/list |
| getTool | GET | /ai/tool/${id} |
| addTool | POST | /ai/tool |
| updateTool | PUT | /ai/tool |
| delTool | DELETE | /ai/tool/${ids} |
| invokeTool | POST | /ai/tool/invoke/${id} |
| testTool | POST | /ai/tool/test/${id} |

---

## 视图层

### Agent 管理 (`views/ai/agent/index.vue`)

**搜索表单**: agentName (输入框) + agentType (下拉: planner/rag/code/review/tool/custom) + status (下拉)
**表格列**: agentName | agentType | modelId | temperature | maxIterations | status | createTime | 操作
**操作按钮**:
- 修改 (v-hasPermi="['ai:agent:edit']")
- 删除 (v-hasPermi="['ai:agent:remove']")
- 执行 → 对话框：选择 agent + 输入 task + 同步/异步开关 → 显示结果
- 异步任务状态 → 小弹窗输入 taskId → 查询状态/取消 (v-hasPermi="['ai:agent:execute']")
**新增/编辑对话框**: 全部字段
**权限**: 新增 ai:agent:add, 编辑 ai:agent:edit, 删除 ai:agent:remove

### 模型配置 (`views/ai/model/index.vue`)

**搜索表单**: modelName (输入框) + provider (下拉) + modelType (下拉) + isEnabled (下拉)
**表格列**: modelName | displayName | provider | modelType | isDefault(标签) | isEnabled(标签) | createTime | 操作
**操作按钮**:
- 修改 (v-hasPermi="['ai:model:edit']")
- 删除 (v-hasPermi="['ai:model:remove']")
- 测试连接 (v-hasPermi="['ai:model:query']") → POST test → ElMessage 成功/失败
- 设为默认 (v-hasPermi="['ai:model:edit']") → PUT set-default → 刷新
**新增/编辑对话框**: 全部字段，apiKey 用 password 输入框
**权限**: 新增 ai:model:add, 编辑 ai:model:edit, 删除 ai:model:remove

### 工具管理 (`views/ai/tool/index.vue`)

**搜索表单**: toolName (输入框) + toolType (下拉) + isEnabled (下拉)
**表格列**: toolName | displayName | toolType | serverUrl | timeoutMs | isEnabled(标签) | createTime | 操作
**操作按钮**:
- 修改 (v-hasPermi="['ai:tool:edit']")
- 删除 (v-hasPermi="['ai:tool:remove']")
- 调用测试 (v-hasPermi="['ai:tool:execute']") → 弹窗输入 JSON params → POST invoke → 显示结果
- 连通测试 (v-hasPermi="['ai:tool:query']") → POST test → ElMessage
**新增/编辑对话框**: 全部字段，schema JSON 用 textarea
**权限**: 新增 ai:tool:add, 编辑 ai:tool:edit, 删除 ai:tool:remove

---

## 路由

在 `dynamicRoutes` 中新增 3 条（均包裹在 Layout 中）：

| Path | Name | Meta |
|------|------|------|
| /ai/agent | AiAgent | title: 'Agent管理', icon: 'cpu', permissions: ['ai:agent:query'] |
| /ai/model | AiModel | title: '模型配置', icon: 'cpu', permissions: ['ai:model:query'] |
| /ai/tool | AiTool | title: '工具管理', icon: 'tool', permissions: ['ai:tool:query'] |

---

## 验收标准

- [ ] Agent 管理页面 CRUD 正常，执行 Agent 能返回结果/taskId，异步任务状态查询可用
- [ ] 模型管理页面 CRUD 正常，测试连接显示成功/失败，设为默认生效
- [ ] 工具管理页面 CRUD 正常，调用测试能显示返回，连通测试有成功/失败反馈
- [ ] 三个页面在侧边栏菜单正确显示（需权限）
- [ ] 所有 API 调用通过 `@/utils/request` 走 `/dev-api` 代理到后端
