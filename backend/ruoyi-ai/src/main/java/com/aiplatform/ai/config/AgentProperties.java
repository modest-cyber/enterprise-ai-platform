package com.aiplatform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.agent")
public class AgentProperties {

    private String baseUrl;

    private String apiPrefix;

    private int connectTimeout;

    private int readTimeout;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

}