package com.aiplatform.ai.service;

import com.aiplatform.ai.dto.ChatRequestDto;
import com.aiplatform.ai.dto.ChatResponseDto;

/**
 * AI 聊天服务接口
 *
 * @author aiplatform
 */
public interface IAiChatService {

    /**
     * 发送消息并获取AI回复
     *
     * @param dto 聊天请求
     * @return 聊天响应
     */
    ChatResponseDto chat(ChatRequestDto dto);
}