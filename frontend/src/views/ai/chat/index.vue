<template>
  <div class="chat-container">
    <div class="chat-left">
      <div class="chat-left-header">
        <el-button type="primary" icon="Plus" @click="handleNewSession" class="new-session-btn">新建会话</el-button>
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
              <span v-if="editingId !== item.conversationId" class="conversation-title" @dblclick="startRename(item)">{{ item.title || '新会话' }}</span>
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

    <div class="chat-right">
      <div v-if="!currentConversationId && messages.length === 0" class="chat-empty">
        <div class="empty-icon">
          <el-icon :size="80"><ChatDotRound /></el-icon>
        </div>
        <p>输入消息开始对话</p>
      </div>

      <div v-else class="chat-window">
        <div class="chat-messages" ref="messageContainer">
          <div
            v-for="msg in messages"
            :key="msg.messageId || msg.id"
            :class="['message-item', msg.role === 'user' ? 'message-user' : 'message-ai']"
          >
            <div class="message-avatar">
              <el-icon :size="28" v-if="msg.role === 'user'"><UserFilled /></el-icon>
              <el-icon :size="28" v-else><Cpu /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-role">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div class="message-time">{{ parseTime(msg.createTime) }}</div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息，Enter发送，Shift+Enter换行"
            @keydown.enter.exact="handleKeyDown"
            resize="none"
          />
          <div class="chat-input-actions">
            <el-select v-model="agentType" placeholder="Agent类型" style="width: 140px" size="default">
              <el-option label="默认" value="" />
              <el-option label="规划(Planner)" value="planner" />
              <el-option label="RAG检索" value="rag" />
              <el-option label="代码生成" value="code" />
              <el-option label="代码审查" value="review" />
            </el-select>
            <el-select v-model="modelId" placeholder="选择模型" style="width: 180px; margin-left: 12px;" size="default" clearable filterable>
              <el-option v-for="m in modelOptions" :key="m.modelId" :label="m.displayName" :value="m.modelId" />
            </el-select>
            <el-button type="primary" icon="Promotion" @click="handleSend" :disabled="sending || !inputMessage.trim()" style="margin-left: auto;">{{ sending ? '发送中...' : '发送' }}</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="AiChat">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, UserFilled, Cpu } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/ruoyi'
import { listConversation, deleteConversation, renameConversation, generateTitle, listMessages, sendChat } from '@/api/ai/chat'
import { listModel } from '@/api/ai/model'

const conversationList = ref<any[]>([])
const currentConversationId = ref<number | null>(null)
const messages = ref<any[]>([])
const inputMessage = ref('')
const agentType = ref('')
const modelId = ref<number | null>(null)
const modelOptions = ref<any[]>([])
const sending = ref(false)
const messageContainer = ref<HTMLElement | null>(null)
const renameInputRef = ref<any>(null)

const editingId = ref<number | null>(null)
const editingTitle = ref('')

const conversationParams = reactive({
  pageNum: 1,
  pageSize: 50
})

function loadModelOptions() {
  listModel({ pageNum: 1, pageSize: 1000, isEnabled: 1 }).then((response: any) => {
    modelOptions.value = response.rows || []
  })
}

function handleNewSession() {
  currentConversationId.value = null
  messages.value = []
  inputMessage.value = ''
}

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

async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message || sending.value) return

  sending.value = true
  inputMessage.value = ''

  const localUserMsg = {
    messageId: Date.now(),
    conversationId: currentConversationId.value,
    role: 'user',
    content: message,
    createTime: new Date().toISOString()
  }
  messages.value.push(localUserMsg)
  await scrollToBottom()

  try {
    const dto: any = {
      message: message,
      agentType: agentType.value || null
    }
    if (modelId.value) {
      dto.modelId = modelId.value
    }
    if (currentConversationId.value) {
      dto.conversationId = currentConversationId.value
    }

    const response = await sendChat(dto)
    const data = response.data

    const aiMsg = {
      messageId: data.messageId || Date.now(),
      conversationId: data.conversationId,
      role: data.role || 'assistant',
      content: data.content,
      createTime: new Date().toISOString()
    }
    messages.value.push(aiMsg)

    if (!currentConversationId.value) {
      currentConversationId.value = data.conversationId
      fetchConversationList()
    } else if (data.title) {
      const conv = conversationList.value.find(c => c.conversationId === data.conversationId)
      if (conv) conv.title = data.title
    }

    await scrollToBottom()
  } catch {
    ElMessage.error('发送消息失败')
    messages.value.pop()
  } finally {
    sending.value = false
  }
}

