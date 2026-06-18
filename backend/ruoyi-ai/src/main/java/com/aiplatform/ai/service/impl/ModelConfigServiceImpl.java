package com.aiplatform.ai.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.mapper.AiModelMapper;
import com.aiplatform.ai.service.IModelConfigService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 模型配置服务实现 —— 管理 LLM 模型配置 CRUD
 * <p>
 * 支持多 Provider（OpenAI/DeepSeek/Qwen/Ollama），多模型类型（chat/embedding/rerank）。
 * 通过 is_default 字段标识默认模型，系统同时只有一个默认模型。
 * ApiKey 以加密形式存储，testConnection() 用于验证模型连通性。
 *
 * @author aiplatform
 */
@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigServiceImpl.class);

    @Autowired
    private AiModelMapper aiModelMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // ==================== 查询 ====================

    @Override
    public AiModel selectModelById(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        return aiModelMapper.selectModelById(modelId);
    }

    @Override
    public List<AiModel> selectModelList(AiModel model) {
        return aiModelMapper.selectModelList(model);
    }

    @Override
    public List<AiModel> selectEnabledModels() {
        return aiModelMapper.selectEnabledModels();
    }

    @Override
    public AiModel selectDefaultModel() {
        return aiModelMapper.selectDefaultModel();
    }

    // ==================== CRUD ====================

    @Override
    public int insertModel(AiModel model) {
        if (model == null || !StringUtils.hasText(model.getModelName())) {
            throw new ServiceException("模型名称不能为空");
        }
        if (!StringUtils.hasText(model.getApiKey())) {
            throw new ServiceException("API Key不能为空");
        }
        model.setCreateBy(SecurityUtils.getUsername());
        return aiModelMapper.insertModel(model);
    }

    @Override
    public int updateModel(AiModel model) {
        if (model == null || model.getModelId() == null) {
            throw new ServiceException("模型ID不能为空");
        }
        model.setUpdateBy(SecurityUtils.getUsername());
        return aiModelMapper.updateModel(model);
    }

    @Override
    public int deleteModelByIds(Long[] modelIds) {
        if (modelIds == null || modelIds.length == 0) {
            throw new ServiceException("待删除的模型ID列表不能为空");
        }
        return aiModelMapper.deleteModelByIds(modelIds);
    }

    // ==================== 业务方法 ====================

    @Override
    public boolean testConnection(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        AiModel model = aiModelMapper.selectModelById(modelId);
        if (model == null) {
            throw new ServiceException("模型不存在, modelId=" + modelId);
        }
        log.info("测试模型连通性, modelId={}, modelName={}, provider={}", modelId, model.getModelName(), model.getProvider());

        try {
            // 构建 ping 请求体
            String body = "{\"model\":\"" + escapeJson(model.getModelName()) + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],"
                    + "\"max_tokens\":1}";

            String url = model.getBaseUrl();
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + model.getApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() == 200;
            log.info("模型连通性测试结果, modelId={}, status={}, success={}", modelId, response.statusCode(), success);
            return success;
        } catch (IOException | InterruptedException e) {
            log.error("模型连通性测试失败, modelId={}", modelId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setDefaultModel(Long modelId) {
        if (modelId == null) {
            throw new ServiceException("模型ID不能为空");
        }
        AiModel model = aiModelMapper.selectModelById(modelId);
        if (model == null) {
            throw new ServiceException("模型不存在, modelId=" + modelId);
        }
        log.info("设置默认模型, modelId={}, modelName={}", modelId, model.getModelName());

        // 1. 将所有模型的 is_default 置为 0
        aiModelMapper.setDefaultOff();

        // 2. 将目标模型的 is_default 置为 1
        model.setIsDefault(1);
        model.setUpdateBy(SecurityUtils.getUsername());
        int rows = aiModelMapper.updateModel(model);

        log.info("默认模型设置完成, modelId={}, rows={}", modelId, rows);
        return rows;
    }

    /**
     * JSON 字符串转义（防止内容中的特殊字符破坏 JSON 结构）
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
