import request from '@/utils/request'

// 查询模型列表
export function listModel(query) {
  return request({ url: '/ai/model/list', method: 'get', params: query })
}

// 查询模型详情
export function getModel(id) {
  return request({ url: '/ai/model/' + id, method: 'get' })
}

// 新增模型
export function addModel(data) {
  return request({ url: '/ai/model', method: 'post', data })
}

// 修改模型
export function updateModel(data) {
  return request({ url: '/ai/model', method: 'put', data })
}

// 删除模型
export function delModel(ids) {
  return request({ url: '/ai/model/' + ids, method: 'delete' })
}

// 获取所有启用的模型（下拉列表用）
export function listEnabledModels() {
  return request({ url: '/ai/model/enabled', method: 'get' })
}

// 测试模型连接
export function testModel(id) {
  return request({ url: '/ai/model/test/' + id, method: 'get' })
}
