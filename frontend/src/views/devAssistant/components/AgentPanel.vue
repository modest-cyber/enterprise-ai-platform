<template>
  <div class="agent-panel">
    <div class="panel-header">
      <span class="panel-title">Agent 运行面板</span>
      <el-button link size="small" @click="store.resetAgentState()">清除</el-button>
    </div>

    <div class="panel-scroll">
      <AgentStatus :status="store.agentStatus" />

      <div class="panel-section">
        <div class="section-title">当前步骤</div>
        <div class="step-list">
          <div v-if="store.agentSteps.length === 0" class="empty-hint">等待执行...</div>
          <div v-for="(step, idx) in store.agentSteps" :key="idx" class="step-item">
            <span class="step-idx">[{{ idx + 1 }}]</span>
            <span class="step-text">{{ step }}</span>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <div class="section-title">工具调用</div>
        <ToolCallList :calls="store.toolCalls" />
      </div>

      <div class="panel-section">
        <div class="section-title">文件变更</div>
        <FileChangeList :changes="store.fileChanges" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AgentStatus from './AgentStatus.vue'
import ToolCallList from './ToolCallList.vue'
import FileChangeList from './FileChangeList.vue'
import useDevAssistantStore from '@/store/modules/devAssistant'

const store = useDevAssistantStore()
</script>

<style scoped lang="scss">
.agent-panel {
  width: 280px;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e5e7eb;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}
.panel-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.panel-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.panel-section {
  margin-top: 16px;
}
.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.step-item {
  font-size: 12px;
  color: #374151;
  padding: 4px 0;
  line-height: 1.5;
  word-break: break-all;
}
.step-idx { color: #9ca3af; margin-right: 4px; }
.step-text {}
.empty-hint { font-size: 12px; color: #9ca3af; padding: 8px 0; }
</style>