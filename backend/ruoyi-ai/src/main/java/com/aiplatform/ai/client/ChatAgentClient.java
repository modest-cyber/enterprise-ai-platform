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
    FastApiClient fastApiClient;
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
        return convertToAjaxResult(rawJson);
    }

    private AjaxResult convertToAjaxResult(String rawJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            Object code = map.get("code");
            Object msg = map.get("msg");
            Object data = map.get("data");

            if (code != null && (int) code == 200) {
                return AjaxResult.success(msg != null ? msg.toString() : "操作成功", data);
            }
            return AjaxResult.error(msg != null ? msg.toString() : "Agent 返回异常");
        } catch (Exception e) {
            log.warn("Agent 返回 JSON 解析失败，按原始文本返回: {}", e.getMessage());
            return AjaxResult.success(rawJson);
        }
    }

}