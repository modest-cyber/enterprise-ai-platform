package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AgentConfig;

/**
 * Agent 管理服务接口
 * <p>
 * 负责 Agent 配置的 CRUD 管理以及 Agent 任务的同步/异步执行。
 * Agent 是 AI 任务的执行单元，定义了 System Prompt 模板、绑定的模型和工具集。
 *
 * @author aiplatform
 */
public interface IAgentService {

    // ==================== 查询 ====================

    /**
     * 根据 ID 查询 Agent
     */
    AgentConfig selectAgentById(Long agentId);

    /**
     * 根据名称查询 Agent
     */
    AgentConfig selectAgentByName(String agentName);

    /**
     * 分页查询 Agent 列表（配合 PageHelper.startPage()）
     */
    List<AgentConfig> selectAgentList(AgentConfig agent);

    // ==================== CRUD ====================

    /**
     * 新增 Agent 配置
     */
    int insertAgent(AgentConfig agent);

    /**
     * 修改 Agent 配置
     */
    int updateAgent(AgentConfig agent);

    /**
     * 批量删除 Agent 配置
     */
    int deleteAgentByIds(Long[] agentIds);

    // ==================== 执行 ====================

    /**
     * 同步执行 Agent 任务
     *
     * @param agentId Agent ID
     * @param task    任务描述（自然语言）
     * @param input   输入参数（JSON 字符串，可选）
     * @return Agent 执行结果
     */
    String executeSync(Long agentId, String task, String input);

    /**
     * 异步提交 Agent 任务
     *
     * @param agentId Agent ID
     * @param task    任务描述（自然语言）
     * @param input   输入参数（JSON 字符串，可选）
     * @return 任务ID，用于后续查询状态
     */
    String submitTask(Long agentId, String task, String input);

    /**
     * 查询异步任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息（JSON）
     */
    String getTaskStatus(String taskId);

    /**
     * 取消进行中的异步任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelTask(String taskId);
}
