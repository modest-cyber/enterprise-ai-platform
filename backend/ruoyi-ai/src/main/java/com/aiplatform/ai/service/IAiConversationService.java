package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;

/**
 * AI 会话服务接口
 *
 * @author aiplatform
 */
public interface IAiConversationService {

    /**
     * 分页查询会话列表
     */
    List<AiConversation> selectConversationList(AiConversation conversation);

    /**
     * 根据ID查询会话
     */
    AiConversation selectById(Long id);

    /**
     * 新增会话
     */
    int insert(AiConversation conversation);

    /**
     * 修改会话
     */
    int update(AiConversation conversation);

    /**
     * 批量删除会话及其消息
     */
    int deleteByIds(Long[] ids);

    /**
     * 根据会话ID查询消息列表
     */
    List<AiMessage> selectMessagesByConversationId(Long conversationId);
}