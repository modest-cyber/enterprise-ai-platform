<template>
  <div class="chat-panel">
    <div class="chat-topbar">
      <ModeSwitcher />
    </div>

    <div class="chat-messages" ref="scrollRef">
      <div v-if="!store.currentSessionId" class="chat-welcome">
        <div class="welcome-icon"><el-icon :size="56"><ChatDotRound /></el-icon></div>
        <h2>{{ store.isAgentMode ? 'Agent 研发助手' : 'AI 研发助手' }}</h2>
        <p>{{ store.isAgentMode ? '输入研发任务，Agent 将自主完成代码分析、修改和执行' : '选择模型后开始对话，帮助解决研发问题' }}</p>
        <div class="example-grid">
          <div
            v-for="q in exampleQuestions"
            :key="q"
            class="example-card"
            @click="sendExample(q)"
          >{{ q }}</div>
        </div>
      </div>

      <div v-else class="msg-list">
        <div
          v-for="msg in store.messages"
          :key="msg.messageId || msg.id"
          :class="['msg-row', msg.role === 'user' ? 'row-user' : 'row-ai']"
        >
          <div :class="['msg-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
            <AgentThinkingBlock v-if="msg.agentSteps && msg.agentSteps.length" :steps="msg.agentSteps" />
            <FileChangeBlock v-if="msg.fileChanges && msg.fileChanges.length" :changes="msg.fileChanges" />
            <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>
        <div ref="messagesEnd" />
      </div>
    </div>

    <div class="chat-input-area">
      <div class="input-wrapper">
        <textarea
          ref="textareaRef"
          v-model="inputMessage"
          class="chat-input"
          :placeholder="store.isAgentMode ? '输入研发任务，Agent 将执行代码分析和修改...' : '输入消息，Enter 发送，Shift+Enter 换行'"
          rows="1"
          @keydown.enter.exact="handleKeyDown"
          @input="autoResize"
        ></textarea>
        <div class="input-toolbar">
          <div class="toolbar-left">
            <el-select
              v-model="store.modelId"
              placeholder="选择模型"
              size="small"
              class="model-select-inline"
            >
              <el-option
                v-for="m in store.modelOptions"
                :key="m.modelId"
                :label="m.displayName || m.modelName"
                :value="m.modelId"
              />
            </el-select>
            <span v-if="store.isAgentMode && store.agentStatus !== 'idle'" class="agent-status-badge" :class="'status-' + store.agentStatus">
              {{ statusLabels[store.agentStatus] || store.agentStatus }}
            </span>
          </div>
          <div class="toolbar-right">
            <el-button
              v-if="store.isAgentMode"
              size="small"
              @click="stopAgent"
              :disabled="store.agentStatus === 'idle' || store.agentStatus === 'finished'"
            >停止</el-button>
            <el-button
              type="primary"
              :icon="Promotion"
              @click="handleSend"
              :disabled="store.sending || !inputMessage.trim()"
              size="small"
            >
              {{ store.sending ? '发送中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="ChatPanel">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import useDevAssistantStore from '@/store/modules/devAssistant'
import ModeSwitcher from './ModeSwitcher.vue'
import AgentThinkingBlock from './AgentThinkingBlock.vue'
import FileChangeBlock from './FileChangeBlock.vue'

const store = useDevAssistantStore()
const inputMessage = ref('')
const scrollRef = ref<HTMLElement | null>(null)
const messagesEnd = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

let abortController: AbortController | null = null

const statusLabels: Record<string, string> = {
  idle: '空闲', thinking: '思考中...', planning: '规划中...',
  running: '执行中...', finished: '已完成', error: '错误'
}

const exampleQuestions = [
  '帮我写一个 SpringBoot 登录模块',
  '帮我分析这段代码',
  '帮我设计数据库',
  '帮我生成接口文档'
]

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

function handleKeyDown(e: KeyboardEvent) {
  if (!e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

async function sendExample(q: string) {
  inputMessage.value = q
  await nextTick()
  autoResize()
  handleSend()
}

async function handleSend() {
  const msg = inputMessage.value.trim()
  if (!msg || store.sending) return

  store.sending = true
  inputMessage.value = ''
  if (textareaRef.value) textareaRef.value.style.height = 'auto'

  if (!store.currentSessionId) {
    await store.createNewConversation(msg.substring(0, 30))
  }

  const userMsg = {
    messageId: Date.now(),
    role: 'user',
    content: msg,
    createTime: new Date().toISOString()
  }
  store.addMessage(userMsg)
  await scrollToBottom()

  const aiMsg: any = {
    messageId: Date.now() + 1,
    role: 'assistant',
    content: '',
    agentSteps: [],
    fileChanges: [],
    createTime: new Date().toISOString()
  }
  store.addMessage(aiMsg)
  await scrollToBottom()

  if (store.isAgentMode) {
    await runAgentMode(msg, aiMsg)
  } else {
    await runChatMode(msg, aiMsg)
  }

  store.sending = false
  await scrollToBottom()
}

async function runChatMode(message: string, aiMsg: any) {
  const token = getToken()
  abortController = new AbortController()

  const dto: any = { message, conversationId: store.currentSessionId }
  if (store.modelId) dto.modelId = Number(store.modelId)

  try {
    const response = await fetch('/api/v1/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(dto),
      signal: abortController.signal
    })

    if (!response.ok) throw new Error('HTTP ' + response.status)
    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue
        if (trimmed.startsWith('data: ')) {
          const dataStr = trimmed.substring(6)
          if (dataStr === '[DONE]') continue
          try {
            const data = JSON.parse(dataStr)
            if (data.type === 'token' && data.content) {
              aiMsg.content += data.content
            } else if (data.type === 'done') {
              if (data.content && !aiMsg.content) aiMsg.content = data.content
            } else if (data.type === 'error') {
              ElMessage.error(data.message || '聊天出错')
            }
          } catch (e) { /* non-JSON */ }
        }
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      ElMessage.error('连接失败: ' + (e.message || '未知错误'))
    }
    if (!aiMsg.content) store.messages.pop()
  }
}

async function runAgentMode(message: string, aiMsg: any) {
  const token = getToken()
  store.resetAgentState()
  store.setAgentStatus('running')
  store.taskDescription = message
  abortController = new AbortController()

  const dto: any = {
    message,
    agentId: null,
    modelId: store.modelId ? Number(store.modelId) : null,
    conversationId: Number(store.currentSessionId),
    repoPath: '.'
  }

  try {
    const response = await fetch('/api/v1/forge/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(dto),
      signal: abortController.signal
    })

    if (!response.ok) throw new Error('HTTP ' + response.status)
    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        if (trimmed.startsWith('data: ')) {
          const dataStr = trimmed.substring(6)
          try {
            const data = JSON.parse(dataStr)

            if (data.type === 'task_start') {
              store.setAgentStatus('thinking')
              store.addAgentStep('任务: ' + data.task)
              aiMsg.agentSteps.push('任务: ' + data.task)
            } else if (data.type === 'step') {
              store.setAgentStatus('running')
              const stepInfo = '[Step ' + data.step + '] ' + (data.toolName || '') + (data.thought ? ' - ' + data.thought.substring(0, 60) : '')
              store.addAgentStep(stepInfo)
              aiMsg.agentSteps.push(stepInfo)
              if (data.toolName) {
                store.addToolCall({ name: data.toolName, params: data.toolParams || '', step: data.step })
              }
            } else if (data.type === 'observation') {
              if (data.output && data.toolName) {
                const isWriteOp = ['file_write', 'file_edit', 'edit', 'write'].includes(data.toolName)
                if (isWriteOp) {
                  const fc = extractFileChange(data.toolName, data.output)
                  if (fc) {
                    store.addFileChange(fc)
                    aiMsg.fileChanges.push(fc)
                  }
                }
                const obsInfo = '[' + data.toolName + '] ' + (data.status === 'success' ? 'OK' : 'FAIL') + ' ' + (data.output || '').substring(0, 100)
                store.addAgentStep(obsInfo)
                aiMsg.agentSteps.push(obsInfo)
              }
            } else if (data.type === 'reflection') {
              store.addAgentStep('反思: ' + data.reason)
              aiMsg.agentSteps.push('反思: ' + data.reason)
            } else if (data.type === 'done') {
              store.setAgentStatus('finished')
              if (data.summary) {
                aiMsg.content = data.summary
              }
            } else if (data.type === 'error') {
              store.setAgentStatus('error')
              ElMessage.error(data.message || 'Agent 执行失败')
              aiMsg.content = '执行失败: ' + (data.message || '未知错误')
            }
          } catch (e) { /* non-JSON */ }
        }
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      store.setAgentStatus('error')
      ElMessage.error('Agent 连接失败: ' + (e.message || '未知错误'))
      aiMsg.content = '连接失败: ' + (e.message || '未知错误')
    }
  }
}

