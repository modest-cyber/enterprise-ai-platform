# M9: Agent/Model/Tool 管理页面 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为企业 AI 平台前端创建 Agent/Model/Tool 三个 CRUD 管理页面及配套 API 层和类型定义

**Architecture:** 遵循现有若依-Vue3 模式：types 定义 → api 封装 → views 页面 → router 注册。所有代码参照 `views/ai/knowledge/index.vue` 和 `api/ai/knowledge.ts` 的风格

**Tech Stack:** Vue 3 + TypeScript (`<script setup lang="ts">`) + Element Plus 2.13 + Axios via `@/utils/request`

## Global Constraints

- API 函数不标注 TypeScript 参数/返回值类型，与 `knowledge.ts` 风格一致
- 视图层使用 `as any` 处理 reactive 对象，与 `knowledge/index.vue` 一致
- 权限字符串必须匹配后端 `@PreAuthorize` 注解
- 不修改任何现有文件（`router/index.ts` 仅追加，`types/api/index.ts` 仅追加 export）
- 删除确认使用 `proxy.$modal.confirm()`，消息使用 `proxy.$modal.msgSuccess()/msgError()`
- 表单验证使用 `proxy.$refs['formRef'].validate()`
- 所有新文件使用 LF 行尾

---

### Task 1: TypeScript 类型定义

**Files:**
- Create: `frontend/src/types/api/ai/agent.ts`
- Create: `frontend/src/types/api/ai/model.ts`
- Create: `frontend/src/types/api/ai/tool.ts`
- Modify: `frontend/src/types/api/index.ts` — 追加 AI 模块 export

**Produces:**
- `AgentConfig`, `AgentExecuteDto`, `AgentQueryParams`
- `AiModel`, `ModelConfigDto`, `ModelQueryParams`
- `AiTool`, `ToolQueryParams`

- [ ] **Step 1: 创建 `frontend/src/types/api/ai/agent.ts`**

```typescript
import type { BaseEntity, PageDomain } from '../common'

/** Agent 配置实体 */
export interface AgentConfig extends BaseEntity {
  agentId: number
  agentName: string
  agentType: string
  description: string
  modelId: number
  systemPrompt: string
  toolsJson: string
  workflowJson: string
  maxIterations: number
  temperature: number
  timeoutSeconds: number
  status: number
}

/** Agent 执行入参 DTO */
export interface AgentExecuteDto {
  agentId: number
  task: string
  input?: string
  async?: boolean
}

/** Agent 查询参数 */
export interface AgentQueryParams extends PageDomain {
  agentName?: string
  agentType?: string
  status?: number
}
```

- [ ] **Step 2: 创建 `frontend/src/types/api/ai/model.ts`**

```typescript
import type { BaseEntity, PageDomain } from '../common'

/** 大模型配置实体 */
export interface AiModel extends BaseEntity {
  modelId: number
  modelName: string
  displayName: string
  provider: string
  apiKey: string
  baseUrl: string
  modelType: string
  maxTokens: number
  temperature: number
  isDefault: number
  isEnabled: number
}

/** 大模型创建/更新 DTO */
export interface ModelConfigDto {
  modelId?: number
  modelName: string
  displayName: string
  provider: string
  apiKey: string
  baseUrl: string
  modelType: string
  maxTokens: number
  temperature: number
  isDefault: number
  isEnabled: number
  remark?: string
}

/** 大模型查询参数 */
export interface ModelQueryParams extends PageDomain {
  modelName?: string
  displayName?: string
  provider?: string
  modelType?: string
  isEnabled?: number
}
```

- [ ] **Step 3: 创建 `frontend/src/types/api/ai/tool.ts`**

```typescript
import type { BaseEntity, PageDomain } from '../common'

/** 工具配置实体 */
export interface AiTool extends BaseEntity {
  toolId: number
  toolName: string
  displayName: string
  toolType: string
  description: string
  serverUrl: string
  inputSchema: string
  outputSchema: string
  authType: string
  authConfig: string
  timeoutMs: number
  retryCount: number
  isEnabled: number
}

/** 工具查询参数 */
export interface ToolQueryParams extends PageDomain {
  toolName?: string
  toolType?: string
  isEnabled?: number
}
```

