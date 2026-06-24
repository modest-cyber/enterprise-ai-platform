<template>
  <div class="agent-thinking-block">
    <div v-for="(step, idx) in steps" :key="idx" class="think-step">
      <el-icon :size="14" class="step-icon"><CircleCheck /></el-icon>
      <span class="step-text">{{ step }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck } from '@element-plus/icons-vue'
defineProps<{ steps: string[] }>()
</script>

<style scoped lang="scss">
.agent-thinking-block {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
}
.think-step {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 2px 0;
  font-size: 12px;
  color: #374151;
  line-height: 1.5;
}
.step-icon { color: #22c55e; margin-top: 2px; flex-shrink: 0; }
.step-text {}
</style>