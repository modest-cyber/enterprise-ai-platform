package com.aiplatform.ai.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.mapper.AiConversationMapper;
import com.aiplatform.ai.mapper.AiMessageMapper;
import com.aiplatform.ai.service.IAiConversationService;
import com.aiplatform.common.utils.DateUtils;

/**
 * AI 会话服务实现
 *
 * @author aiplatform
 */
@Service
public class AiConversationServiceImpl implements IAiConversationService {

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    @Override
    public List<AiConversation> selectConversationList(AiConversation conversation) {
        return conversationMapper.selectConversationList(conversation);
    }

    @Override
    public AiConversation selectById(Long id) {
        return conversationMapper.selectConversationById(id);
    }

    @Override
    public int insert(AiConversation conversation) {
        Date now = DateUtils.getNowDate();
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        return conversationMapper.insertConversation(conversation);
    }

    @Override
    public int update(AiConversation conversation) {
        conversation.setUpdateTime(DateUtils.getNowDate());
        return conversationMapper.updateConversation(conversation);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        for (Long conversationId : ids) {
            messageMapper.deleteMessagesByConversationId(conversationId);
        }
        return conversationMapper.deleteConversationByIds(ids);
    }

    @Override
    public List<AiMessage> selectMessagesByConversationId(Long conversationId) {
        return messageMapper.selectMessagesByConversationId(conversationId);
    }
}