<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="handleNewSession" class="new-session-btn">
          <el-icon><Plus /></el-icon>
          <span>新建会话</span>
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="item in conversationList"
          :key="item.conversationId"
          :class="['conversation-item', { active: currentConversationId === item.conversationId }]"
          @click="handleSelectConversation(item)"
        >
          <div class="conversation-info">
            <div class="conversation-title-row">
              <span
                v-if="editingId !== item.conversationId"
                class="conversation-title"
                @dblclick="startRename(item)"
              >{{ item.title || '新会话' }}</span>
              <el-input
                v-else
                v-model="editingTitle"
                size="small"
                class="rename-input"
                @blur="confirmRename(item)"
                @keydown.enter="confirmRename(item)"
                @click.stop
                ref="renameInputRef"
              />
            </div>
            <div class="conversation-meta">
              <el-tag size="small" type="info">{{ item.agentType || '默认' }}</el-tag>
              <span class="conversation-time">{{ parseTime(item.updateTime || item.createTime) }}</span>
            </div>
          </div>
          <div class="conversation-actions" @click.stop>
            <el-tooltip content="重新生成标题" placement="top">
              <el-button link icon="Refresh" size="small" @click="handleRegenerateTitle(item)" />
            </el-tooltip>
            <el-tooltip content="重命名" placement="top">
              <el-button link icon="Edit" size="small" @click="startRename(item)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" icon="Delete" size="small" @click="handleDeleteConversation(item)" />
            </el-tooltip>
          </div>
        </div>
        <div v-if="conversationList.length === 0" class="no-conversation">
          暂无会话
        </div>
      </div>
    </div>

    <!-- 右侧主聊天区域 -->
    <div class="chat-main">
      <!-- 可滚动内容区 -->
      <div class="chat-content" ref="scrollContainer">
        <!-- 欢迎页 / 空状态：没有消息时显示 -->
        <div v-if="messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">
            <el-icon :size="64"><ChatDotRound /></el-icon>
          </div>
          <h1 v-if="currentConversationId">开始新的对话</h1>
          <h1 v-else>你好，我是 AI 助手</h1>
          <p v-if="currentConversationId">发送消息开始与 AI 交流</p>
          <p v-else>今天有什么可以帮助你的？</p>

          <!-- 示例问题：仅在首次进入（无会话）时展示 -->
          <div v-if="!currentConversationId" class="example-section">
            <div class="example-label">示例问题：</div>
            <div class="example-grid">
              <div
                v-for="q in exampleQuestions"
                :key="q"
                class="example-card"
                @click="sendExample(q)"
              >
                {{ q }}
              </div>
            </div>
          </div>
        </div>

        <!-- 消息列表：有消息时显示 -->
        <div v-else class="chat-messages">
          <div
            v-for="msg in messages"
            :key="msg.messageId || msg.id"
            :class="['message-row', msg.role === 'user' ? 'row-user' : 'row-ai']"
          >
            <div class="message-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
              <div class="bubble-text" v-html="formatMessage(msg.content)"></div>
            </div>
          </div>
          <div ref="messagesEnd" />
        </div>
      </div>

      <!-- 输入区域：始终显示，固定在底部 -->
      <div class="chat-input-area">
        <div class="input-wrapper">
          <textarea
            ref="textareaRef"
            v-model="inputMessage"
            class="chat-textarea"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            rows="1"
            @keydown.enter.exact="handleKeyDown"
            @input="autoResize"
          ></textarea>
          <div class="input-toolbar">
            <div class="input-selects">
              <el-select v-model="modelId" placeholder="选择模型" size="small" class="input-select" clearable>
                <el-option
                  v-for="m in modelOptions"
                  :key="m.modelId"
                  :label="m.displayName || m.modelName"
                  :value="m.modelId"
                />
              </el-select>
              <el-select v-model="agentId" placeholder="Agent类型" size="small" class="input-select" clearable>
                <el-option
                  v-for="a in agentOptions"
                  :key="a.agentId"
                  :label="a.agentName"
                  :value="a.agentId"
                />
              </el-select>
            </div>
            <el-button
              type="primary"
              :icon="Promotion"
              @click="handleSend"
              :disabled="sending || !inputMessage.trim()"
            >
              {{ sending ? '发送中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="AiChat">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Plus, Promotion } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/ruoyi'
import { getToken } from '@/utils/auth'
import { listConversation, createConversation, deleteConversation, renameConversation, generateTitle, listMessages } from '@/api/ai/chat'
import { listEnabledAgents } from '@/api/ai/agent'
import { listEnabledModels } from '@/api/ai/model'

const conversationList = ref<any[]>([])
const currentConversationId = ref<number | null>(null)
const messages = ref<any[]>([])
const inputMessage = ref('')
const modelId = ref<number | null>(null)
const modelOptions = ref<any[]>([])
const agentId = ref<number | null>(null)
const agentOptions = ref<any[]>([])
const sending = ref(false)
const scrollContainer = ref<HTMLElement | null>(null)
const messagesEnd = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const renameInputRef = ref<any>(null)

const editingId = ref<number | null>(null)
const editingTitle = ref('')

const conversationParams = reactive({
  pageNum: 1,
  pageSize: 50
})

const exampleQuestions = [
  '帮我写一个 SpringBoot 登录模块',
  '帮我分析这段代码',
  '帮我设计数据库',
  '帮我生成接口文档'
]

// ── 新建会话 ──
function handleNewSession() {
  currentConversationId.value = null
  messages.value = []
  inputMessage.value = ''
  // 重置输入框高度
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  })
}