- [ ] **Step 4: 修改 `frontend/src/types/api/index.ts`，在文件末尾追加 AI 模块导出**

在 `export * from "./tool/gen";` 之后追加：

```typescript
// AI 模块
export * from "./ai/agent";
export * from "./ai/model";
export * from "./ai/tool";
```

- [ ] **Step 5: 提交**

```bash
git add frontend/src/types/api/ai/agent.ts frontend/src/types/api/ai/model.ts frontend/src/types/api/ai/tool.ts frontend/src/types/api/index.ts
git commit -m "feat: add AI module TypeScript type definitions for Agent/Model/Tool"
```

---

### Task 2: API 层

**Files:**
- Create: `frontend/src/api/ai/agent.ts`
- Create: `frontend/src/api/ai/model.ts`
- Create: `frontend/src/api/ai/tool.ts`

**Consumes:** Task 1 的类型定义（类型文件存在即可，API 函数不直接引用）

**Produces:** 23 个 API 函数，供 Task 3/4/5 的视图层调用

- [ ] **Step 1: 创建 `frontend/src/api/ai/agent.ts`**

```typescript
import request from '@/utils/request'

// 查询Agent列表
export function listAgent(query) {
  return request({ url: '/ai/agent/list', method: 'get', params: query })
}

// 查询Agent详情
export function getAgent(id) {
  return request({ url: '/ai/agent/' + id, method: 'get' })
}

// 新增Agent
export function addAgent(data) {
  return request({ url: '/ai/agent', method: 'post', data })
}

// 修改Agent
export function updateAgent(data) {
  return request({ url: '/ai/agent', method: 'put', data })
}

// 删除Agent
export function delAgent(ids) {
  return request({ url: '/ai/agent/' + ids, method: 'delete' })
}

// 同步执行Agent
export function executeAgent(data) {
  return request({ url: '/ai/agent/execute', method: 'post', data })
}

// 异步提交Agent任务
export function submitAgent(data) {
  return request({ url: '/ai/agent/submit', method: 'post', data })
}

// 查询异步任务状态
export function getTaskStatus(taskId) {
  return request({ url: '/ai/agent/status/' + taskId, method: 'get' })
}

// 取消异步任务
export function cancelTask(taskId) {
  return request({ url: '/ai/agent/cancel/' + taskId, method: 'post' })
}
```

- [ ] **Step 2: 创建 `frontend/src/api/ai/model.ts`**

```typescript
import request from '@/utils/request'

// 查询模型列表
export function listModel(query) {
  return request({ url: '/ai/model/list', method: 'get', params: query })
}

// 查询模型详情
export function getModel(id) {
  return request({ url: '/ai/model/' + id, method: 'get' })
}

// 新增模型
export function addModel(data) {
  return request({ url: '/ai/model', method: 'post', data })
}

// 修改模型
export function updateModel(data) {
  return request({ url: '/ai/model', method: 'put', data })
}

// 删除模型
export function delModel(ids) {
  return request({ url: '/ai/model/' + ids, method: 'delete' })
}

// 测试模型连接
export function testModel(id) {
  return request({ url: '/ai/model/test/' + id, method: 'post' })
}

// 设为默认模型
export function setDefaultModel(id) {
  return request({ url: '/ai/model/set-default/' + id, method: 'put' })
}
```

- [ ] **Step 3: 创建 `frontend/src/api/ai/tool.ts`**

```typescript
import request from '@/utils/request'

// 查询工具列表
export function listTool(query) {
  return request({ url: '/ai/tool/list', method: 'get', params: query })
}

// 查询工具详情
export function getTool(id) {
  return request({ url: '/ai/tool/' + id, method: 'get' })
}

// 新增工具
export function addTool(data) {
  return request({ url: '/ai/tool', method: 'post', data })
}

// 修改工具
export function updateTool(data) {
  return request({ url: '/ai/tool', method: 'put', data })
}

// 删除工具
export function delTool(ids) {
  return request({ url: '/ai/tool/' + ids, method: 'delete' })
}

// 调用工具
export function invokeTool(id, params) {
  return request({ url: '/ai/tool/invoke/' + id, method: 'post', data: params })
}

// 测试工具连通性
export function testTool(id) {
  return request({ url: '/ai/tool/test/' + id, method: 'post' })
}
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/api/ai/agent.ts frontend/src/api/ai/model.ts frontend/src/api/ai/tool.ts
git commit -m "feat: add Agent/Model/Tool API functions"
```