function handleKeyDown(event: KeyboardEvent) {
  if (!event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function fetchConversationList() {
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

function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

function formatMessage(content: string): string {
  if (!content) return ''
  return content.replace(/\n/g, '<br/>')
}

onMounted(() => {
  fetchConversationList()
  loadModelOptions()
})
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: calc(100vh - 84px);
  background-color: var(--el-bg-color-page, #f5f7fa);
}
.chat-left {
  width: 280px;
  min-width: 280px;
  background-color: var(--el-bg-color, #fff);
  border-right: 1px solid var(--el-border-color-light, #e8e8e8);
  display: flex;
  flex-direction: column;
}
.chat-left-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light, #e8e8e8);
}
.new-session-btn { width: 100%; }
.conversation-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
  &:hover { background-color: var(--el-fill-color-light, #f5f7fa); }
  &.active { background-color: var(--el-color-primary-light-9, #ecf5ff); border-left: 3px solid var(--el-color-primary, #409eff); }
}
.conversation-info { flex: 1; min-width: 0; }
.conversation-title-row { margin-bottom: 4px; }
.conversation-title {
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rename-input { max-width: 180px; }
.conversation-meta { display: flex; align-items: center; gap: 8px; }
.conversation-time { font-size: 12px; color: var(--el-text-color-secondary, #909399); }
.conversation-actions {
  display: flex; align-items: center; gap: 2px;
  opacity: 0; transition: opacity 0.15s; margin-left: 4px;
}
.conversation-item:hover .conversation-actions { opacity: 1; }
.no-conversation { text-align: center; color: var(--el-text-color-secondary, #909399); padding: 40px 16px; font-size: 14px; }
.chat-right {
  flex: 1; display: flex; flex-direction: column;
  background-color: var(--el-bg-color, #fff); overflow: hidden;
}
.chat-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: var(--el-text-color-secondary, #909399);
  .empty-icon { color: var(--el-text-color-placeholder, #c0c4cc); margin-bottom: 16px; }
  p { font-size: 16px; }
}
.chat-window { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; background-color: var(--el-bg-color-page, #f5f7fa); }
.message-item { display: flex; margin-bottom: 20px; align-items: flex-start;
  &.message-ai { flex-direction: row; }
  &.message-user { flex-direction: row-reverse; }
}
.message-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background-color: var(--el-border-color-extra-light, #e8ecf4);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  .message-user & { background-color: var(--el-color-primary-light-8, #d9e9ff); color: var(--el-color-primary, #409eff); margin-left: 12px; }
  .message-ai & { background-color: var(--el-color-success-light-9, #e8f5e9); color: var(--el-color-success, #67c23a); margin-right: 12px; }
}
.message-content { max-width: 70%; }
.message-role { font-size: 13px; color: var(--el-text-color-secondary, #909399); margin-bottom: 4px; }
.message-text {
  padding: 12px 16px; border-radius: 8px; line-height: 1.6; font-size: 14px; word-break: break-word;
  .message-user & { background-color: var(--el-color-primary, #409eff); color: #fff; }
  .message-ai & { background-color: var(--el-bg-color, #fff); color: var(--el-text-color-primary, #303133); box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08); }
}
.message-time { font-size: 12px; color: var(--el-text-color-placeholder, #c0c4cc); margin-top: 4px; }
.chat-input { padding: 16px 20px; border-top: 1px solid var(--el-border-color-light, #e8e8e8); background-color: var(--el-bg-color, #fff); }
.chat-input-actions { display: flex; align-items: center; margin-top: 12px; }
</style>