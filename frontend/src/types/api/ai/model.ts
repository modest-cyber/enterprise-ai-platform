import type { BaseEntity, PageDomain } from '../common'

/** 大模型配置实体 */
export interface AiModel extends BaseEntity {
  modelId: number
  modelName: string
  displayName: string
  provider: string
  apiKey: string
  baseUrl: string
  modelType: string
  maxTokens: number
  temperature: number
  isEnabled: number
}

/** 大模型创建/更新 DTO */
export interface ModelConfigDto {
  modelId?: number
  modelName: string
  displayName: string
  provider: string
  apiKey: string
  baseUrl: string
  modelType: string
  maxTokens: number
  temperature: number
  isEnabled: number
  remark?: string
}

/** 大模型查询参数 */
export interface ModelQueryParams extends PageDomain {
  modelName?: string
  displayName?: string
  provider?: string
  modelType?: string
  isEnabled?: number
}