// ── 选择会话 ──
function handleSelectConversation(item: any) {
  currentConversationId.value = item.conversationId
  loadMessages(item.conversationId)
}

function loadMessages(conversationId: number) {
  listMessages(conversationId).then((response: any) => {
    messages.value = response.data || []
    scrollToBottom()
  })
}

// ── 发送消息 ──
async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message || sending.value) return

  sending.value = true
  inputMessage.value = ''
  // 重置输入框高度
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  // 添加用户消息到界面
  const userMsg = {
    messageId: Date.now(),
    role: 'user',
    content: message,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMsg)
  await scrollToBottom()

  // 添加 AI 占位气泡（流式填充）
  const aiMsg = {
    messageId: Date.now() + 1,
    role: 'assistant',
    content: '',
    createTime: new Date().toISOString()
  }
  messages.value.push(aiMsg)
  await scrollToBottom()

  // 如果没有会话，先调用后端接口创建会话
  if (!currentConversationId.value) {
    try {
      const createBody: any = { title: message.substring(0, 30) }
      if (agentId.value) createBody.agentId = agentId.value
      if (modelId.value) createBody.modelId = modelId.value
      const createRes = await createConversation(createBody)
      const convData = createRes.data
      currentConversationId.value = convData.conversationId
      // 插入到会话列表顶部
      conversationList.value.unshift({
        conversationId: convData.conversationId,
        title: message.substring(0, 30),
        agentType: agentOptions.value.find(a => a.agentId === agentId.value)?.agentName || '',
        updateTime: new Date().toISOString(),
        createTime: new Date().toISOString()
      })
    } catch (e: any) {
      ElMessage.error('创建会话失败')
      sending.value = false
      // 移除 AI 占位消息
      messages.value.pop()
      return
    }
  }

  // 构建 SSE 请求体
  const dto: any = {
    message,
    conversationId: currentConversationId.value,
    agentId: agentId.value || null,
    modelId: modelId.value || null
  }

  try {
    const token = getToken()
    const response = await fetch('/api/v1/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify(dto)
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

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

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        if (!line) continue

        if (line.startsWith('data: ')) {
          const dataStr = line.substring(6)
          if (dataStr === '[DONE]') continue

          try {
            const data = JSON.parse(dataStr)

            if (data.type === 'token' && data.content) {
              aiMsg.content += data.content
            } else if (data.type === 'done') {
              if (data.content && !aiMsg.content) {
                aiMsg.content = data.content
              }
            } else if (data.type === 'error') {
              ElMessage.error(data.message || '聊天出错')
            }
          } catch {
            // 非 JSON 数据，跳过
          }
        } else if (line.startsWith('event: ')) {
          const eventType = line.substring(7)
          if (eventType === 'done') {
            // SSE done 事件：刷新会话列表以同步后端状态
            refreshConversationListKeepCurrent()
          }
        }
      }
    }

    // SSE 流结束后刷新会话列表
    refreshConversationListKeepCurrent()
  } catch (e: any) {
    console.error('SSE 连接失败:', e)
    ElMessage.error('连接失败: ' + (e.message || '未知错误'))
    // 移除空的 AI 占位消息
    if (!aiMsg.content) {
      messages.value.pop()
    }
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function handleKeyDown(event: KeyboardEvent) {
  if (!event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

// ── 点示例问题直接发送 ──
function sendExample(question: string) {
  inputMessage.value = question
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = textareaRef.value.scrollHeight + 'px'
    }
  })
  handleSend()
}

// ── 输入框自动撑开高度 ──
function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

// ── 会话列表 ──
function fetchConversationList() {
  listConversation(conversationParams).then((response: any) => {
    conversationList.value = response.rows || []
  })
}

function refreshConversationListKeepCurrent() {
  listConversation(conversationParams).then((response: any) => {
    conversationList.value = response.rows || []
  })
}

function handleDeleteConversation(item: any) {
  ElMessageBox.confirm('确定要删除该会话吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteConversation(item.conversationId).then(() => {
      ElMessage.success('删除成功')
      if (currentConversationId.value === item.conversationId) {
        handleNewSession()
      }
      fetchConversationList()
    })
  }).catch(() => {})
}

function startRename(item: any) {
  editingId.value = item.conversationId
  editingTitle.value = item.title || ''
  nextTick(() => {
    if (renameInputRef.value) {
      renameInputRef.value.focus()
    }
  })
}

function confirmRename(item: any) {
  const newTitle = editingTitle.value.trim()
  editingId.value = null
  if (!newTitle || newTitle === item.title) return

  renameConversation(item.conversationId, { title: newTitle }).then(() => {
    item.title = newTitle
  }).catch(() => {
    ElMessage.error('重命名失败')
  })
}

