import request from '@/utils/request'

export function listConversations(params) {
  return request({ url: '/ai/chat/conversations', method: 'get', params })
}

export function createConversation(data) {
  return request({ url: '/ai/chat/conversations', method: 'post', data })
}

export function deleteConversation(id) {
  return request({ url: '/ai/chat/conversations/' + id, method: 'delete' })
}

export function renameConversation(id, data) {
  return request({ url: '/ai/chat/conversations/' + id + '/rename', method: 'put', data })
}

export function listMessages(conversationId) {
  return request({ url: '/ai/chat/conversations/' + conversationId + '/messages', method: 'get' })
}

export function listEnabledModels() {
  return request({ url: '/ai/model/list', method: 'get' })
}