package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.service.IModelConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    @Override
    public AiModel selectModelById(Long modelId) {
        return null;
    }

    @Override
    public List<AiModel> selectModelList(AiModel model) {
        return new ArrayList<>();
    }

    @Override
    public List<AiModel> selectEnabledModels() {
        return new ArrayList<>();
    }

    @Override
    public AiModel selectDefaultModel() {
        return null;
    }

    @Override
    public int insertModel(AiModel model) {
        return 0;
    }

    @Override
    public int updateModel(AiModel model) {
        return 0;
    }

    @Override
    public int deleteModelByIds(Long[] modelIds) {
        return 0;
    }

    @Override
    public boolean testConnection(Long modelId) {
        return false;
    }

    @Override
    public int setDefaultModel(Long modelId) {
        return 0;
    }
}