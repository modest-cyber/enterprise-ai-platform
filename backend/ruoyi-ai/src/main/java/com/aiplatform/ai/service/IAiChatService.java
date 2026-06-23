package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;
import com.aiplatform.ai.domain.dto.ConversationConfigDto;
import com.aiplatform.ai.domain.dto.MessageSaveRequestDto;

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

    /** 重命名会话 */
    void renameConversation(Long id, String title);

    /** 重新生成会话标题 */
    String generateTitle(Long id);

    /**
     * 获取会话配置（供 FastAPI 内部接口调用）
     * 返回会话信息、Agent 配置、Model 配置、历史消息
     */
    ConversationConfigDto getConversationConfig(Long conversationId);

    /**
     * 创建会话并返回配置（供 FastAPI 内部接口调用）
     * 用于新会话场景：FastAPI 收到 conversationId=null 时，先创建会话再聊天
     */
    ConversationConfigDto createConversationFromInternal(Long userId, String title,
                                                          Long agentId, Long modelId);

    /**
     * 保存消息（供 FastAPI 内部接口调用）
     * 同时保存用户消息和 AI 回复
     */
    void saveMessage(MessageSaveRequestDto dto);
}