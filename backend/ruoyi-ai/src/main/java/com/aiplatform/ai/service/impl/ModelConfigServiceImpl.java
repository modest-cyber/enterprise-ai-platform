package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.domain.ModelConfig;
import com.aiplatform.ai.service.IModelConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    @Override
    public List<ModelConfig> listConfigs() {
        return new ArrayList<>();
    }

    @Override
    public ModelConfig getConfigById(Long id) {
        return null;
    }

    @Override
    public int addConfig(ModelConfig config) {
        return 0;
    }

    @Override
    public int updateConfig(ModelConfig config) {
        return 0;
    }

    @Override
    public int deleteConfigByIds(Long[] ids) {
        return 0;
    }

    @Override
    public ModelConfig getDefaultConfig() {
        return null;
    }
}