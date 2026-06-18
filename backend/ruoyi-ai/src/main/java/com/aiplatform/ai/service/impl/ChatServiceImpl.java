package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.service.IChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.openai.chat.model:gpt-4o-mini}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String chat(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "ChatClient 未配置，请在 application.yml 中设置 ai.openai.api-key";
        }
        try {
            String body = buildChatBody(prompt, null);
            return callOpenAI(body);
        } catch (Exception e) {
            log.error("对话调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    @Override
    public String chatWithHistory(String prompt, List<Map<String, String>> history) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "ChatClient 未配置";
        }
        try {
            String body = buildChatBody(prompt, history);
            return callOpenAI(body);
        } catch (Exception e) {
            log.error("多轮对话调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    @Override
    public String chatWithTemplate(String templateName, Map<String, Object> variables) {
        String template = loadTemplate(templateName);
        String prompt = renderTemplate(template, variables);
        return chat(prompt);
    }

    private String buildChatBody(String prompt, List<Map<String, String>> history) {
        StringBuilder messages = new StringBuilder("[");
        if (history != null) {
            for (Map<String, String> msg : history) {
                messages.append("{\"role\":\"").append(escapeJson(msg.get("role")))
                        .append("\",\"content\":\"").append(escapeJson(msg.get("content"))).append("\"},");
            }
        }
        messages.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}");
        messages.append("]");

        return "{\"model\":\"" + escapeJson(model) + "\",\"messages\":" + messages + "}";
    }

    private String callOpenAI(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String loadTemplate(String templateName) {
        return "你是一个AI助手，请回答以下问题：{question}";
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        if (variables == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
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