package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiModel;

/**
 * 模型配置服务 —— 管理LLM模型配置信息
 * <p>
 * 支持多Provider（OpenAI/DeepSeek/Qwen/Ollama），多模型类型（chat/embedding/rerank）。
 * 通过 is_default 字段标识默认模型，系统同时只有一个默认模型。
 * ApiKey 以加密形式存储，testConnection() 用于验证模型连通性。
 *
 * @author aiplatform
 */
public interface IModelConfigService {

    /**
     * 根据ID查询模型配置
     */
    AiModel selectModelById(Long modelId);

    /**
     * 分页查询模型列表（配合 PageHelper.startPage()）
     */
    List<AiModel> selectModelList(AiModel model);

    /**
     * 查询所有已启用的模型（用于下拉选择）
     */
    List<AiModel> selectEnabledModels();

    /**
     * 查询默认模型（is_default=1）
     */
    AiModel selectDefaultModel();

    /**
     * 新增模型配置
     */
    int insertModel(AiModel model);

    /**
     * 修改模型配置
     */
    int updateModel(AiModel model);

    /**
     * 批量删除模型配置（物理删除）
     */
    int deleteModelByIds(Long[] modelIds);

    /**
     * 测试模型连通性（发送 ping 请求到 LLM API）
     *
     * @param modelId 模型ID
     * @return true-连接成功，false-连接失败
     */
    boolean testConnection(Long modelId);

    /**
     * 设置默认模型（将其他模型 is_default 置为0，将目标模型置为1）
     *
     * @param modelId 目标模型ID
     * @return 影响行数
     */
    int setDefaultModel(Long modelId);
}