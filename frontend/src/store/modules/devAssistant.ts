import { defineStore } from 'pinia'
import {
  listConversations, createConversation, deleteConversation,
  renameConversation, listMessages
} from '@/api/devAssistant'
import { listEnabledModels } from '@/api/ai/model'

const useDevAssistantStore = defineStore('devAssistant', {
  state: () => ({
    sessions: [] as any[],
    currentSessionId: null as number | null,
    messages: [] as any[],
    isAgentMode: false as boolean,
    modelId: null as number | null,
    modelOptions: [] as any[],
    sending: false as boolean,
    agentStatus: 'idle' as string,
    agentSteps: [] as string[],
    toolCalls: [] as { name: string; params: string; step: number }[],
    fileChanges: [] as { action: string; file: string }[],
    taskDescription: '' as string,
    diffTarget: null as { action: string; file: string } | null,
    showDiff: false as boolean,
    diffOldCode: '' as string,
    diffNewCode: '' as string
  }),

  actions: {
    switchMode(mode: 'chat' | 'agent') {
      this.isAgentMode = mode === 'agent'
    },

    async fetchConversationList() {
      const res: any = await listConversations({ pageNum: 1, pageSize: 100 })
      this.sessions = res.rows || []
    },

    async fetchModelOptions() {
      try {
        const res: any = await listEnabledModels()
        this.modelOptions = res.data || []
      } catch { /* ignore */ }
    },

    async createNewConversation(title?: string) {
      const res: any = await createConversation({ title: title || '新会话', modelId: this.modelId })
      const id = res.data
      if (id) {
        this.currentSessionId = id
        this.messages = []
        this.resetAgentState()
        await this.fetchConversationList()
      }
      return id
    },

    async selectSession(id: number) {
      this.currentSessionId = id
      this.messages = []
      this.resetAgentState()
      try {
        const res: any = await listMessages(id)
        this.messages = res.data || []
      } catch { /* no history */ }
    },

    async deleteConversationById(id: number) {
      await deleteConversation(id)
      if (this.currentSessionId === id) {
        this.currentSessionId = null
        this.messages = []
        this.resetAgentState()
      }
      await this.fetchConversationList()
    },

    async renameConversationById(id: number, title: string) {
      await renameConversation(id, title)
      const s = this.sessions.find((c: any) => c.conversationId === id)
      if (s) s.title = title
    },

    addMessage(msg: any) {
      this.messages.push(msg)
    },

    addAgentStep(step: string) {
      this.agentSteps.push(step)
    },

    addToolCall(call: { name: string; params: string; step: number }) {
      this.toolCalls.push(call)
    },

    addFileChange(change: { action: string; file: string }) {
      if (!this.fileChanges.some((f: any) => f.file === change.file)) {
        this.fileChanges.push(change)
      }
    },

    setAgentStatus(status: string) {
      this.agentStatus = status
    },

    resetAgentState() {
      this.agentStatus = 'idle'
      this.agentSteps = []
      this.toolCalls = []
      this.fileChanges = []
      this.taskDescription = ''
    },

    setDiffTarget(change: { action: string; file: string } | null) {
      this.diffTarget = change
    },

    openDiff(oldCode: string, newCode: string) {
      this.diffOldCode = oldCode
      this.diffNewCode = newCode
      this.showDiff = true
    },

    closeDiff() {
      this.showDiff = false
      this.diffOldCode = ''
      this.diffNewCode = ''
    }
  }
})

export default useDevAssistantStore