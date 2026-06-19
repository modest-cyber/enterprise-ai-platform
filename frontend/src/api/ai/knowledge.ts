import request from '@/utils/request'

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

export function uploadDocument(data) {
  return request({
    url: '/ai/kb/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function searchKnowledge(data) {
  return request({ url: '/ai/kb/search', method: 'post', data })
}

export function listDocuments(id) {
  return request({ url: '/ai/kb/' + id + '/doc/list', method: 'get' })
}

export function deleteDocument(docId) {
  return request({ url: '/ai/kb/doc/' + docId, method: 'delete' })
}