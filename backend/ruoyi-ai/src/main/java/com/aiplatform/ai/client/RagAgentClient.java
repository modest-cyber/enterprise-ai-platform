package com.aiplatform.ai.client;

import java.util.HashMap;
import java.util.List;
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
public class RagAgentClient {

    @Resource
    private FastApiClient fastApiClient;
    @Resource
    private AgentProperties agentProperties;
    @Resource
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagAgentClient(FastApiClient fastApiClient, AgentProperties agentProperties) {
        this.fastApiClient = fastApiClient;
        this.agentProperties = agentProperties;
    }

    @Log(title = "AI调用", businessType = BusinessType.OTHER)
    public AjaxResult rag(String query, List<Map<String, Object>> documents) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("documents", documents);

        String rawJson = fastApiClient.post("/rag", body);
        return convertToAjaxResult(rawJson);
    }

    @Log(title = "AI调用", businessType = BusinessType.OTHER)
    public AjaxResult embed(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);

        String rawJson = fastApiClient.post("/embed", body);
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