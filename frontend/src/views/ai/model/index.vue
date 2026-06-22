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