---

### Task 3: Agent 管理页面

**Files:**
- Create: `frontend/src/views/ai/agent/index.vue`

**Consumes:**
- `listAgent`, `getAgent`, `addAgent`, `updateAgent`, `delAgent`, `executeAgent`, `submitAgent`, `getTaskStatus`, `cancelTask` from `@/api/ai/agent`

**Produces:** 完整的 Agent CRUD 页面，含搜索表单、表格、新增/编辑对话框、执行面板、异步任务状态查询

- [ ] **Step 1: 创建 `frontend/src/views/ai/agent/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="Agent名称" prop="agentName">
        <el-input v-model="queryParams.agentName" placeholder="请输入Agent名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Agent类型" prop="agentType">
        <el-select v-model="queryParams.agentType" placeholder="Agent类型" clearable>
          <el-option label="Planner" value="planner" />
          <el-option label="RAG" value="rag" />
          <el-option label="Code" value="code" />
          <el-option label="Review" value="review" />
          <el-option label="Tool" value="tool" />
          <el-option label="Custom" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:agent:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['ai:agent:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:agent:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="agentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="Agent名称" align="center" prop="agentName" :show-overflow-tooltip="true" />
      <el-table-column label="Agent类型" align="center" prop="agentType" width="100" />
      <el-table-column label="关联模型ID" align="center" prop="modelId" width="100" />
      <el-table-column label="温度" align="center" prop="temperature" width="80" />
      <el-table-column label="最大迭代" align="center" prop="maxIterations" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:agent:edit']">编辑</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:agent:remove']">删除</el-button>
          <el-button link type="primary" icon="VideoPlay" @click="handleExecute(scope.row)" v-hasPermi="['ai:agent:execute']">执行</el-button>
          <el-button link type="primary" icon="Clock" @click="handleTaskStatus" v-hasPermi="['ai:agent:query']">任务状态</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="agentRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Agent名称" prop="agentName">
              <el-input v-model="form.agentName" placeholder="请输入Agent名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Agent类型" prop="agentType">
              <el-select v-model="form.agentType" placeholder="请选择" style="width:100%">
                <el-option label="Planner" value="planner" />
                <el-option label="RAG" value="rag" />
                <el-option label="Code" value="code" />
                <el-option label="Review" value="review" />
                <el-option label="Tool" value="tool" />
                <el-option label="Custom" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联模型ID" prop="modelId">
              <el-input-number v-model="form.modelId" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大迭代" prop="maxIterations">
              <el-input-number v-model="form.maxIterations" :min="1" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(秒)" prop="timeoutSeconds">
              <el-input-number v-model="form.timeoutSeconds" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="3" placeholder="请输入系统提示词" />
        </el-form-item>
        <el-form-item label="工具JSON" prop="toolsJson">
          <el-input v-model="form.toolsJson" type="textarea" :rows="2" placeholder='如 ["1","2"]' />
        </el-form-item>
        <el-form-item label="工作流JSON" prop="workflowJson">
          <el-input v-model="form.workflowJson" type="textarea" :rows="2" placeholder="LangGraph workflow JSON" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 执行Agent对话框 -->
    <el-dialog title="执行Agent" v-model="executeOpen" width="600px" append-to-body>
      <el-form ref="executeRef" :model="executeForm" :rules="executeRules" label-width="80px">
        <el-form-item label="Agent" prop="agentId">
          <el-input :value="executeForm.agentName" disabled />
        </el-form-item>
        <el-form-item label="任务描述" prop="task">
          <el-input v-model="executeForm.task" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="输入参数" prop="input">
          <el-input v-model="executeForm.input" type="textarea" :rows="2" placeholder="JSON格式输入参数（可选）" />
        </el-form-item>
        <el-form-item label="执行方式">
          <el-radio-group v-model="executeForm.async">
            <el-radio :value="false">同步执行</el-radio>
            <el-radio :value="true">异步提交</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div v-if="executeResult" class="execute-result">
        <el-alert :title="executeResult" type="success" :closable="false" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitExecute" :loading="executing">执 行</el-button>
          <el-button @click="executeOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务状态查询对话框 -->
    <el-dialog title="异步任务状态" v-model="taskStatusOpen" width="500px" append-to-body>
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务ID">
          <el-input v-model="taskForm.taskId" placeholder="请输入任务ID" />
        </el-form-item>
      </el-form>
      <div v-if="taskResult" class="execute-result">
        <el-alert :title="taskResult" :type="taskResultType" :closable="false" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="queryTaskStatus">查询状态</el-button>
          <el-button type="warning" @click="cancelTaskHandler" v-hasPermi="['ai:agent:execute']">取消任务</el-button>
          <el-button @click="taskStatusOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Agent">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listAgent, getAgent, addAgent, updateAgent, delAgent, executeAgent, submitAgent, getTaskStatus, cancelTask } from '@/api/ai/agent'
const { proxy } = getCurrentInstance() as any

const agentList = ref([])
const open = ref(false)
const loading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

// 执行相关
const executeOpen = ref(false)
const executing = ref(false)
const executeResult = ref('')

// 任务状态相关
const taskStatusOpen = ref(false)
const taskResult = ref('')
const taskResultType = ref('info')

const data = reactive({
  form: {} as any,
  queryParams: { pageNum: 1, pageSize: 10, agentName: undefined, agentType: undefined, status: undefined } as any,
  rules: {
    agentName: [{ required: true, message: 'Agent名称不能为空', trigger: 'blur' }],
    agentType: [{ required: true, message: 'Agent类型不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

const executeData = reactive({
  executeForm: {} as any,
  executeRules: {
    task: [{ required: true, message: '任务描述不能为空', trigger: 'blur' }]
  }
})
const { executeForm, executeRules } = toRefs(executeData)

const taskData = reactive({
  taskForm: {} as any
})
const { taskForm } = toRefs(taskData)

function getList() {
  loading.value = true
  listAgent(queryParams.value).then((response: any) => {
    agentList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() {
  form.value = {
    agentId: undefined, agentName: undefined, agentType: undefined, description: undefined,
    modelId: undefined, systemPrompt: undefined, toolsJson: undefined, workflowJson: undefined,
    maxIterations: 10, temperature: 0.7, timeoutSeconds: 300, status: 1
  }
  proxy.resetForm('agentRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.agentId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
function handleAdd() { reset(); open.value = true; title.value = '新增Agent' }
function handleUpdate(row: any) {
  reset()
  getAgent(row.agentId || ids.value[0]).then((response: any) => { form.value = response.data; open.value = true; title.value = '修改Agent' })
}
function submitForm() {
  proxy.$refs['agentRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.agentId != undefined) {
        updateAgent(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addAgent(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}
function handleDelete(row: any) {
  const agentIds = row.agentId || ids.value.join(',')
  proxy.$modal.confirm('确认删除Agent"' + agentIds + '"？').then(() => {
    return delAgent(agentIds)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

// 执行Agent
function handleExecute(row: any) {
  executeResult.value = ''
  executeForm.value = { agentId: row.agentId, agentName: row.agentName, task: '', input: '', async: false }
  executeOpen.value = true
}
function submitExecute() {
  proxy.$refs['executeRef'].validate((valid: boolean) => {
    if (valid) {
      executing.value = true
      const payload = { agentId: executeForm.value.agentId, task: executeForm.value.task, input: executeForm.value.input || undefined }
      if (executeForm.value.async) {
        submitAgent(payload).then((response: any) => {
          executeResult.value = '任务已提交，taskId: ' + response.data
          executing.value = false
        }).catch(() => { executing.value = false })
      } else {
        executeAgent(payload).then((response: any) => {
          executeResult.value = typeof response.data === 'string' ? response.data : JSON.stringify(response.data)
          executing.value = false
        }).catch(() => { executing.value = false })
      }
    }
  })
}

// 查询异步任务状态
function handleTaskStatus() {
  taskResult.value = ''
  taskForm.value = { taskId: '' }
  taskStatusOpen.value = true
}
function queryTaskStatus() {
  if (!taskForm.value.taskId) { proxy.$modal.msgError('请输入任务ID'); return }
  getTaskStatus(taskForm.value.taskId).then((response: any) => {
    taskResult.value = '状态: ' + response.data
    taskResultType.value = 'info'
  }).catch(() => {})
}
function cancelTaskHandler() {
  if (!taskForm.value.taskId) { proxy.$modal.msgError('请输入任务ID'); return }
  cancelTask(taskForm.value.taskId).then((response: any) => {
    taskResult.value = response.msg || '取消成功'
    taskResultType.value = 'warning'
  }).catch(() => {})
}

getList()
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/ai/agent/index.vue
git commit -m "feat: add Agent management CRUD page with execute panel"
```

