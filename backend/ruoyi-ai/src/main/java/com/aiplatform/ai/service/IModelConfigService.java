package com.aiplatform.ai.service;

import com.aiplatform.ai.domain.ModelConfig;

import java.util.List;

/**
 * 模型配置服务 —— 管理 LLM 模型配置信息
 */
public interface IModelConfigService {

    List<ModelConfig> listConfigs();

    ModelConfig getConfigById(Long id);

    int addConfig(ModelConfig config);

    int updateConfig(ModelConfig config);

    int deleteConfigByIds(Long[] ids);

    /**
     * 获取当前启用的默认模型配置
     */
    ModelConfig getDefaultConfig();
}