<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <template #header><span>选择知识库</span></template>
          <el-checkbox-group v-model="selectedKbIds">
            <div v-for="kb in kbOptions" :key="kb.kbId" style="margin-bottom:8px">
              <el-checkbox :label="kb.kbId">{{ kb.name }}</el-checkbox>
            </div>
          </el-checkbox-group>
          <div style="margin-top:16px">
            <el-button type="primary" icon="Select" size="small" @click="selectAll">全选</el-button>
            <el-button icon="Close" size="small" @click="selectedKbIds = []">清空</el-button>
          </div>
        </el-card>
        <el-card style="margin-top:16px">
          <el-form label-width="70px" size="small">
            <el-form-item label="TopK"><el-input-number v-model="topK" :min="1" :max="20" /></el-form-item>
            <el-form-item label="最低分"><el-input-number v-model="minScore" :min="0" :max="1" :step="0.1" :precision="1" /></el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="18">
        <div class="search-input-area">
          <el-input v-model="query" placeholder="输入检索关键词" clearable @keyup.enter="handleSearch" size="large">
            <template #append><el-button icon="Search" @click="handleSearch" :loading="searching" :disabled="!query">检索</el-button></template>
          </el-input>
        </div>

        <div v-if="results.length > 0" class="search-results">
          <div v-for="(item, idx) in results" :key="idx" class="result-item">
            <div class="result-header">
              <el-tag size="small" type="success">{{ scorePercent(item.score) }}</el-tag>
              <span class="result-file">{{ item.fileName }}</span>
            </div>
            <div class="result-snippet">{{ item.snippet }}</div>
          </div>
        </div>
        <el-empty v-else description="请选择知识库并输入关键词检索" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="KbSearch">
import { ref, onMounted } from 'vue'
import { listKnowledge, searchKnowledge } from '@/api/ai/knowledge'
import { ElMessage } from 'element-plus'

const kbOptions = ref<any[]>([])
const selectedKbIds = ref<number[]>([])
const query = ref('')
const topK = ref(5)
const minScore = ref(0.5)
const results = ref<any[]>([])
const searching = ref(false)

onMounted(() => {
  listKnowledge({ pageNum: 1, pageSize: 100 }).then((r: any) => { kbOptions.value = r.rows || [] })
})

function selectAll() { selectedKbIds.value = kbOptions.value.map((k: any) => k.kbId) }

function handleSearch() {
  if (!query.value) { ElMessage.warning('请输入检索关键词'); return }
  if (selectedKbIds.value.length === 0) { ElMessage.warning('请选择至少一个知识库'); return }
  searching.value = true
  searchKnowledge({ kbIds: selectedKbIds.value, query: query.value, topK: topK.value, minScore: minScore.value }).then((r: any) => {
    results.value = r.data || []
    searching.value = false
  }).catch(() => { searching.value = false })
}

function scorePercent(score: number): string {
  return (score * 100).toFixed(1) + '%'
}
</script>

<style scoped>
.search-input-area { margin-bottom: 20px; }
.search-results { max-height: 60vh; overflow-y: auto; }
.result-item { padding: 16px; margin-bottom: 12px; background: var(--el-fill-color-lighter, #f5f7fa); border-radius: 8px; }
.result-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.result-file { font-weight: 600; color: var(--el-text-color-primary, #303133); }
.result-snippet { font-size: 14px; color: var(--el-text-color-regular, #606266); line-height: 1.6; }
</style>