---

### Task 4: Model 管理页面

**Files:**
- Create: `frontend/src/views/ai/model/index.vue`

**Consumes:**
- `listModel`, `getModel`, `addModel`, `updateModel`, `delModel`, `testModel`, `setDefaultModel` from `@/api/ai/model`

**Produces:** 完整的 Model CRUD 页面，含搜索表单、表格（含 isDefault/isEnabled 标签）、新增/编辑对话框（apiKey 为 password 字段、isDefault/isEnabled 为开关）、测试连接按钮、设为默认按钮

- [ ] **Step 1: 创建 `frontend/src/views/ai/model/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模型标识" prop="modelName">
        <el-input v-model="queryParams.modelName" placeholder="请输入模型标识" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="提供商" prop="provider">
        <el-select v-model="queryParams.provider" placeholder="提供商" clearable>
          <el-option label="OpenAI" value="openai" />
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="Qwen" value="qwen" />
          <el-option label="Ollama" value="ollama" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型类型" prop="modelType">
        <el-select v-model="queryParams.modelType" placeholder="模型类型" clearable>
          <el-option label="Chat" value="chat" />
          <el-option label="Embedding" value="embedding" />
          <el-option label="Rerank" value="rerank" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="isEnabled">
        <el-select v-model="queryParams.isEnabled" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:model:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['ai:model:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:model:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型标识" align="center" prop="modelName" :show-overflow-tooltip="true" />
      <el-table-column label="显示名称" align="center" prop="displayName" :show-overflow-tooltip="true" />
      <el-table-column label="提供商" align="center" prop="provider" width="90" />
      <el-table-column label="模型类型" align="center" prop="modelType" width="100" />
      <el-table-column label="默认" align="center" prop="isDefault" width="70">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="success">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="isEnabled" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'danger'">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="340" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:model:edit']">编辑</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:model:remove']">删除</el-button>
          <el-button link type="primary" icon="Connection" @click="handleTest(scope.row)" v-hasPermi="['ai:model:query']">测试连接</el-button>
          <el-button link type="primary" icon="Star" @click="handleSetDefault(scope.row)" v-hasPermi="['ai:model:edit']" v-if="scope.row.isDefault !== 1">设为默认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form ref="modelRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型标识" prop="modelName">
              <el-input v-model="form.modelName" placeholder="如 deepseek-chat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名称" prop="displayName">
              <el-input v-model="form.displayName" placeholder="如 DeepSeek-V3" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提供商" prop="provider">
              <el-select v-model="form.provider" placeholder="请选择" style="width:100%">
                <el-option label="OpenAI" value="openai" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="Qwen" value="qwen" />
                <el-option label="Ollama" value="ollama" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType" placeholder="请选择" style="width:100%">
                <el-option label="Chat" value="chat" />
                <el-option label="Embedding" value="embedding" />
                <el-option label="Rerank" value="rerank" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入API Key" show-password />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如 https://api.deepseek.com/v1" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="默认模型">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Model">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listModel, getModel, addModel, updateModel, delModel, testModel, setDefaultModel } from '@/api/ai/model'
const { proxy } = getCurrentInstance() as any

const modelList = ref([])
const open = ref(false)
const loading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as any,
  queryParams: { pageNum: 1, pageSize: 10, modelName: undefined, provider: undefined, modelType: undefined, isEnabled: undefined } as any,
  rules: {
    modelName: [{ required: true, message: '模型标识不能为空', trigger: 'blur' }],
    displayName: [{ required: true, message: '显示名称不能为空', trigger: 'blur' }],
    provider: [{ required: true, message: '提供商不能为空', trigger: 'change' }],
    apiKey: [{ required: true, message: 'API Key不能为空', trigger: 'blur' }],
    baseUrl: [{ required: true, message: 'Base URL不能为空', trigger: 'blur' }],
    modelType: [{ required: true, message: '模型类型不能为空', trigger: 'change' }],
    maxTokens: [{ required: true, message: '最大Token不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listModel(queryParams.value).then((response: any) => {
    modelList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() {
  form.value = {
    modelId: undefined, modelName: undefined, displayName: undefined, provider: undefined,
    apiKey: undefined, baseUrl: undefined, modelType: undefined, maxTokens: 4096,
    temperature: 0.7, isDefault: 0, isEnabled: 1, remark: undefined
  }
  proxy.resetForm('modelRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.modelId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
function handleAdd() { reset(); open.value = true; title.value = '新增模型' }
function handleUpdate(row: any) {
  reset()
  getModel(row.modelId || ids.value[0]).then((response: any) => { form.value = response.data; open.value = true; title.value = '修改模型' })
}
function submitForm() {
  proxy.$refs['modelRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.modelId != undefined) {
        updateModel(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addModel(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}
function handleDelete(row: any) {
  const modelIds = row.modelId || ids.value.join(',')
  proxy.$modal.confirm('确认删除模型"' + modelIds + '"？').then(() => {
    return delModel(modelIds)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

// 测试连接
function handleTest(row: any) {
  testModel(row.modelId).then(() => {
    proxy.$modal.msgSuccess('连接成功')
  }).catch(() => {
    proxy.$modal.msgError('连接失败')
  })
}

// 设为默认
function handleSetDefault(row: any) {
  proxy.$modal.confirm('确认将模型"' + row.displayName + '"设为默认？').then(() => {
    return setDefaultModel(row.modelId)
  }).then(() => { getList(); proxy.$modal.msgSuccess('设置成功') }).catch(() => {})
}

getList()
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/ai/model/index.vue
git commit -m "feat: add Model management CRUD page with test connection and set default"
```