function handleRegenerateTitle(item: any) {
  generateTitle(item.conversationId).then((response: any) => {
    const newTitle = response.data
    item.title = newTitle
    ElMessage.success('标题已更新')
  }).catch(() => {
    ElMessage.error('生成标题失败')
  })
}

// ── 滚动到底部 ──
function scrollToBottom() {
  nextTick(() => {
    if (messagesEnd.value) {
      messagesEnd.value.scrollIntoView({ behavior: 'smooth' })
    } else if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

function formatMessage(content: string): string {
  if (!content) return ''
  return content.replace(/\n/g, '<br/>')
}

// ── 下拉选项 ──
function fetchAgentOptions() {
  listEnabledAgents().then((response: any) => {
    agentOptions.value = response.data || []
  })
}

function fetchModelOptions() {
  listEnabledModels().then((response: any) => {
    modelOptions.value = response.data || []
  })
}

onMounted(() => {
  fetchConversationList()
  fetchAgentOptions()
  fetchModelOptions()
})
</script>

<style scoped lang="scss">
// ── 变量 ──
$sidebar-width: 280px;
$bg-page: #f7f7f8;
$bg-sidebar: #f9fafb;
$bg-main: #ffffff;
$bubble-user-bg: #3b82f6;
$bubble-ai-bg: #ffffff;
$text-primary: #1f2937;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$border-color: #e5e7eb;
$radius-md: 12px;
$radius-lg: 16px;
$shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
$shadow-md: 0 2px 10px rgba(0, 0, 0, 0.06);
$max-msg-width: 800px;

// ── 整体容器 ──
.chat-container {
  display: flex;
  height: calc(100vh - 84px);
  background-color: $bg-page;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

// ── 左侧边栏 ──
.chat-sidebar {
  width: $sidebar-width;
  min-width: $sidebar-width;
  background: $bg-sidebar;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 14px 12px;
  border-bottom: 1px solid $border-color;
}

.new-session-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 2px;
  }
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  margin-bottom: 2px;

  &:hover {
    background-color: #e5e7eb;
    .conversation-actions { opacity: 1; }
  }
  &.active {
    background-color: #e0e7ff;
    border: 1px solid #c7d2fe;
  }
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title-row {
  margin-bottom: 4px;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.rename-input {
  max-width: 180px;
}

.conversation-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.conversation-time {
  font-size: 11px;
  color: $text-muted;
}

.conversation-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  margin-left: 4px;
  flex-shrink: 0;
}

.no-conversation {
  text-align: center;
  color: $text-muted;
  padding: 40px 16px;
  font-size: 14px;
}

// ── 右侧主区域 ──
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: $bg-main;
  position: relative;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 3px;
  }
}

// ── 欢迎页 / 空状态 ──
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px 40px;
  text-align: center;
  min-height: 100%;
}

.welcome-icon {
  color: #d1d5db;
  margin-bottom: 20px;
}

.chat-welcome h1 {
  font-size: 26px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 10px;
}

.chat-welcome p {
  font-size: 16px;
  color: $text-secondary;
  margin: 0 0 40px;
}

.example-section {
  width: 100%;
  max-width: 560px;
}

.example-label {
  font-size: 14px;
  color: $text-muted;
  margin-bottom: 12px;
}

.example-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.example-card {
  padding: 12px 16px;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  font-size: 14px;
  color: $text-primary;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
  background: #fafafa;

  &:hover {
    background: #f0f0f0;
    border-color: #d1d5db;
    box-shadow: $shadow-sm;
  }
}

// ── 消息列表 ──
.chat-messages {
  max-width: $max-msg-width;
  margin: 0 auto;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-row {
  display: flex;
  width: 100%;
}

.row-user {
  justify-content: flex-end;
}

.row-ai {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 18px;
  border-radius: $radius-lg;
  line-height: 1.65;
  font-size: 15px;
  word-break: break-word;
}

.bubble-user {
  background: $bubble-user-bg;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: $bubble-ai-bg;
  color: $text-primary;
  box-shadow: $shadow-md;
  border-bottom-left-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
}

// ── 输入区域 ──
.chat-input-area {
  border-top: 1px solid $border-color;
  background: $bg-main;
  padding: 12px 20px 20px;
}

.input-wrapper {
  max-width: $max-msg-width;
  margin: 0 auto;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  padding: 10px 14px;
  background: #fafbfc;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:focus-within {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    background: #fff;
  }
}

.chat-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.5;
  font-family: inherit;
  background: transparent;
  color: $text-primary;
  min-height: 24px;
  max-height: 200px;

  &::placeholder {
    color: $text-muted;
  }
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  gap: 12px;
}

.input-selects {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-select {
  width: 170px;
}

// ── 响应式 ──
@media (max-width: 768px) {
  .chat-sidebar {
    width: 240px;
    min-width: 240px;
  }
  .chat-messages {
    padding: 16px 12px;
  }
  .message-bubble {
    max-width: 90%;
  }
  .example-grid {
    grid-template-columns: 1fr;
  }
}
</style>
