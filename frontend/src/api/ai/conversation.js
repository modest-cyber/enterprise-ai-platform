import request from '@/utils/request'

// 查询会话列表
export function listConversation(query) {
  return request({
    url: '/ai/conversation/list',
    method: 'get',
    params: query
  })
}

// 查询会话详细
export function getConversation(id) {
  return request({
    url: '/ai/conversation/' + id,
    method: 'get'
  })
}

// 新增会话
export function addConversation(data) {
  return request({
    url: '/ai/conversation',
    method: 'post',
    data: data
  })
}

// 修改会话
export function updateConversation(data) {
  return request({
    url: '/ai/conversation',
    method: 'put',
    data: data
  })
}

// 删除会话
export function delConversation(id) {
  return request({
    url: '/ai/conversation/' + id,
    method: 'delete'
  })
}

// 查询会话消息
export function listMessages(id) {
  return request({
    url: '/ai/conversation/' + id + '/messages',
    method: 'get'
  })
}