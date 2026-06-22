<template>
  <div class="dashboard-container">
    <!-- 第一行：系统概览统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card--agent">
          <div class="stat-card__inner">
            <div class="stat-card__icon">
              <el-icon :size="36"><Cpu /></el-icon>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">Agent总数</div>
              <div class="stat-card__value">{{ overview.agentCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card--knowledge">
          <div class="stat-card__inner">
            <div class="stat-card__icon">
              <el-icon :size="36"><Collection /></el-icon>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">知识库数量</div>
              <div class="stat-card__value">{{ overview.knowledgeCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card--doc">
          <div class="stat-card__inner">
            <div class="stat-card__icon">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">文档数量</div>
              <div class="stat-card__value">{{ overview.documentCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card--chat">
          <div class="stat-card__inner">
            <div class="stat-card__icon">
              <el-icon :size="36"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-card__info">
              <div class="stat-card__label">累计会话数</div>
              <div class="stat-card__value">{{ overview.chatCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：最近活动 + 模型服务状态 -->
    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover" class="recent-card">
          <template #header><span>最近活动</span></template>
          <el-tabs v-model="recentTab">
            <el-tab-pane label="Agent" name="agents">
              <el-table :data="recent.agents" size="small" :show-header="true">
                <el-table-column label="Agent名称" prop="name" show-overflow-tooltip />
                <el-table-column label="类型" prop="type" width="90" />
                <el-table-column label="创建时间" prop="createTime" width="170">
                  <template #default="scope"><span>{{ scope.row.createTime }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="知识库" name="knowledge">
              <el-table :data="recent.knowledge" size="small" :show-header="true">
                <el-table-column label="知识库名称" prop="name" show-overflow-tooltip />
                <el-table-column label="类型" prop="kbType" width="90" />
                <el-table-column label="创建时间" prop="createTime" width="170">
                  <template #default="scope"><span>{{ scope.row.createTime }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="会话" name="conversations">
              <el-table :data="recent.conversations" size="small" :show-header="true">
                <el-table-column label="会话标题" prop="title" show-overflow-tooltip />
                <el-table-column label="类型" prop="agentType" width="90" />
                <el-table-column label="创建时间" prop="createTime" width="170">
                  <template #default="scope"><span>{{ scope.row.createTime }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="model-status-card">
          <template #header><span>模型服务状态</span></template>
          <div class="service-list">
            <div v-for="svc in modelServices" :key="svc.name" class="service-item">
              <span class="service-name">{{ svc.name }}</span>
              <span class="service-host">{{ svc.host }}:{{ svc.port }}</span>
              <el-tag :type="svc.status === 'online' ? 'success' : 'danger'" size="small" effect="dark">
                {{ svc.status === 'online' ? '在线' : '离线' }}
              </el-tag>
            </div>
          </div>
          <div v-if="modelServices.length === 0" class="empty-hint">
            暂无模型服务配置
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：近7天会话趋势 -->
    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span>近7天会话趋势</span></template>
          <div ref="trendChartRef" class="trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：快捷入口 -->
    <el-row :gutter="16" class="mt16 mb16">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span>快捷入口</span></template>
          <div class="quick-actions">
            <el-button type="primary" icon="Plus" @click="goRoute('/ai/agent')">创建Agent</el-button>
            <el-button type="success" icon="Collection" @click="goRoute('/ai/knowledge')">知识库管理</el-button>
            <el-button type="warning" icon="ChatDotRound" @click="goRoute('/ai/chat')">开始对话</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Index">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getOverview, getRecent, getTrend, getModelStatus } from '@/api/ai/dashboard'

const router = useRouter()

const trendChartRef = ref<HTMLElement>()

const overview = reactive({
  agentCount: 0,
  knowledgeCount: 0,
  documentCount: 0,
  chatCount: 0
})

const recent = reactive({
  agents: [] as any[],
  knowledge: [] as any[],
  conversations: [] as any[]
})

const recentTab = ref('agents')
const modelServices = ref<any[]>([])
const trendData = ref<any[]>([])

// 加载概览数据
function loadOverview() {
  getOverview().then((res: any) => {
    if (res.data) {
      overview.agentCount = res.data.agentCount ?? 0
      overview.knowledgeCount = res.data.knowledgeCount ?? 0
      overview.documentCount = res.data.documentCount ?? 0
      overview.chatCount = res.data.chatCount ?? 0
    }
  })
}

// 加载最近活动
function loadRecent() {
  getRecent().then((res: any) => {
    if (res.data) {
      recent.agents = res.data.agents || []
      recent.knowledge = res.data.knowledge || []
      recent.conversations = res.data.conversations || []
    }
  })
}

// 加载趋势数据
function loadTrend() {
  getTrend().then((res: any) => {
    if (res.data && res.data.data) {
      trendData.value = res.data.data
      nextTick(() => renderTrendChart())
    }
  })
}

// 加载模型状态
function loadModelStatus() {
  getModelStatus().then((res: any) => {
    if (res.data && res.data.services) {
      modelServices.value = res.data.services
    }
  })
}

// 渲染趋势图
function renderTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trendData.value.map((d: any) => d.date),
      axisLabel: { rotate: 30 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [{
      name: '会话数',
      type: 'line',
      data: trendData.value.map((d: any) => d.count),
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.35)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      },
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 2 }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

// 快捷入口跳转
function goRoute(path: string) {
  router.push(path)
}

onMounted(() => {
  loadOverview()
  loadRecent()
  loadTrend()
  loadModelStatus()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 4px 0;
  background: var(--el-bg-color-page, #f5f7fa);
  min-height: calc(100vh - 84px);
}

.mt16 { margin-top: 16px; }
.mb16 { margin-bottom: 16px; }

// 统计卡片
.stat-cards {
  .stat-card {
    border-radius: 8px;
    transition: transform 0.2s;
    &:hover { transform: translateY(-2px); }
    &__inner {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 4px 0;
    }
    &__icon {
      width: 56px;
      height: 56px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      color: #fff;
    }
    &__label {
      font-size: 13px;
      color: var(--el-text-color-secondary, #909399);
      margin-bottom: 6px;
    }
    &__value {
      font-size: 28px;
      font-weight: 700;
      color: var(--el-text-color-primary, #303133);
    }
    &--agent .stat-card__icon { background: linear-gradient(135deg, #409EFF, #337ECC); }
    &--knowledge .stat-card__icon { background: linear-gradient(135deg, #67C23A, #529B2E); }
    &--doc .stat-card__icon { background: linear-gradient(135deg, #E6A23C, #C28B2E); }
    &--chat .stat-card__icon { background: linear-gradient(135deg, #F56C6C, #D45353); }
  }
}

// 模型服务状态
.service-list {
  .service-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid var(--el-border-color-light, #EBEEF5);
    &:last-child { border-bottom: none; }
    .service-name { font-weight: 500; min-width: 110px; }
    .service-host { color: var(--el-text-color-secondary, #909399); font-size: 12px; flex: 1; text-align: center; }
  }
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary, #909399);
  padding: 20px;
}

// 趋势图
.trend-chart {
  width: 100%;
  height: 280px;
}

// 快捷入口
.quick-actions {
  display: flex;
  gap: 12px;
}

// 最近活动卡片
.recent-card,
.model-status-card {
  border-radius: 8px;
}
</style>
