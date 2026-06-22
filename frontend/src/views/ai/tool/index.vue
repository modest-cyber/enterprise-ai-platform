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
