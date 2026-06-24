<template>
  <div class="dev-assistant-container">
    <SessionList />
    <ChatPanel />
    <AgentPanel v-if="store.isAgentMode" />
    <CodeDiffViewer
      :visible="store.showDiff"
      :oldCode="store.diffOldCode"
      :newCode="store.diffNewCode"
      @close="store.closeDiff()"
    />
  </div>
</template>

<script setup lang="ts" name="DevAssistant">
import { onMounted } from 'vue'
import useDevAssistantStore from '@/store/modules/devAssistant'
import SessionList from './components/SessionList.vue'
import ChatPanel from './components/ChatPanel.vue'
import AgentPanel from './components/AgentPanel.vue'
import CodeDiffViewer from './components/CodeDiffViewer.vue'

const store = useDevAssistantStore()

onMounted(() => {
  store.fetchConversationList()
  store.fetchModelOptions()
})
</script>

<style scoped lang="scss">
.dev-assistant-container {
  display: flex;
  height: calc(100vh - 84px);
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>