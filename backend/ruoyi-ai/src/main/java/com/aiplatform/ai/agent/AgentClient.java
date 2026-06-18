package com.aiplatform.ai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Python Agent 调用客户端
 * <p>
 * 设计原则：Agent 逻辑全在 Python 端（LangChain/LangGraph），
 * Java 端只负责 HTTP 调用，不做 Agent 编排。
 */
@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    @Value("${ai.agent.base-url:http://localhost:8000}")
    private String agentBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 调用 Python Agent 执行任务（非流式）
     *
     * @param task   任务描述
     * @param input  输入参数 JSON 字符串
     * @return Agent 执行结果
     */
    public String execute(String task, String input) {
        try {
            String body = String.format("{\"task\":\"%s\",\"input\":%s}",
                    escapeJson(task), input != null ? input : "{}");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentBaseUrl + "/agent/execute"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Agent 调用完成, task={}, status={}", task, response.statusCode());
            return response.body();
        } catch (Exception e) {
            log.error("Agent 调用失败, task={}", task, e);
            throw new RuntimeException("Agent 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询 Agent 任务状态
     */
    public String getTaskStatus(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentBaseUrl + "/agent/status/" + taskId))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            log.error("查询 Agent 状态失败, taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 将知识文档推送到 Python Agent 做向量化索引
     */
    public String indexDocument(String docId, String content) {
        try {
            String body = String.format("{\"docId\":\"%s\",\"content\":\"%s\"}",
                    escapeJson(docId), escapeJson(content));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentBaseUrl + "/agent/index"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            log.error("文档索引失败, docId={}", docId, e);
            throw new RuntimeException("文档索引失败: " + e.getMessage(), e);
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