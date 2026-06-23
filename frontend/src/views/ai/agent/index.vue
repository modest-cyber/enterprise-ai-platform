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
      <el-table-column label="关联模型" align="center" width="140">
          <template #default="scope"><span>{{ getModelName(scope.row.modelId) }}</span></template>
        </el-table-column>
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
            <el-form-item label="关联模型" prop="modelId">
              <el-select v-model="form.modelId" placeholder="请选择模型" clearable filterable style="width:100%">
                <el-option v-for="m in modelOptions" :key="m.modelId" :label="m.displayName + ' (' + m.modelName + ')'" :value="m.modelId" />
              </el-select>
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
        <el-form-item label="关联工具" prop="toolsJson">
          <el-select v-model="form.toolIds" multiple clearable filterable placeholder="请选择工具" style="width:100%">
            <el-option v-for="t in toolOptions" :key="t.toolId" :label="t.displayName + ' (' + t.toolName + ')'" :value="t.toolId" />
          </el-select>
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
import { listEnabledModels } from '@/api/ai/model'
import { listTool } from '@/api/ai/tool'
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
const modelOptions = ref<any[]>([])
const toolOptions = ref<any[]>([])

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

// 获取工具选项列表
function getToolOptions() {
  listTool({ pageNum: 1, pageSize: 1000 }).then((response: any) => {
    toolOptions.value = response.rows || []
  })
}
// 获取模型选项列表（仅启用的模型）
function getModelOptions() {
  listEnabledModels().then((response: any) => {
    modelOptions.value = response.data || []
  })
}
// 根据模型ID获取显示名称
function getModelName(modelId: number) {
  const m = modelOptions.value.find((item: any) => item.modelId === modelId)
  return m ? m.displayName : (modelId || '-')
}

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
    modelId: undefined, systemPrompt: undefined, toolsJson: undefined, toolIds: [], workflowJson: undefined,
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
function handleAdd() { reset(); form.value.toolIds = []; open.value = true; title.value = '新增Agent' }
function handleUpdate(row: any) {
  reset()
  getAgent(row.agentId || ids.value[0]).then((response: any) => {
    const data = response.data
    // toolsJson 字符串转数组供多选下拉使用
    if (data.toolsJson) {
      try { data.toolIds = JSON.parse(data.toolsJson) } catch { data.toolIds = [] }
    }
    form.value = data
    open.value = true
    title.value = '修改Agent'
  })
}
function submitForm() {
  proxy.$refs['agentRef'].validate((valid: boolean) => {
    if (valid) {
      // 将 toolIds 数组转为 toolsJson JSON 字符串
      form.value.toolsJson = form.value.toolIds && form.value.toolIds.length > 0
        ? JSON.stringify(form.value.toolIds)
        : undefined
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
getModelOptions()
getToolOptions()
</script>
