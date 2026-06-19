package com.aiplatform.ai.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;
import com.aiplatform.ai.mapper.AiConversationMapper;
import com.aiplatform.ai.mapper.AiMessageMapper;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.DateUtils;
import com.aiplatform.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 聊天服务实现 — 统一管理会话 CRUD 与消息发送
 *
 * @author aiplatform
 */
@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {


    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    // ==================== 会话 CRUD ====================

    @Override
    public List<AiConversation> selectConversationList(AiConversation conversation) {
        return conversationMapper.selectConversationList(conversation);
    }

    @Override
    public AiConversation selectConversationById(Long id) {
        return conversationMapper.selectConversationById(id);
    }

    @Override
    public Long createConversation(AiConversation conversation) {
        Date now = DateUtils.getNowDate();
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insertConversation(conversation);
        return conversation.getConversationId();
    }

    @Override
    public int updateConversation(AiConversation conversation) {
        conversation.setUpdateTime(DateUtils.getNowDate());
        return conversationMapper.updateConversation(conversation);
    }

    @Override
    public int deleteConversation(Long[] ids) {
        for (Long conversationId : ids) {
            messageMapper.deleteMessagesByConversationId(conversationId);
        }
        return conversationMapper.deleteConversationByIds(ids);
    }

    @Override
    public List<AiMessage> listMessages(Long conversationId) {
        return messageMapper.selectMessagesByConversationId(conversationId);
    }

    // ==================== 消息发送 ====================

    @Override
    public ChatResponseDto chat(ChatRequestDto dto) {
        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        Long conversationId;

        if (dto.getConversationId() != null) {
            AiConversation conversation = conversationMapper.selectConversationById(dto.getConversationId());
            if (conversation == null) {
                throw new ServiceException("会话不存在");
            }
            conversationId = dto.getConversationId();
        } else {
            AiConversation conversation = new AiConversation();
            conversation.setUserId(userId);
            String title = dto.getMessage();
            if (title.length() > 30) {
                title = title.substring(0, 30);
            }
            conversation.setTitle(title);
            conversation.setAgentType(dto.getAgentType());
            conversation.setStatus(1);
            conversation.setCreateTime(now);
            conversation.setUpdateTime(now);
            conversationMapper.insertConversation(conversation);
            conversationId = conversation.getConversationId();
        }

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(dto.getMessage());
        userMessage.setCreateTime(now);
        messageMapper.insertMessage(userMessage);

        String reply = "这是模拟回复：" + dto.getMessage();

        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(reply);
        aiMessage.setSourceType(dto.getAgentType());
        aiMessage.setCreateTime(DateUtils.getNowDate());
        messageMapper.insertMessage(aiMessage);

        AiConversation updateConv = new AiConversation();
        updateConv.setConversationId(conversationId);
        updateConv.setUpdateTime(DateUtils.getNowDate());
        conversationMapper.updateConversation(updateConv);

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(conversationId);
        response.setMessageId(aiMessage.getMessageId());
        response.setContent(reply);
        response.setRole("assistant");
        response.setAgentType(dto.getAgentType());

        return response;
    }
}