---

### Task 5: Tool 管理页面

**Files:**
- Create: `frontend/src/views/ai/tool/index.vue`

**Consumes:**
- `listTool`, `getTool`, `addTool`, `updateTool`, `delTool`, `invokeTool`, `testTool` from `@/api/ai/tool`

**Produces:** 完整的 Tool CRUD 页面，含搜索表单、表格、新增/编辑对话框（JSON schema 用 textarea）、调用测试弹窗（输入 JSON params → 显示返回结果）、连通测试按钮

- [ ] **Step 1: 创建 `frontend/src/views/ai/tool/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="工具标识" prop="toolName">
        <el-input v-model="queryParams.toolName" placeholder="请输入工具标识" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工具类型" prop="toolType">
        <el-select v-model="queryParams.toolType" placeholder="工具类型" clearable>
          <el-option label="MCP Server" value="mcp_server" />
          <el-option label="HTTP API" value="http_api" />
          <el-option label="Function" value="function" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="isEnabled">
        <el-select v-model="queryParams.isEnabled" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:tool:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['ai:tool:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:tool:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="toolList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工具标识" align="center" prop="toolName" :show-overflow-tooltip="true" />
      <el-table-column label="显示名称" align="center" prop="displayName" :show-overflow-tooltip="true" />
      <el-table-column label="工具类型" align="center" prop="toolType" width="110">
        <template #default="scope">
          <el-tag>{{ scope.row.toolType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="服务URL" align="center" prop="serverUrl" :show-overflow-tooltip="true" />
      <el-table-column label="超时(ms)" align="center" prop="timeoutMs" width="90" />
      <el-table-column label="状态" align="center" prop="isEnabled" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'danger'">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:tool:edit']">编辑</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:tool:remove']">删除</el-button>
          <el-button link type="primary" icon="VideoPlay" @click="handleInvoke(scope.row)" v-hasPermi="['ai:tool:execute']">调用测试</el-button>
          <el-button link type="primary" icon="Connection" @click="handleTest(scope.row)" v-hasPermi="['ai:tool:query']">连通测试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="toolRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工具标识" prop="toolName">
              <el-input v-model="form.toolName" placeholder="请输入工具标识" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名称" prop="displayName">
              <el-input v-model="form.displayName" placeholder="请输入显示名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工具类型" prop="toolType">
              <el-select v-model="form.toolType" placeholder="请选择" style="width:100%">
                <el-option label="MCP Server" value="mcp_server" />
                <el-option label="HTTP API" value="http_api" />
                <el-option label="Function" value="function" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证类型" prop="authType">
              <el-select v-model="form.authType" placeholder="请选择" style="width:100%">
                <el-option label="None" value="none" />
                <el-option label="API Key" value="api_key" />
                <el-option label="OAuth2" value="oauth2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="服务URL" prop="serverUrl">
          <el-input v-model="form.serverUrl" placeholder="请输入服务URL" />
        </el-form-item>
        <el-form-item label="输入Schema" prop="inputSchema">
          <el-input v-model="form.inputSchema" type="textarea" :rows="4" placeholder="JSON Schema格式" />
        </el-form-item>
        <el-form-item label="输出Schema" prop="outputSchema">
          <el-input v-model="form.outputSchema" type="textarea" :rows="3" placeholder="JSON Schema格式" />
        </el-form-item>
        <el-form-item label="认证配置" prop="authConfig">
          <el-input v-model="form.authConfig" type="textarea" :rows="2" placeholder="JSON格式认证配置" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="超时(ms)" prop="timeoutMs">
              <el-input-number v-model="form.timeoutMs" :min="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试次数" prop="retryCount">
              <el-input-number v-model="form.retryCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 调用测试对话框 -->
    <el-dialog title="调用测试" v-model="invokeOpen" width="600px" append-to-body>
      <el-form ref="invokeRef" :model="invokeForm" label-width="80px">
        <el-form-item label="工具">
          <el-input :value="invokeForm.displayName" disabled />
        </el-form-item>
        <el-form-item label="参数JSON">
          <el-input v-model="invokeForm.params" type="textarea" :rows="5" placeholder='请输入调用参数，JSON格式，如 {"key": "value"}' />
        </el-form-item>
      </el-form>
      <div v-if="invokeResult" class="execute-result">
        <el-input v-model="invokeResult" type="textarea" :rows="8" readonly />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitInvoke" :loading="invoking">调 用</el-button>
          <el-button @click="invokeOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Tool">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listTool, getTool, addTool, updateTool, delTool, invokeTool, testTool } from '@/api/ai/tool'
const { proxy } = getCurrentInstance() as any

const toolList = ref([])
const open = ref(false)
const loading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

// 调用测试相关
const invokeOpen = ref(false)
const invoking = ref(false)
const invokeResult = ref('')

const data = reactive({
  form: {} as any,
  queryParams: { pageNum: 1, pageSize: 10, toolName: undefined, toolType: undefined, isEnabled: undefined } as any,
  rules: {
    toolName: [{ required: true, message: '工具标识不能为空', trigger: 'blur' }],
    displayName: [{ required: true, message: '显示名称不能为空', trigger: 'blur' }],
    toolType: [{ required: true, message: '工具类型不能为空', trigger: 'change' }],
    inputSchema: [{ required: true, message: '输入Schema不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

const invokeData = reactive({
  invokeForm: {} as any
})
const { invokeForm } = toRefs(invokeData)

function getList() {
  loading.value = true
  listTool(queryParams.value).then((response: any) => {
    toolList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() {
  form.value = {
    toolId: undefined, toolName: undefined, displayName: undefined, toolType: undefined,
    description: undefined, serverUrl: undefined, inputSchema: undefined, outputSchema: undefined,
    authType: 'none', authConfig: undefined, timeoutMs: 30000, retryCount: 0, isEnabled: 1
  }
  proxy.resetForm('toolRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.toolId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
function handleAdd() { reset(); open.value = true; title.value = '新增工具' }
function handleUpdate(row: any) {
  reset()
  getTool(row.toolId || ids.value[0]).then((response: any) => { form.value = response.data; open.value = true; title.value = '修改工具' })
}
function submitForm() {
  proxy.$refs['toolRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.toolId != undefined) {
        updateTool(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addTool(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}
function handleDelete(row: any) {
  const toolIds = row.toolId || ids.value.join(',')
  proxy.$modal.confirm('确认删除工具"' + toolIds + '"？').then(() => {
    return delTool(toolIds)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

// 调用测试
function handleInvoke(row: any) {
  invokeResult.value = ''
  invokeForm.value = { toolId: row.toolId, displayName: row.displayName, params: '' }
  invokeOpen.value = true
}
function submitInvoke() {
  invoking.value = true
  let params = undefined
  if (invokeForm.value.params) {
    try { params = JSON.parse(invokeForm.value.params) }
    catch { proxy.$modal.msgError('参数JSON格式错误'); invoking.value = false; return }
  }
  invokeTool(invokeForm.value.toolId, params).then((response: any) => {
    invokeResult.value = typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2)
    invoking.value = false
  }).catch(() => { invoking.value = false })
}

// 连通测试
function handleTest(row: any) {
  testTool(row.toolId).then(() => {
    proxy.$modal.msgSuccess('连接成功')
  }).catch(() => {
    proxy.$modal.msgError('连接失败')
  })
}

getList()
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/ai/tool/index.vue
git commit -m "feat: add Tool management CRUD page with invoke test and connectivity test"
```

