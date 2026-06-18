package com.aiplatform.ai.service.impl;

import java.util.Date;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.dto.ChatRequestDto;
import com.aiplatform.ai.dto.ChatResponseDto;
import com.aiplatform.ai.mapper.AiConversationMapper;
import com.aiplatform.ai.mapper.AiMessageMapper;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.DateUtils;
import com.aiplatform.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 聊天服务实现
 *
 * @author aiplatform
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiMessageMapper messageMapper;

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