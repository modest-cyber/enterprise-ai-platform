import request from '@/utils/request'

// 查询工具列表
export function listTool(query) {
  return request({ url: '/ai/tool/list', method: 'get', params: query })
}

// 查询工具详情
export function getTool(id) {
  return request({ url: '/ai/tool/' + id, method: 'get' })
}

// 新增工具
export function addTool(data) {
  return request({ url: '/ai/tool', method: 'post', data })
}

// 修改工具
export function updateTool(data) {
  return request({ url: '/ai/tool', method: 'put', data })
}

// 删除工具
export function delTool(ids) {
  return request({ url: '/ai/tool/' + ids, method: 'delete' })
}

// 调用工具
export function invokeTool(id, params) {
  return request({ url: '/ai/tool/invoke/' + id, method: 'post', data: params })
}

// 测试工具连通性
export function testTool(id) {
  return request({ url: '/ai/tool/test/' + id, method: 'post' })
}
