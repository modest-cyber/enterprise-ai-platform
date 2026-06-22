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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型配置服务实现 —— 管理 LLM 模型配置 CRUD
 * <p>
 * 支持多 Provider（OpenAI/DeepSeek/Qwen/Ollama），多模型类型（chat/embedding/rerank）。
 * 基础入参非空校验由 Controller 层 DTO + @Valid 完成，Service 只保留业务规则。
 *
 * @author aiplatform
 */
@Service
@Slf4j
public class ModelConfigServiceImpl implements IModelConfigService {


    @Autowired
    private AiModelMapper aiModelMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // ==================== 查询 ====================

    @Override
    public AiModel selectModelById(Long modelId) {
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

    // ==================== CRUD ====================

    @Override
    public int insertModel(AiModel model) {
        model.setCreateBy(SecurityUtils.getUsername());
        return aiModelMapper.insertModel(model);
    }

    @Override
    public int updateModel(AiModel model) {
        model.setUpdateBy(SecurityUtils.getUsername());
        return aiModelMapper.updateModel(model);
    }

    @Override
    public int deleteModelByIds(Long[] modelIds) {
        return aiModelMapper.deleteModelByIds(modelIds);
    }

    // ==================== 业务方法 ====================

    @Override
    public boolean testConnection(Long modelId) {
        AiModel model = aiModelMapper.selectModelById(modelId);
        if (model == null) {
            throw new ServiceException("模型不存在, modelId=" + modelId);
        }
        log.info("测试模型连通性, modelId={}, modelName={}, provider={}", modelId, model.getModelName(), model.getProvider());

        try {
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
