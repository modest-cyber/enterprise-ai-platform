import request from '@/utils/request'

export function listConversation(query) {
  return request({
    url: '/ai/chat/conversations',
    method: 'get',
    params: query
  })
}

export function getConversation(id) {
  return request({
    url: '/ai/chat/conversations/' + id,
    method: 'get'
  })
}

export function createConversation(data) {
  return request({
    url: '/ai/chat/conversations',
    method: 'post',
    data: data
  })
}

export function updateConversation(data) {
  return request({
    url: '/ai/chat/conversations',
    method: 'put',
    data: data
  })
}

export function deleteConversation(id) {
  return request({
    url: '/ai/chat/conversations/' + id,
    method: 'delete'
  })
}

export function renameConversation(id, data) {
  return request({
    url: '/ai/chat/conversations/' + id + '/rename',
    method: 'put',
    data: data
  })
}

export function generateTitle(id) {
  return request({
    url: '/ai/chat/conversations/' + id + '/generate-title',
    method: 'post'
  })
}

export function listMessages(id) {
  return request({
    url: '/ai/chat/conversations/' + id + '/messages',
    method: 'get'
  })
}

export function sendChat(data) {
  return request({
    url: '/ai/chat/send',
    method: 'post',
    data: data
  })
}