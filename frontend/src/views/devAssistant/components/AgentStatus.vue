<template>
  <div class="agent-status">
    <div class="status-indicator" :class="'status-' + status">
      <span class="status-dot"></span>
      <span class="status-label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const labelMap: Record<string, string> = {
  idle: '空闲',
  thinking: '思考中...',
  planning: '规划中...',
  running: '执行中...',
  finished: '已完成',
  error: '错误'
}

const label = computed(() => labelMap[props.status] || props.status)
</script>

<style scoped lang="scss">
.agent-status { padding: 8px 0; }
.status-indicator { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px; font-size: 13px; font-weight: 500; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-idle { background: #f3f4f6; color: #6b7280; }
.status-idle .status-dot { background: #9ca3af; }
.status-thinking, .status-planning { background: #fef3c7; color: #92400e; }
.status-thinking .status-dot, .status-planning .status-dot { background: #f59e0b; }
.status-running { background: #dbeafe; color: #1e40af; }
.status-running .status-dot { background: #3b82f6; animation: pulse 1.5s infinite; }
.status-finished { background: #d1fae5; color: #065f46; }
.status-finished .status-dot { background: #10b981; }
.status-error { background: #fee2e2; color: #991b1b; }
.status-error .status-dot { background: #ef4444; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
</style>