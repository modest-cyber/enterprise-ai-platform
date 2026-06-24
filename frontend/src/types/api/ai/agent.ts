import type { BaseEntity, PageDomain } from '../common'

/** Agent 配置实体 */
export interface AgentConfig extends BaseEntity {
  agentId: number
  agentName: string
  agentType: string
  description: string
  systemPrompt: string
  toolsJson: string
  workflowJson: string
  maxIterations: number
  temperature: number
  timeoutSeconds: number
  status: number
}

/** Agent 执行入参 DTO */
export interface AgentExecuteDto {
  agentId: number
  task: string
  input?: string
  async?: boolean
}

/** Agent 查询参数 */
export interface AgentQueryParams extends PageDomain {
  agentName?: string
  agentType?: string
  status?: number
}
