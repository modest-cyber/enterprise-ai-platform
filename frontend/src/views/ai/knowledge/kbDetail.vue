<template>
  <div class="app-container">
    <!-- 知识库信息 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-descriptions title="知识库信息" :column="3" border>
          <el-descriptions-item label="名称">{{ kbInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="类型"><dict-tag :options="ai_kb_type" :value="kbInfo.kbType" /></el-descriptions-item>
          <el-descriptions-item label="文档数量">{{ kbInfo.docCount }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ kbInfo.description }}</el-descriptions-item>
          <el-descriptions-item label="切块大小">{{ kbInfo.chunkSize }}</el-descriptions-item>
          <el-descriptions-item label="重叠大小">{{ kbInfo.chunkOverlap }}</el-descriptions-item>
          <el-descriptions-item label="状态"><dict-tag :options="ai_kb_status" :value="kbInfo.status" /></el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(kbInfo.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="Embedding">{{ kbInfo.embeddingModel || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-col>
    </el-row>

    <el-divider />

    <!-- 文档统计卡片 -->
    <el-row :gutter="16" class="doc-stats">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in statCards" :key="card.label" style="margin-bottom: 12px;">
        <el-card shadow="hover" :class="['stat-card', card.cssClass]">
          <div class="stat-inner">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :accept="'.txt,.pdf,.md,.docx,.xlsx,.html'"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
          :before-upload="handleBeforeUpload">
          <el-button type="primary" plain icon="Upload">上传文档</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="docMultiple"
                   @click="handleDeleteDoc()" v-hasPermi="['ai:kb:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Refresh" @click="refreshAll">刷新</el-button>
      </el-col>
    </el-row>

    <!-- 文档表格 -->
    <el-table v-loading="docLoading" :data="docList" @selection-change="handleDocSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件名" align="center" prop="fileName" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="文件类型" align="center" prop="fileType" width="90" />
      <el-table-column label="文件大小" align="center" width="100">
        <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="切块数" align="center" prop="chunkCount" width="80" />
      <el-table-column label="向量数" align="center" width="80">
        <template #default="scope">{{ scope.row.vectorCount || '-' }}</template>
      </el-table-column>
      <el-table-column label="处理状态" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.processStatus === 'SUCCESS'" type="success">已完成</el-tag>
          <el-tag v-else-if="scope.row.processStatus === 'PROCESSING'" type="warning">处理中</el-tag>
          <el-tag v-else-if="scope.row.processStatus === 'FAILED'" type="danger">失败</el-tag>
          <el-tag v-else type="info">待处理</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" icon="VideoPlay" @click="handleProcess(scope.row)">处理</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDeleteDoc(scope.row)" v-hasPermi="['ai:kb:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="docTotal > 0" :total="docTotal" v-model:page="docPage.pageNum"
                v-model:limit="docPage.pageSize" @pagination="getDocList" />

    <!-- 文档预览对话框 -->
    <el-dialog title="文档预览" v-model="previewOpen" width="80%" top="5vh" append-to-body destroy-on-close>
      <div class="preview-container">
        <div v-if="previewLoading" class="preview-loading">加载中...</div>
        <iframe v-else-if="previewType === 'pdf'" :src="previewPdfUrl" class="preview-iframe" />
        <div v-else-if="previewType === 'markdown'" class="preview-markdown" v-html="previewHtml" />
        <pre v-else class="preview-text">{{ previewContent }}</pre>
      </div>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog title="文档处理" v-model="processOpen" width="600px" append-to-body :close-on-click-modal="false">
      <div class="process-dialog">
        <el-steps :active="processSteps" align-center>
          <el-step title="文件读取" />
          <el-step title="文本解析" />
          <el-step title="文本切块" />
          <el-step title="Embedding" />
          <el-step title="写入Milvus" />
        </el-steps>

        <div class="process-status" style="margin-top: 24px; text-align: center;">
          <p><strong>当前状态：{{ processStatusText }}</strong></p>
          <el-progress :percentage="processProgress" :status="processProgress === 100 ? 'success' : undefined" />
          <p v-if="processMessage" style="color: #909399; margin-top: 8px;">{{ processMessage }}</p>
          <p v-if="processData.chunkCount" style="margin-top: 8px;">
            切块数：{{ processData.chunkCount }} | 向量数：{{ processData.vectorCount }}
          </p>
          <el-alert v-if="processFailed" title="处理失败" type="error" :description="processMessage" show-icon style="margin-top: 12px;" />
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="startProcess" :loading="processRunning" :disabled="processRunning">开始处理</el-button>
        <el-button @click="closeProcess">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="KbDetail">
import { ref, reactive, computed, watch, getCurrentInstance, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledge, listDocuments, deleteDocument, processDocument, getProcessStatus, getDocumentContent, getDocStats } from '@/api/ai/knowledge'
import { getToken } from '@/utils/auth'

const route = useRoute()
const { proxy } = getCurrentInstance() as any
const { ai_kb_type, ai_kb_status } = proxy.useDict('ai_kb_type', 'ai_kb_status')

const kbId = computed(() => Number(route.query.kbId))

// ==================== 知识库信息 ====================
const kbInfo = ref<any>({})

// ==================== 文档统计 ====================
const docStats = reactive({ total: 0, pending: 0, processing: 0, success: 0, failed: 0 })
const statCards = computed(() => [
  { label: '总文档', value: docStats.total, cssClass: 'stat-total' },
  { label: '待处理', value: docStats.pending, cssClass: 'stat-pending' },
  { label: '处理中', value: docStats.processing, cssClass: 'stat-processing' },
  { label: '已完成', value: docStats.success, cssClass: 'stat-success' },
  { label: '失败', value: docStats.failed, cssClass: 'stat-failed' }
])

// ==================== 文档列表 ====================
const docList = ref<any[]>([])
const docLoading = ref(false)
const docIds = ref<number[]>([])
const docMultiple = ref(true)
const docTotal = ref(0)
const docPage = reactive({ pageNum: 1, pageSize: 10 })

// ==================== 上传 ====================
const uploadUrl = computed(() => import.meta.env.VITE_APP_BASE_API + '/ai/kb/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const uploadData = computed(() => ({ kbId: kbId.value }))

// ==================== 预览 ====================
const previewOpen = ref(false)
const previewLoading = ref(false)
const previewContent = ref('')
const previewHtml = ref('')
const previewType = ref('')
const previewFileName = ref('')
const previewDocId = ref<number | null>(null)
const previewPdfUrl = computed(() => {
  return import.meta.env.VITE_APP_BASE_API + '/ai/document/' + previewDocId.value + '/content'
})

// ==================== 处理 ====================
const processOpen = ref(false)
const processDocId = ref<number | null>(null)
const processRunning = ref(false)
const processProgress = ref(0)
const processMessage = ref('')
const processFailed = ref(false)
const processData = reactive({ chunkCount: 0, vectorCount: 0 })
const processStatusText = computed(() => {
  if (processFailed.value) return '失败'
  if (processProgress.value === 100) return '已完成'
  if (processRunning.value) return '处理中'
  return '待处理'
})
const processSteps = computed(() => {
  if (processProgress.value >= 100) return 5
  if (processProgress.value >= 80) return 4
  if (processProgress.value >= 50) return 3
  if (processProgress.value >= 30) return 2
  if (processProgress.value >= 10) return 1
  return 0
})
let pollTimer: any = null

// ==================== 方法 ====================

function refreshAll() { getKbInfo(); getDocList(); loadDocStats() }

function getKbInfo() {
  getKnowledge(kbId.value).then((res: any) => { kbInfo.value = res.data || {} })
}

function loadDocStats() {
  getDocStats(kbId.value).then((res: any) => {
    if (res.data) {
      docStats.total = res.data.total || 0
      docStats.pending = res.data.pending || 0
      docStats.processing = res.data.processing || 0
      docStats.success = res.data.success || 0
      docStats.failed = res.data.failed || 0
    }
  })
}

function getDocList() {
  docLoading.value = true
  listDocuments(kbId.value).then((res: any) => {
    docList.value = res.data || []
    docTotal.value = (res.data || []).length
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
  if (!allowed.includes(ext)) { proxy.$modal.msgError('不支持的文件类型: ' + ext); return false }
  return true
}

function handleUploadSuccess() { proxy.$modal.msgSuccess('上传成功'); refreshAll() }

function handleDeleteDoc(row?: any) {
  const ids = row ? [row.docId] : docIds.value
  if (!ids.length) { proxy.$modal.msgError('请选择文档'); return }
  const label = row ? row.fileName : ids.length + '个文档'
  proxy.$modal.confirm('确认删除"' + label + '"？').then(() => {
    return Promise.all(ids.map((id: number) => deleteDocument(id)))
  }).then(() => { refreshAll(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleView(row: any) {
  previewDocId.value = row.docId
  previewFileName.value = row.fileName
  previewLoading.value = true
  previewOpen.value = true

  const ext = (row.fileType || '').toLowerCase()
  if (ext === 'pdf') {
    previewType.value = 'pdf'
    previewLoading.value = false
  } else if (ext === 'md') {
    getDocumentContent(row.docId).then((res: any) => {
      previewHtml.value = (res.data || '').replace(/\n/g, '<br/>')
      previewType.value = 'markdown'
      previewLoading.value = false
    })
  } else {
    getDocumentContent(row.docId).then((res: any) => {
      previewContent.value = res.data || ''
      previewType.value = 'text'
      previewLoading.value = false
    })
  }
}

function handleProcess(row: any) {
  processDocId.value = row.docId
  processProgress.value = row.processProgress || 0
  processMessage.value = row.processMessage || ''
  processFailed.value = row.processStatus === 'FAILED'
  processData.chunkCount = row.chunkCount || 0
  processData.vectorCount = row.vectorCount || 0
  processRunning.value = false
  processOpen.value = true
}

function startProcess() {
  if (!processDocId.value) return
  processRunning.value = true
  processFailed.value = false
  processDocument(processDocId.value).then(() => {
    startPolling()
  }).catch(() => {
    processFailed.value = true
    processMessage.value = '提交处理失败'
    processRunning.value = false
  })
}

function startPolling() {
  pollTimer = setInterval(() => {
    if (!processDocId.value) { stopPolling(); return }
    getProcessStatus(processDocId.value).then((res: any) => {
      if (res.data) {
        processProgress.value = res.data.progress || 0
        processMessage.value = res.data.message || ''
        processData.chunkCount = res.data.chunkCount || 0
        processData.vectorCount = res.data.vectorCount || 0
        if (res.data.status === 'SUCCESS') {
          processProgress.value = 100
          processRunning.value = false
          stopPolling()
        } else if (res.data.status === 'FAILED') {
          processFailed.value = true
          processRunning.value = false
          stopPolling()
        }
      }
    }).catch(() => {})
  }, 3000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  refreshAll()
}

function closeProcess() { stopPolling(); processOpen.value = false }

onBeforeUnmount(() => stopPolling())

function formatFileSize(size: number): string {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, s = size
  while (s >= 1024 && i < units.length - 1) { s /= 1024; i++ }
  return s.toFixed(1) + ' ' + units[i]
}

watch(() => route.query.kbId, () => {
  if (route.query.kbId) { refreshAll() }
}, { immediate: true })
</script>

<style scoped lang="scss">
.doc-stats {
  .stat-card {
    text-align: center; border-radius: 8px;
    .stat-inner { padding: 8px 0; }
    .stat-label { font-size: 13px; color: var(--el-text-color-secondary, #909399); margin-bottom: 6px; }
    .stat-value { font-size: 24px; font-weight: 700; }
    &.stat-total .stat-value { color: #409EFF; }
    &.stat-pending .stat-value { color: #909399; }
    &.stat-processing .stat-value { color: #E6A23C; }
    &.stat-success .stat-value { color: #67C23A; }
    &.stat-failed .stat-value { color: #F56C6C; }
  }
}
.preview-container {
  min-height: 400px;
  .preview-loading { text-align: center; padding: 80px 0; color: var(--el-text-color-secondary, #909399); }
  .preview-iframe { width: 100%; height: 70vh; border: none; }
  .preview-text { white-space: pre-wrap; word-break: break-word; max-height: 70vh; overflow-y: auto; padding: 16px; background: var(--el-fill-color-light, #f5f7fa); border-radius: 4px; font-size: 14px; line-height: 1.6; }
  .preview-markdown { max-height: 70vh; overflow-y: auto; padding: 16px; line-height: 1.8; font-size: 14px; }
}
</style>
