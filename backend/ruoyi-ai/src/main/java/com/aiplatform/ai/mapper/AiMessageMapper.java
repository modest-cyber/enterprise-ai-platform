package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiMessage;

/**
 * AI消息 数据层
 *
 * @author aiplatform
 */
public interface AiMessageMapper
{
    AiMessage selectMessage(AiMessage message);

    AiMessage selectMessageById(Long messageId);

    List<AiMessage> selectMessageList(AiMessage message);

    List<AiMessage> selectMessagesByConversationId(Long conversationId);

    int insertMessage(AiMessage message);

    int updateMessage(AiMessage message);

    int deleteMessageById(Long messageId);

    int deleteMessageByIds(Long[] messageIds);

    int deleteMessagesByConversationId(Long conversationId);
}