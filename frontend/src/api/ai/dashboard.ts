import request from '@/utils/request'

// 系统概览统计
export function getOverview() {
  return request({ url: '/dashboard/overview', method: 'get' })
}

// 最近活动列表
export function getRecent() {
  return request({ url: '/dashboard/recent', method: 'get' })
}

// 近7天会话趋势
export function getTrend() {
  return request({ url: '/dashboard/trend', method: 'get' })
}

// 模型服务状态
export function getModelStatus() {
  return request({ url: '/dashboard/modelStatus', method: 'get' })
}
