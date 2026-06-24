<template>
  <div class="file-change-list">
    <div v-if="changes.length === 0" class="empty-hint">暂无变更</div>
    <div v-for="(change, idx) in changes" :key="idx" class="file-item" @click="$emit('viewDiff', change)">
      <span :class="['change-tag', 'tag-' + change.action]">{{ change.action }}</span>
      <span class="file-path">{{ change.file }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ changes: any[] }>()
defineEmits(['viewDiff'])
</script>

<style scoped lang="scss">
.file-change-list {}
.empty-hint { font-size: 12px; color: #9ca3af; padding: 8px 0; }
.file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 4px; cursor: pointer; font-size: 12px;
  &:hover { background: #f3f4f6; }
}
.change-tag {
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 4px; font-size: 10px; font-weight: 700;
  font-family: monospace; color: #fff; flex-shrink: 0;
}
.tag-A { background: #10b981; }
.tag-M { background: #f59e0b; }
.tag-D { background: #ef4444; }
.file-path { color: #1f2937; font-family: 'Consolas', monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
</style>