<template>
  <div class="code-diff-viewer" v-if="visible">
    <div class="diff-header">
      <span class="diff-title">代码差异</span>
      <el-button link size="small" @click="$emit('close')">关闭</el-button>
    </div>
    <div class="diff-body">
      <div class="diff-pane">
        <div class="pane-label">修改前</div>
        <div class="pane-content">
          <div v-for="(line, idx) in oldLines" :key="'o-' + idx" class="diff-line" :class="lineClass(line)">
            <span class="line-num">{{ idx + 1 }}</span>
            <span class="line-text">{{ line.text }}</span>
          </div>
        </div>
      </div>
      <div class="diff-pane">
        <div class="pane-label">修改后</div>
        <div class="pane-content">
          <div v-for="(line, idx) in newLines" :key="'n-' + idx" class="diff-line" :class="lineClass(line)">
            <span class="line-num">{{ idx + 1 }}</span>
            <span class="line-text">{{ line.text }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  oldCode: string
  newCode: string
}>()

defineEmits(['close'])

const oldLines = computed(() => parseLines(props.oldCode))
const newLines = computed(() => parseLines(props.newCode))

function parseLines(code: string): { text: string; type: string }[] {
  if (!code) return []
  return code.split('\n').map(line => {
    let type = 'normal'
    if (line.startsWith('+')) type = 'added'
    else if (line.startsWith('-')) type = 'removed'
    return { text: line, type }
  })
}

function lineClass(line: { type: string }) {
  return 'line-' + line.type
}
</script>

<style scoped lang="scss">
.code-diff-viewer {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 900px;
  max-width: 95vw;
  max-height: 80vh;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  z-index: 9999;
}
.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}
.diff-title { font-size: 14px; font-weight: 600; }
.diff-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.diff-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  &:first-child { border-right: 1px solid #e5e7eb; }
}
.pane-label {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}
.pane-content {
  flex: 1;
  overflow-y: auto;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
}
.diff-line {
  display: flex;
  padding: 0 4px;
  &.line-added { background: #dcfce7; }
  &.line-removed { background: #fee2e2; }
  &.line-normal {}
}
.line-num {
  width: 36px;
  text-align: right;
  padding-right: 8px;
  color: #9ca3af;
  user-select: none;
  flex-shrink: 0;
}
.line-text { white-space: pre; flex: 1; overflow-x: auto; }
</style>