---

### Task 6: 路由注册

**Files:**
- Modify: `frontend/src/router/index.ts` — 在 `dynamicRoutes` 数组末尾、`]` 之前追加 3 条路由

**Consumes:** Task 3/4/5 的视图文件已存在

**Produces:** 侧边栏菜单显示 Agent管理、模型配置、工具管理，路由可导航到对应页面

- [ ] **Step 1: 修改 `frontend/src/router/index.ts`**

在 `dynamicRoutes` 数组中，找到最后的 `ai/knowledge/search` 路由块右花括号 `}` 和 `]`（第 194 行附近），在 `}` 之后、`]` 之前插入以下 3 条路由：

```typescript
  {
    path: '/ai/agent',
    component: Layout,
    permissions: ['ai:agent:query'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/agent/index.vue'),
        name: 'AiAgent',
        meta: { title: 'Agent管理', icon: 'cpu' }
      }
    ]
  },
  {
    path: '/ai/model',
    component: Layout,
    permissions: ['ai:model:query'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/model/index.vue'),
        name: 'AiModel',
        meta: { title: '模型配置', icon: 'cpu' }
      }
    ]
  },
  {
    path: '/ai/tool',
    component: Layout,
    permissions: ['ai:tool:query'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/tool/index.vue'),
        name: 'AiTool',
        meta: { title: '工具管理', icon: 'tool' }
      }
    ]
  },
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/router/index.ts
git commit -m "feat: register Agent/Model/Tool routes in dynamicRoutes"
```

---

## 自检清单

**1. Spec coverage:** 设计文档中每个需求都有对应 Task：
- 类型定义 → Task 1
- 6 个 API 文件 → Task 2 (agent.ts, model.ts, tool.ts)
- Agent 管理页面 → Task 3
- Model 管理页面 → Task 4
- Tool 管理页面 → Task 5
- 路由注册 → Task 6

**2. Placeholder scan:** 无 TBD/TODO/占位符，所有代码块均为完整可运行的代码

**3. Type consistency:**
- API 函数名与视图层 imports 完全一致
- 后端 API URL 与 Controller `@RequestMapping` 对齐
- 权限字符串与后端 `@PreAuthorize` 匹配
- 字段名 camelCase 与后端实体 Java 属性名一致
