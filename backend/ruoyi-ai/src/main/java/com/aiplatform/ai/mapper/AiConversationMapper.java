package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI会话 数据层
 *
 * @author aiplatform
 */
@Mapper
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