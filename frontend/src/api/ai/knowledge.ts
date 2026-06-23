import request from '@/utils/request'

// ==================== 知识库 CRUD ====================
export function listKnowledge(query) {
  return request({ url: '/ai/kb/list', method: 'get', params: query })
}
export function getKnowledge(id) {
  return request({ url: '/ai/kb/' + id, method: 'get' })
}
export function addKnowledge(data) {
  return request({ url: '/ai/kb', method: 'post', data })
}
export function updateKnowledge(data) {
  return request({ url: '/ai/kb', method: 'put', data })
}
export function deleteKnowledge(ids) {
  return request({ url: '/ai/kb/' + ids, method: 'delete' })
}

// ==================== 文档管理 ====================
export function uploadDocument(data) {
  return request({
    url: '/ai/kb/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export function listDocuments(id) {
  return request({ url: '/ai/kb/' + id + '/doc/list', method: 'get' })
}
export function deleteDocument(docId) {
  return request({ url: '/ai/kb/doc/' + docId, method: 'delete' })
}

// ==================== 文档文件 ====================
export function getDocumentFile(docId: number) {
  return request({
    url: '/ai/document/' + docId + '/file',
    method: 'get',
    responseType: 'blob'
  })
}

// ==================== 文档处理 ====================
export function processDocument(docId) {
  return request({ url: '/ai/document/process/' + docId, method: 'post' })
}
export function getProcessStatus(docId) {
  return request({ url: '/ai/document/process/' + docId, method: 'get' })
}
export function getDocumentContent(docId) {
  return request({ url: '/ai/document/' + docId + '/content', method: 'get' })
}
export function getDocStats(kbId) {
  return request({ url: '/ai/document/stats/' + kbId, method: 'get' })
}

// ==================== 检索 ====================
export function searchKnowledge(data) {
  return request({ url: '/ai/kb/search', method: 'post', data })
}
