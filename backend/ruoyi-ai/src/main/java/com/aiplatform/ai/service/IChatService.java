package com.aiplatform.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI对话服务 —— 负责对话编排、消息管理
 * <p>
 * 核心流程：接收用户消息 → 创建/获取会话 → 保存用户消息 → 调用Python Agent → 保存AI回复 → 返回结果。
 * chat() 为同步模式（阻塞等待完整回复），chatStream() 为SSE流式模式（逐块推送）。
 * Spring AI 在此仅做 ChatClient 封装层，Agent 编排和 RAG 检索全在 Python 端。
 *
 * @author aiplatform
 */
public interface IChatService {

    /**
     * 同步对话（阻塞等待 Python Agent 完整回复）
     *
     * @param prompt 用户输入消息
     * @return AI回复文本
     */
    String chat(String prompt);

    /**
     * 带历史的多轮对话
     *
     * @param prompt  当前用户消息
     * @param history 历史消息列表（role/content键值对）
     * @return AI回复文本
     */
    String chatWithHistory(String prompt, List<Map<String, String>> history);

    /**
     * 使用Prompt模板渲染后调用LLM
     *
     * @param templateName 模板名称
     * @param variables    模板变量（key为占位符名，value为替换值）
     * @return AI回复文本
     */
    String chatWithTemplate(String templateName, Map<String, Object> variables);
}