import type { BaseEntity, PageDomain } from '../common'

/** 工具配置实体 */
export interface AiTool extends BaseEntity {
  toolId: number
  toolName: string
  displayName: string
  toolType: string
  description: string
  serverUrl: string
  inputSchema: string
  outputSchema: string
  authType: string
  authConfig: string
  timeoutMs: number
  retryCount: number
  isEnabled: number
}

/** 工具查询参数 */
export interface ToolQueryParams extends PageDomain {
  toolName?: string
  toolType?: string
  isEnabled?: number
}
