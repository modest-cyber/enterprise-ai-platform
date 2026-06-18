package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiTool;

/**
 * MCP 工具管理服务接口
 * <p>
 * 负责 MCP 工具的注册、配置管理和调用执行。
 * 工具注册后可由 Agent 在执行任务时自动发现并调用。
 * 当前阶段 invokeTool 为 Mock 实现，M7 阶段对接 McpToolExecutor。
 *
 * @author aiplatform
 */
public interface IToolService {

    // ==================== 查询 ====================

    /**
     * 根据 ID 查询工具
     */
    AiTool selectToolById(Long toolId);

    /**
     * 根据名称查询工具
     */
    AiTool selectToolByName(String toolName);

    /**
     * 分页查询工具列表（配合 PageHelper.startPage()）
     */
    List<AiTool> selectToolList(AiTool tool);

    /**
     * 查询所有已启用的工具（用于 Agent 工具选择）
     */
    List<AiTool> selectEnabledTools();

    // ==================== CRUD ====================

    /**
     * 新增工具
     */
    int insertTool(AiTool tool);

    /**
     * 修改工具
     */
    int updateTool(AiTool tool);

    /**
     * 批量删除工具
     */
    int deleteToolByIds(Long[] toolIds);

    // ==================== 执行 ====================

    /**
     * 调用工具执行
     * <p>
     * 当前阶段为 Mock 实现，M7 完成后对接 McpToolExecutor.invoke()。
     *
     * @param toolId 工具ID
     * @param params 输入参数（JSON 字符串）
     * @return 工具执行结果（JSON）
     */
    String invokeTool(Long toolId, String params);

    /**
     * 测试工具连通性
     *
     * @param toolId 工具ID
     * @return true-连通，false-不通
     */
    boolean testConnection(Long toolId);
}
