package com.aiplatform.ai.client;

import java.util.HashMap;
import java.util.Map;
import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.common.core.domain.AjaxResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatAgentClient {

    @Resource
    private FastApiClient fastApiClient;
    @Resource
    private ObjectMapper objectMapper = new ObjectMapper();

    public ChatAgentClient(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    public AjaxResult chat(String message, String sessionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("sessionId", sessionId);
        body.put("agent", new HashMap<>());
        body.put("model", new HashMap<>());

        String rawJson = fastApiClient.post("/chat", body);
        return parseChatResult(rawJson);
    }

    public AjaxResult chatWithContext(String message, String conversationId,
                                       AgentConfig agent, AiModel model) {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("message", message);

        Map<String, Object> agentMap = new HashMap<>();
        if (agent != null) {
            agentMap.put("id", agent.getAgentId());
            agentMap.put("name", agent.getAgentName());
            agentMap.put("systemPrompt", agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "");
        }
        body.put("agent", agentMap);

        Map<String, Object> modelMap = new HashMap<>();
        if (model != null) {
            modelMap.put("id", model.getModelId());
            modelMap.put("provider", model.getProvider());
            modelMap.put("modelName", model.getModelName());
            modelMap.put("baseUrl", model.getBaseUrl());
            modelMap.put("apiKey", model.getApiKey() != null ? model.getApiKey() : "");
            modelMap.put("temperature", model.getTemperature() != null ? model.getTemperature() : 0.7);
            modelMap.put("maxTokens", model.getMaxTokens() != null ? model.getMaxTokens() : 4096);
        }
        body.put("model", modelMap);

        String rawJson = fastApiClient.post("/chat", body);
        return parseChatResult(rawJson);
    }

    private AjaxResult parseChatResult(String rawJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            Object success = map.get("success");
            if (Boolean.TRUE.equals(success)) {
                Object content = map.get("content");
                Map<String, Object> data = new HashMap<>();
                data.put("content", content != null ? content : "");
                return AjaxResult.success(data);
            }
            return AjaxResult.error(map.get("message") != null ? map.get("message").toString() : "Agent 返回异常");
        } catch (Exception e) {
            log.warn("Agent 返回 JSON 解析失败，按原始文本返回: {}", e.getMessage());
            return AjaxResult.success(rawJson);
        }
    }

}