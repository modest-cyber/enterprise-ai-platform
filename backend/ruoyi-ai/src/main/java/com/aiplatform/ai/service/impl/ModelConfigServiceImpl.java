package com.aiplatform.ai.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.service.IModelConfigService;
import com.aiplatform.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模型配置服务实现 —— 管理LLM模型配置CRUD
 * <p>
 * 当前为骨架实现（Mapper 未注入），P2 阶段完善后对接 AiModelMapper。
 * testConnection() 在 P4 阶段对接 FastApiClient 实现真实连通性测试。
 * setDefaultModel() 需确保同时只有一个 is_default=1 的模型。
 *
 * @author aiplatform
 */
@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    /**
     * 根据ID查询模型配置
     */
    @Override
    public AiModel selectModelById(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        return null;
    }

    /**
     * 分页查询模型列表
     */
    @Override
    public List<AiModel> selectModelList(AiModel model) {
        return new ArrayList<>();
    }

    /**
     * 查询所有已启用的模型
     */
    @Override
    public List<AiModel> selectEnabledModels() {
        return new ArrayList<>();
    }

    /**
     * 查询默认模型（is_default=1）
     */
    @Override
    public AiModel selectDefaultModel() {
        return null;
    }

    /**
     * 新增模型配置
     */
    @Override
    public int insertModel(AiModel model) {
        if (model == null || !StringUtils.hasText(model.getModelName())) {
            throw new ServiceException("模型名称不能为空");
        }
        if (!StringUtils.hasText(model.getApiKey())) {
            throw new ServiceException("API Key不能为空");
        }
        return 0;
    }

    /**
     * 修改模型配置
     */
    @Override
    public int updateModel(AiModel model) {
        if (model == null || model.getModelId() == null) {
            throw new ServiceException("模型ID不能为空");
        }
        return 0;
    }

    /**
     * 批量删除模型配置（物理删除）
     */
    @Override
    public int deleteModelByIds(Long[] modelIds) {
        if (modelIds == null || modelIds.length == 0) {
            throw new ServiceException("待删除的模型ID列表不能为空");
        }
        return 0;
    }

    /**
     * 测试模型连通性（发送 ping 请求到 LLM API）
     * <p>
     * 当前骨架返回 false，P4 阶段实现真实的 HTTP ping 测试。
     */
    @Override
    public boolean testConnection(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        return false;
    }

    /**
     * 设置默认模型（将其他模型 is_default 置为0，将目标模型置为1）
     */
    @Override
    public int setDefaultModel(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        return 0;
    }
}