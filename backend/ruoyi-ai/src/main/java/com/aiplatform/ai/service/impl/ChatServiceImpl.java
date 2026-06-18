package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.service.IChatService;
import com.aiplatform.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI对话服务实现 —— 基于 HttpClient 直调 OpenAI 兼容 API
 * <p>
 * 设计原则：Spring AI 仅做 Prompt 模板和简单 ChatClient 封装，
 * Agent 编排和 RAG 检索全在 Python Agent 端。
 * 当前实现为 Java 端直调 LLM API（OpenAI 兼容协议），生产环境应通过 FastApiClient 调用 Python Agent。
 *
 * @author aiplatform
 */
@Service
public class ChatServiceImpl implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    /** LLM API Key（从配置文件读取，生产环境通过 ModelConfigService 动态获取） */
    @Value("${ai.openai.api-key:}")
    private String apiKey;

    /** LLM API Base URL */
    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    /** 默认模型名称 */
    @Value("${ai.openai.chat.model:gpt-4o-mini}")
    private String model;

    /** HTTP 客户端（连接超时30秒） */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 同步单轮对话
     * <p>
     * 校验 API Key 配置 → 构建 OpenAI 请求体 → HTTP POST → 返回AI回复文本
     */
    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException("ChatClient 未配置，请在 application.yml 中设置 ai.openai.api-key");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new ServiceException("消息内容不能为空");
        }
        try {
            String body = buildChatBody(prompt, null);
            return callOpenAI(body);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("对话调用失败, prompt={}", prompt, e);
            throw new ServiceException("AI调用失败: " + e.getMessage());
        }
    }

    /**
     * 带历史的多轮对话
     */
    @Override
    public String chatWithHistory(String prompt, List<Map<String, String>> history) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException("ChatClient 未配置");
        }
        if (CollectionUtils.isEmpty(history)) {
            return chat(prompt);
        }
        try {
            String body = buildChatBody(prompt, history);
            return callOpenAI(body);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("多轮对话调用失败, prompt={}", prompt, e);
            throw new ServiceException("AI调用失败: " + e.getMessage());
        }
    }

    /**
     * Prompt模板渲染后调用LLM
     * <p>
     * 从模板库加载模板 → 替换变量占位符 → 调用 LLM
     */
    @Override
    public String chatWithTemplate(String templateName, Map<String, Object> variables) {
        if (!StringUtils.hasText(templateName)) {
            throw new ServiceException("模板名称不能为空");
        }
        String template = loadTemplate(templateName);
        String prompt = renderTemplate(template, variables);
        return chat(prompt);
    }

    /**
     * 构建 OpenAI 兼容 API 请求体（Chat Completions 格式）
     *
     * @param prompt  当前用户消息
     * @param history 历史消息列表，可为 null
     * @return JSON 请求体字符串
     */
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

    /**
     * HTTP POST 调用 OpenAI API
     */
    private String callOpenAI(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("LLM调用完成, status={}", response.statusCode());
        if (response.statusCode() != 200) {
            throw new ServiceException("LLM API返回错误, status=" + response.statusCode());
        }
        return response.body();
    }

    /**
     * 加载Prompt模板
     * <p>
     * 当前为简化实现（返回固定模板），生产环境应从数据库或配置文件加载。
     */
    private String loadTemplate(String templateName) {
        return "你是一个AI助手，请回答以下问题：{question}";
    }

    /**
     * 渲染模板变量（将 {key} 占位符替换为实际值）
     */
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * JSON 字符串转义（防止内容中的特殊字符破坏 JSON 结构）
     */
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