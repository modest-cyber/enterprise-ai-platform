<template>
  <div class="mode-switcher">
    <div class="switcher-track" :class="{ agent: store.isAgentMode }">
      <button
        :class="['switcher-btn', { active: !store.isAgentMode }]"
        @click="store.switchMode('chat')"
      >对话模式</button>
      <button
        :class="['switcher-btn', { active: store.isAgentMode }]"
        @click="store.switchMode('agent')"
      >Agent 模式</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import useDevAssistantStore from '@/store/modules/devAssistant'
const store = useDevAssistantStore()
</script>

<style scoped lang="scss">
.mode-switcher {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}
.switcher-track {
  display: flex;
  background: #e5e7eb;
  border-radius: 8px;
  padding: 2px;
}
.switcher-btn {
  padding: 4px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  background: transparent;
  color: #6b7280;
  transition: all 0.2s;
  &.active {
    background: #fff;
    color: #111827;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  }
}
</style>