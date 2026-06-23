package com.aiplatform.ai.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.aiplatform.ai.client.ChatAgentClient;
import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;
import com.aiplatform.ai.mapper.AiConversationMapper;
import com.aiplatform.ai.mapper.AiMessageMapper;
import com.aiplatform.ai.service.IAgentService;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.ai.service.IModelConfigService;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.DateUtils;
import com.aiplatform.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private AiConversationMapper conversationMapper;

    @Resource
    private AiMessageMapper messageMapper;

    @Resource
    private ChatAgentClient chatAgentClient;

    @Resource
    private IAgentService agentService;

    @Resource
    private IModelConfigService modelConfigService;

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

    @Override
    public void renameConversation(Long id, String title) {
        AiConversation conv = new AiConversation();
        conv.setConversationId(id);
        conv.setTitle(title);
        conv.setUpdateTime(DateUtils.getNowDate());
        conversationMapper.updateConversation(conv);
    }

    @Override
    public String generateTitle(Long id) {
        AiConversation conv = conversationMapper.selectConversationById(id);
        if (conv == null) {
            throw new ServiceException("会话不存在");
        }
        List<AiMessage> messages = messageMapper.selectMessagesByConversationId(id);
        String firstUserMsg = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .findFirst()
                .map(AiMessage::getContent)
                .orElse(conv.getTitle());

        String title = generateTitleFromMessage(firstUserMsg);

        AiConversation update = new AiConversation();
        update.setConversationId(id);
        update.setTitle(title);
        update.setUpdateTime(DateUtils.getNowDate());
        conversationMapper.updateConversation(update);

        return title;
    }

    private String generateTitleFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "新会话";
        }
        String cleaned = message.replaceAll("[\\n\\r]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 20) {
            return cleaned;
        }
        int cut = Math.min(cleaned.length(), 20);
        while (cut > 10 && cleaned.charAt(cut) != ' ' && !isCjkBoundary(cleaned, cut)) {
            cut--;
        }
        if (cut <= 10) {
            String title = cleaned.substring(0, 18).trim();
            return title.length() < cleaned.length() ? title : cleaned.substring(0, 20).trim();
        }
        return cleaned.substring(0, cut).trim();
    }

    private boolean isCjkBoundary(String s, int idx) {
        if (idx <= 0 || idx >= s.length()) return false;
        char c = s.charAt(idx - 1);
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    // ==================== 消息发送 — 通过 Python Agent 调用真实 LLM ====================

    @Override
    public ChatResponseDto chat(ChatRequestDto dto) {
        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        Long conversationId;
        String title;
        Long resolvedAgentId;
        Long resolvedModelId;

        if (dto.getConversationId() != null) {
            AiConversation conversation = conversationMapper.selectConversationById(dto.getConversationId());
            if (conversation == null) {
                throw new ServiceException("会话不存在");
            }
            conversationId = dto.getConversationId();
            title = conversation.getTitle();
            resolvedAgentId = conversation.getAgentId();
            resolvedModelId = conversation.getModelId();
        } else {
            title = generateTitleFromMessage(dto.getMessage());
            resolvedAgentId = dto.getAgentId();
            resolvedModelId = dto.getModelId();

            AiConversation conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setTitle(title);
            conversation.setAgentId(resolvedAgentId);
            conversation.setModelId(resolvedModelId);
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

        String reply;
        try {
            reply = callPythonAgent(dto.getMessage(), String.valueOf(conversationId),
                    resolvedAgentId, resolvedModelId);
        } catch (Exception e) {
            log.error("Python Agent 调用失败, 回退 mock: {}", e.getMessage());
            reply = "这是模拟回复：" + dto.getMessage();
        }

        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(reply);
        aiMessage.setCreateTime(DateUtils.getNowDate());
        messageMapper.insertMessage(aiMessage);

        AiConversation updateConv = new AiConversation();
        updateConv.setConversationId(conversationId);
        updateConv.setUpdateTime(DateUtils.getNowDate());
        conversationMapper.updateConversation(updateConv);

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(conversationId);
        response.setTitle(title);
        response.setMessageId(aiMessage.getMessageId());
        response.setContent(reply);
        response.setRole("assistant");

        return response;
    }

    private String callPythonAgent(String message, String conversationId,
                                    Long agentId, Long modelId) {
        AgentConfig agent = agentId != null ? agentService.selectAgentById(agentId) : null;
        AiModel model = modelId != null ? modelConfigService.selectModelById(modelId) : null;

        log.info("调用 Python Agent: agentId={}, modelId={}", agentId, modelId);

        AjaxResult result = chatAgentClient.chatWithContext(
                message, conversationId, agent, model);

        Object data = result.get(AjaxResult.DATA_TAG);
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object content = dataMap.get("content");
            if (content != null) {
                return content.toString();
            }
        }
        return result.get(AjaxResult.MSG_TAG) != null
                ? result.get(AjaxResult.MSG_TAG).toString()
                : "AI 回复异常";
    }
}