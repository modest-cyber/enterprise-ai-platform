<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-descriptions title="知识库信息" :column="1" border>
          <el-descriptions-item label="名称">{{ kbInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ kbInfo.description }}</el-descriptions-item>
          <el-descriptions-item label="类型"><dict-tag :options="ai_kb_type" :value="kbInfo.kbType" /></el-descriptions-item>
          <el-descriptions-item label="切块大小">{{ kbInfo.chunkSize }}</el-descriptions-item>
          <el-descriptions-item label="重叠大小">{{ kbInfo.chunkOverlap }}</el-descriptions-item>
          <el-descriptions-item label="文档数量">{{ kbInfo.docCount }}</el-descriptions-item>
          <el-descriptions-item label="状态"><dict-tag :options="ai_kb_status" :value="kbInfo.status" /></el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(kbInfo.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-col>
    </el-row>

    <el-divider />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload :action="uploadUrl" :headers="uploadHeaders" :data="uploadData" :accept="'.txt,.pdf,.md,.docx,.xlsx,.html'" :show-file-list="false" :on-success="handleUploadSuccess" :before-upload="handleBeforeUpload">
          <el-button type="primary" plain icon="Upload">上传文档</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="docMultiple" @click="handleDeleteDoc" v-hasPermi="['ai:kb:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Refresh" @click="getDocList">刷新</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="docLoading" :data="docList" @selection-change="handleDocSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件名称" align="center" prop="fileName" :show-overflow-tooltip="true" />
      <el-table-column label="文件类型" align="center" prop="fileType" width="100">
        <template #default="scope"><dict-tag :options="ai_doc_type" :value="scope.row.fileType" /></template>
      </el-table-column>
      <el-table-column label="文件大小" align="center" width="100">
        <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="切块数" align="center" prop="chunkCount" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope"><dict-tag :options="ai_doc_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="handleDeleteDoc(scope.row)" v-hasPermi="['ai:kb:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="docTotal > 0" :total="docTotal" v-model:page="docPage.pageNum" v-model:limit="docPage.pageSize" @pagination="getDocList" />
  </div>
</template>

<script setup lang="ts" name="KbDetail">
import { ref, reactive, computed, watch, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledge } from '@/api/ai/knowledge'
import { listDocuments, deleteDocument } from '@/api/ai/knowledge'
import { getToken } from '@/utils/auth'

const route = useRoute()
const { proxy } = getCurrentInstance() as any
const { ai_kb_type, ai_kb_status, ai_doc_type, ai_doc_status } = proxy.useDict('ai_kb_type', 'ai_kb_status', 'ai_doc_type', 'ai_doc_status')

const kbInfo = ref<any>({})
const docList = ref<any[]>([])
const docLoading = ref(false)
const docIds = ref<number[]>([])
const docSingle = ref(true)
const docMultiple = ref(true)
const docTotal = ref(0)
const docPage = reactive({ pageNum: 1, pageSize: 10 })
const kbId = computed(() => Number(route.query.kbId))

const uploadUrl = computed(() => import.meta.env.VITE_APP_BASE_API + '/ai/kb/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const uploadData = computed(() => ({ kbId: kbId.value }))

function getKbInfo() {
  getKnowledge(kbId.value).then((response: any) => { kbInfo.value = response.data })
}
function getDocList() {
  docLoading.value = true
  listDocuments(kbId.value).then((response: any) => {
    docList.value = response.data || []
    docTotal.value = (response.data || []).length
    docLoading.value = false
  })
}
function handleDocSelectionChange(selection: any[]) {
  docIds.value = selection.map((item: any) => item.docId)
  docMultiple.value = !selection.length
}
function handleBeforeUpload(file: any) {
  const ext = file.name.split('.').pop().toLowerCase()
  const allowed = ['txt', 'pdf', 'md', 'docx', 'xlsx', 'html']
  if (!allowed.includes(ext)) {
    proxy.$modal.msgError('不支持的文件类型，仅支持: ' + allowed.join(', '))
    return false
  }
  return true
}
function handleUploadSuccess() {
  proxy.$modal.msgSuccess('上传成功')
  getDocList()
  getKbInfo()
}
function handleDeleteDoc(row: any) {
  const ids = row.docId || docIds.value.join(',')
  proxy.$modal.confirm('确认删除所选文档？').then(() => {
    if (row.docId) {
      return deleteDocument(row.docId)
    }
    return Promise.all(docIds.value.map((id: number) => deleteDocument(id)))
  }).then(() => { getDocList(); getKbInfo(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function formatFileSize(size: number): string {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let s = size
  while (s >= 1024 && i < units.length - 1) { s /= 1024; i++ }
  return s.toFixed(1) + ' ' + units[i]
}

watch(() => route.query.kbId, () => {
  if (route.query.kbId) {
    getKbInfo()
    getDocList()
  }
}, { immediate: true })
</script>