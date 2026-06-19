<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="知识库名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入知识库名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库类型" prop="kbType">
        <el-select v-model="queryParams.kbType" placeholder="知识库类型" clearable>
          <el-option v-for="dict in ai_kb_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option v-for="dict in ai_kb_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:kb:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['ai:kb:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:kb:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="kbList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="知识库类型" align="center" prop="kbType">
        <template #default="scope"><dict-tag :options="ai_kb_type" :value="scope.row.kbType" /></template>
      </el-table-column>
      <el-table-column label="文档数量" align="center" prop="docCount" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope"><dict-tag :options="ai_kb_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['ai:kb:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:kb:edit']">编辑</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:kb:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="kbRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="知识库类型" prop="kbType">
          <el-select v-model="form.kbType" placeholder="请选择">
            <el-option v-for="dict in ai_kb_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="切块大小" prop="chunkSize">
          <el-input-number v-model="form.chunkSize" :min="100" :max="8192" />
        </el-form-item>
        <el-form-item label="重叠大小" prop="chunkOverlap">
          <el-input-number v-model="form.chunkOverlap" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in ai_kb_status" :key="dict.value" :value="Number(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Embedding模型" prop="embeddingModel">
          <el-input v-model="form.embeddingModel" placeholder="如 text-embedding-3-small" />
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

<script setup lang="ts" name="Knowledge">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listKnowledge, getKnowledge, addKnowledge, updateKnowledge, deleteKnowledge } from '@/api/ai/knowledge'
const { proxy } = getCurrentInstance() as any
const { ai_kb_type, ai_kb_status } = proxy.useDict('ai_kb_type', 'ai_kb_status')

const kbList = ref([])
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
  queryParams: { pageNum: 1, pageSize: 10, name: undefined, kbType: undefined, status: undefined } as any,
  rules: {
    name: [{ required: true, message: '知识库名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listKnowledge(queryParams.value).then((response: any) => {
    kbList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() {
  form.value = { kbId: undefined, name: undefined, description: undefined, kbType: undefined, chunkSize: 512, chunkOverlap: 50, status: 1, embeddingModel: undefined }
  proxy.resetForm('kbRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.kbId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
function handleAdd() { reset(); open.value = true; title.value = '新增知识库' }
function handleUpdate(row: any) {
  reset()
  getKnowledge(row.kbId || ids.value[0]).then((response: any) => { form.value = response.data; open.value = true; title.value = '修改知识库' })
}
function submitForm() {
  proxy.$refs['kbRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.kbId != undefined) {
        updateKnowledge(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addKnowledge(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}
function handleDelete(row: any) {
  const kbIds = row.kbId || ids.value.join(',')
  proxy.$modal.confirm('确认删除知识库"' + kbIds + '"？').then(() => {
    return deleteKnowledge(kbIds)
  }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleDetail(row: any) {
  proxy.$router.push({ path: '/ai/knowledge/detail', query: { kbId: row.kbId } })
}
getList()
</script>