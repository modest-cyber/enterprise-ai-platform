<template>
  <div class="tool-call-list">
    <div v-if="calls.length === 0" class="empty-hint">暂无工具调用</div>
    <div v-for="(call, idx) in calls" :key="idx" class="tool-item">
      <div class="tool-header">
        <span class="tool-name">{{ call.name }}</span>
        <span class="tool-step">Step {{ call.step }}</span>
      </div>
      <div v-if="call.params" class="tool-params">{{ call.params }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ calls: any[] }>()
</script>

<style scoped lang="scss">
.tool-call-list {}
.empty-hint { font-size: 12px; color: #9ca3af; padding: 8px 0; }
.tool-item { padding: 8px 10px; margin-bottom: 6px; background: #f9fafb; border-radius: 6px; border: 1px solid #e5e7eb; }
.tool-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.tool-name { font-size: 12px; font-weight: 600; color: #374151; }
.tool-step { font-size: 11px; color: #9ca3af; }
.tool-params { font-size: 11px; color: #6b7280; word-break: break-all; }
</style>