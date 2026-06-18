package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiConversation;

/**
 * AI会话 数据层
 *
 * @author aiplatform
 */
public interface AiConversationMapper
{
    AiConversation selectConversation(AiConversation conversation);

    AiConversation selectConversationById(Long conversationId);

    List<AiConversation> selectConversationList(AiConversation conversation);

    int insertConversation(AiConversation conversation);

    int updateConversation(AiConversation conversation);

    int deleteConversationById(Long conversationId);

    int deleteConversationByIds(Long[] conversationIds);
}