package com.aiplatform.ai.client;

import java.util.HashMap;
import java.util.Map;
import com.aiplatform.ai.config.AgentProperties;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.enums.BusinessType;
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
    private AgentProperties agentProperties;
    @Resource
    private ObjectMapper objectMapper = new ObjectMapper();

    public ChatAgentClient(FastApiClient fastApiClient, AgentProperties agentProperties) {
        this.fastApiClient = fastApiClient;
        this.agentProperties = agentProperties;
    }

    @Log(title = "AI调用", businessType = BusinessType.OTHER)
    public AjaxResult chat(String message, String sessionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("sessionId", sessionId);

        String rawJson = fastApiClient.post("/chat", body);
        return convertChatResult(rawJson);
    }

    @Log(title = "AI调用", businessType = BusinessType.OTHER)
    public AjaxResult chatWithModel(String message, String sessionId,
                                     String provider, String modelName,
                                     String baseUrl, String apiKey,
                                     Double temperature, Integer maxTokens) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("sessionId", sessionId);
        body.put("provider", provider != null ? provider : "");
        body.put("modelName", modelName != null ? modelName : "");
        body.put("baseUrl", baseUrl != null ? baseUrl : "");
        body.put("apiKey", apiKey != null ? apiKey : "");
        body.put("temperature", temperature != null ? temperature : 0.7);
        body.put("maxTokens", maxTokens != null ? maxTokens : 4096);

        String rawJson = fastApiClient.post("/chat", body);
        return convertChatResult(rawJson);
    }

    private AjaxResult convertChatResult(String rawJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            Object content = map.get("content");
            if (content != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("content", content);
                data.put("sessionId", map.get("sessionId"));
                data.put("agent", map.get("agent"));
                data.put("tokenUsage", map.get("tokenUsage"));
                return AjaxResult.success(data);
            }
            return AjaxResult.success(rawJson);
        } catch (Exception e) {
            log.warn("Agent 返回 JSON 解析失败，按原始文本返回: {}", e.getMessage());
            return AjaxResult.success(rawJson);
        }
    }

}