function extractFileChange(toolName: string, output: string) {
  const pathMatch = output.match(/([\/\\]?[\w.-]+[\/\\][\w.-]+\.(java|py|ts|js|vue|tsx|jsx|css|scss|html|xml|yml|yaml|json|md|sql|go|rs))/i)
  if (pathMatch) return { action: 'M', file: pathMatch[0] }
  if (toolName === 'file_write') {
    const writeMatch = output.match(/wrote|written|saved|created[:\\s]+(.+)/i)
    if (writeMatch) return { action: 'A', file: writeMatch[1].trim() }
  }
  return null
}

function stopAgent() {
  if (abortController) {
    abortController.abort()
    store.setAgentStatus('idle')
    store.sending = false
  }
}

function renderMarkdown(content: string): string {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
    return '<pre class="code-block"><code class="' + (lang ? 'lang-' + lang : '') + '">' + code.trim() + '</code></pre>'
  })
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^\* (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
  html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
  html = html.replace(/\n/g, '<br/>')

  return html
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesEnd.value) {
      messagesEnd.value.scrollIntoView({ behavior: 'smooth' })
    } else if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  store.switchMode('chat')
})
</script>

<style scoped lang="scss">
.chat-panel { flex: 1; display: flex; flex-direction: column; min-width: 0; background: #fff; }
.chat-topbar { display: flex; justify-content: space-between; align-items: center; padding: 8px 16px; border-bottom: 1px solid #e5e7eb; background: #fff; }
.model-select { width: 180px; }
.chat-messages { flex: 1; overflow-y: auto; background: #f7f7f8; }
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 20px; text-align: center; min-height: 100%; }
.chat-welcome h2 { font-size: 24px; font-weight: 600; margin: 16px 0 8px; color: #1f2937; }
.chat-welcome p { font-size: 15px; color: #6b7280; margin-bottom: 32px; }
.welcome-icon { color: #d1d5db; }
.example-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; max-width: 560px; width: 100%; }
.example-card { padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; cursor: pointer; transition: all 0.15s; }
.example-card:hover { background: #f0f0f0; border-color: #d1d5db; }
.msg-list { max-width: 820px; margin: 0 auto; padding: 24px 20px; display: flex; flex-direction: column; gap: 18px; }
.msg-row { display: flex; width: 100%; }
.row-user { justify-content: flex-end; }
.row-ai { justify-content: flex-start; }
.msg-bubble { max-width: 85%; padding: 14px 18px; border-radius: 14px; line-height: 1.65; font-size: 14px; word-break: break-word; }
.bubble-user { background: #3b82f6; color: #fff; border-bottom-right-radius: 4px; }
.bubble-ai { background: #fff; color: #1f2937; box-shadow: 0 1px 3px rgba(0,0,0,0.08); border-bottom-left-radius: 4px; }
:deep(.code-block) { background: #1e293b; color: #e2e8f0; padding: 14px 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; margin: 8px 0; white-space: pre-wrap; }
:deep(.inline-code) { background: #f3f4f6; padding: 1px 4px; border-radius: 3px; font-size: 13px; }
:deep(h2), :deep(h3), :deep(h4) { margin: 8px 0 4px; }
:deep(ul) { padding-left: 20px; }
:deep(blockquote) { border-left: 3px solid #d1d5db; padding-left: 12px; color: #6b7280; margin: 4px 0; }
.chat-input-area { border-top: 1px solid #e5e7eb; padding: 12px 20px 16px; background: #fff; }
.input-wrapper { max-width: 820px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; padding: 10px 14px; background: #fafbfc; }
.input-wrapper:focus-within { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.chat-input { width: 100%; border: none; outline: none; resize: none; font-size: 14px; line-height: 1.5; font-family: inherit; background: transparent; min-height: 24px; max-height: 200px; }
.chat-input::placeholder { color: #9ca3af; }
.input-toolbar { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; gap: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.model-select-inline { width: 160px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.agent-status-badge { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.status-idle { background: #e5e7eb; color: #6b7280; }
.status-thinking, .status-planning { background: #fef3c7; color: #92400e; }
.status-running { background: #dbeafe; color: #1e40af; }
.status-finished { background: #d1fae5; color: #065f46; }
.status-error { background: #fee2e2; color: #991b1b; }
</style>