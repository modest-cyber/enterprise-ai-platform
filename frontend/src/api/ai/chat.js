import request from '@/utils/request'

// 发送聊天消息
export function sendChat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data: data
  })
}