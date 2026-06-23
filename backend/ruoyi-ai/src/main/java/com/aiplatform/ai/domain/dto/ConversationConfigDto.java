package com.aiplatform.ai.domain.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 会话配置响应 DTO — FastAPI 通过内部接口获取 Agent/Model/历史消息
 *
 * @author aiplatform
 */
@Getter
@Setter
public class ConversationConfigDto {

    private Long conversationId;
    private String title;
    private Long userId;
    private AgentInfo agent;
    private ModelInfo model;
    private List<HistoryMessage> history;

    @Getter
    @Setter
    public static class AgentInfo {
        private Long agentId;
        private String agentName;
        private String agentType;
        private String systemPrompt;
        private Integer maxIterations;
        private Double temperature;
        private Integer timeoutSeconds;
    }

    @Getter
    @Setter
    public static class ModelInfo {
        private Long modelId;
        private String modelName;
        private String displayName;
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String modelType;
        private Integer maxTokens;
        private Double temperature;
    }

    @Getter
    @Setter
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
