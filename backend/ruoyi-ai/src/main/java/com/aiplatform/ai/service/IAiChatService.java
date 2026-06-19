package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;

/**
 * AI 聊天服务接口 — 统一管理会话 CRUD 与消息发送
 *
 * @author aiplatform
 */
public interface IAiChatService {

    /** 分页查询会话列表 */
    List<AiConversation> selectConversationList(AiConversation conversation);

    /** 根据ID查询会话 */
    AiConversation selectConversationById(Long id);

    /** 新增会话，返回会话ID */
    Long createConversation(AiConversation conversation);

    /** 修改会话 */
    int updateConversation(AiConversation conversation);

    /** 批量删除会话及其消息 */
    int deleteConversation(Long[] ids);

    /** 根据会话ID查询消息列表 */
    List<AiMessage> listMessages(Long conversationId);

    /** 发送消息并获取AI回复 */
    ChatResponseDto chat(ChatRequestDto dto);
}