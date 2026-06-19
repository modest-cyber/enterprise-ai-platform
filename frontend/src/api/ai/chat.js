import request from '@/utils/request'

// 查询会话列表
export function listConversation(query) {
  return request({
    url: '/ai/chat/conversations',
    method: 'get',
    params: query
  })
}

// 查询会话详细
export function getConversation(id) {
  return request({
    url: '/ai/chat/conversations/' + id,
    method: 'get'
  })
}

// 新增会话
export function createConversation(data) {
  return request({
    url: '/ai/chat/conversations',
    method: 'post',
    data: data
  })
}

// 修改会话
export function updateConversation(data) {
  return request({
    url: '/ai/chat/conversations',
    method: 'put',
    data: data
  })
}

// 删除会话
export function deleteConversation(id) {
  return request({
    url: '/ai/chat/conversations/' + id,
    method: 'delete'
  })
}

// 查询会话消息
export function listMessages(id) {
  return request({
    url: '/ai/chat/conversations/' + id + '/messages',
    method: 'get'
  })
}

// 发送聊天消息
export function sendChat(data) {
  return request({
    url: '/ai/chat/send',
    method: 'post',
    data: data
  })
}