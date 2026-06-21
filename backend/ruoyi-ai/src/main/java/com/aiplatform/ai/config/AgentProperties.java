package com.aiplatform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.agent")
public class AgentProperties {

    private String baseUrl = "http://localhost:8000";

    private String apiPrefix = "/api";

    private int connectTimeout = 30;

    private int readTimeout = 30;

}