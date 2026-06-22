import request from '@/utils/request'

// 查询Agent列表
export function listAgent(query) {
  return request({ url: '/ai/agent/list', method: 'get', params: query })
}

// 查询Agent详情
export function getAgent(id) {
  return request({ url: '/ai/agent/' + id, method: 'get' })
}

// 新增Agent
export function addAgent(data) {
  return request({ url: '/ai/agent', method: 'post', data })
}

// 修改Agent
export function updateAgent(data) {
  return request({ url: '/ai/agent', method: 'put', data })
}

// 删除Agent
export function delAgent(ids) {
  return request({ url: '/ai/agent/' + ids, method: 'delete' })
}

// 同步执行Agent
export function executeAgent(data) {
  return request({ url: '/ai/agent/execute', method: 'post', data })
}

// 异步提交Agent任务
export function submitAgent(data) {
  return request({ url: '/ai/agent/submit', method: 'post', data })
}

// 查询异步任务状态
export function getTaskStatus(taskId) {
  return request({ url: '/ai/agent/status/' + taskId, method: 'get' })
}

// 取消异步任务
export function cancelTask(taskId) {
  return request({ url: '/ai/agent/cancel/' + taskId, method: 'post' })
}
