<template>
  <div class="file-change-block">
    <div class="block-title">文件变更</div>
    <div v-for="(change, idx) in changes" :key="idx" class="change-row">
      <span :class="['fc-action', 'fc-' + change.action]">{{ change.action }}</span>
      <span class="fc-file">{{ change.file }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ changes: any[] }>()
</script>

<style scoped lang="scss">
.file-change-block {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
}
.block-title {
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 6px;
}
.change-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
  font-size: 12px;
}
.fc-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.fc-A { background: #d1fae5; color: #065f46; }
.fc-M { background: #fef3c7; color: #92400e; }
.fc-D { background: #fee2e2; color: #991b1b; }
.fc-file { color: #374151; word-break: break-all; }
</style>