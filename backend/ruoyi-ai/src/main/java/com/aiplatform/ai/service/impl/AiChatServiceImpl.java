package com.aiplatform.ai.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.aiplatform.ai.client.ChatAgentClient;
import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.domain.AiTool;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;
import com.aiplatform.ai.domain.dto.ConversationConfigDto;
import com.aiplatform.ai.domain.dto.MessageSaveRequestDto;
import java.util.stream.Collectors;
import com.aiplatform.ai.mapper.AiConversationMapper;
import com.aiplatform.ai.mapper.AiMessageMapper;
import com.aiplatform.ai.mapper.AiToolMapper;
import com.aiplatform.ai.service.IAgentService;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.ai.service.IModelConfigService;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.DateUtils;
import com.aiplatform.common.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private AiToolMapper aiToolMapper;

    @Resource
    private ChatAgentClient chatAgentClient;

    @Resource
    private IAgentService agentService;

    @Resource
    private IModelConfigService modelConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public ConversationConfigDto getConversationConfig(Long conversationId) {
        AiConversation conversation = conversationMapper.selectConversationById(conversationId);
        if (conversation == null) {
            throw new ServiceException("会话不存在");
        }

        ConversationConfigDto dto = new ConversationConfigDto();
        dto.setConversationId(conversation.getConversationId());
        dto.setTitle(conversation.getTitle());
        dto.setUserId(conversation.getUserId());

        // Agent 配置 — agentId 是聊天主入口
        AgentConfig agent = null;
        Long resolvedModelId = conversation.getModelId();

        if (conversation.getAgentId() != null) {
            agent = agentService.selectAgentById(conversation.getAgentId());
            if (agent != null) {
                ConversationConfigDto.AgentInfo agentInfo = new ConversationConfigDto.AgentInfo();
                agentInfo.setAgentId(agent.getAgentId());
                agentInfo.setAgentName(agent.getAgentName());
                agentInfo.setAgentType(agent.getAgentType());
                agentInfo.setSystemPrompt(agent.getSystemPrompt());
                agentInfo.setMaxIterations(agent.getMaxIterations());
                agentInfo.setTemperature(agent.getTemperature());
                agentInfo.setTimeoutSeconds(agent.getTimeoutSeconds());

                // ★ Agent 绑定的模型 ID — 优先使用此模型
                agentInfo.setModelId(agent.getModelId());
                if (agent.getModelId() != null) {
                    resolvedModelId = agent.getModelId();
                }

                // ★ 加载 Agent 绑定的工具定义
                if (agent.getToolsJson() != null && !agent.getToolsJson().isBlank()) {
                    try {
                        List<Long> toolIds = objectMapper.readValue(
                                agent.getToolsJson(), new TypeReference<List<Long>>() {});
                        if (toolIds != null && !toolIds.isEmpty()) {
                            List<ConversationConfigDto.ToolDefinition> tools = new ArrayList<>();
                            for (Long toolId : toolIds) {
                                AiTool tool = aiToolMapper.selectToolById(toolId);
                                if (tool != null && Integer.valueOf(1).equals(tool.getIsEnabled())) {
                                    ConversationConfigDto.ToolDefinition td =
                                            new ConversationConfigDto.ToolDefinition();
                                    td.setToolId(tool.getToolId());
                                    td.setToolName(tool.getToolName());
                                    td.setDisplayName(tool.getDisplayName());
                                    td.setToolType(tool.getToolType());
                                    td.setDescription(tool.getDescription());
                                    td.setInputSchema(tool.getInputSchema());
                                    tools.add(td);
                                }
                            }
                            agentInfo.setTools(tools);
                        }
                    } catch (Exception e) {
                        log.warn("解析 Agent {} 的工具列表失败: {}", agent.getAgentId(), e.getMessage());
                    }
                }

                dto.setAgent(agentInfo);
            }
        }

        // Model 配置 — 优先使用 Agent 绑定的模型
        if (resolvedModelId != null) {
            AiModel model = modelConfigService.selectModelById(resolvedModelId);
            if (model != null) {
                ConversationConfigDto.ModelInfo modelInfo = new ConversationConfigDto.ModelInfo();
                modelInfo.setModelId(model.getModelId());
                modelInfo.setModelName(model.getModelName());
                modelInfo.setDisplayName(model.getDisplayName());
                modelInfo.setProvider(model.getProvider());
                modelInfo.setApiKey(model.getApiKey());
                modelInfo.setBaseUrl(model.getBaseUrl());
                modelInfo.setModelType(model.getModelType());
                modelInfo.setMaxTokens(model.getMaxTokens());
                modelInfo.setTemperature(model.getTemperature());
                dto.setModel(modelInfo);
            }
        }

        // 历史消息
        List<AiMessage> messages = messageMapper.selectMessagesByConversationId(conversationId);
        dto.setHistory(messages.stream().map(m -> {
            ConversationConfigDto.HistoryMessage hm = new ConversationConfigDto.HistoryMessage();
            hm.setRole(m.getRole());
            hm.setContent(m.getContent());
            return hm;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public ConversationConfigDto createConversationFromInternal(Long userId, String title,
                                                                 Long agentId, Long modelId) {
        Date now = DateUtils.getNowDate();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新会话");
        conversation.setAgentId(agentId);
        conversation.setModelId(modelId);
        conversation.setStatus(1);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insertConversation(conversation);

        return getConversationConfig(conversation.getConversationId());
    }

    @Override
    public void saveMessage(MessageSaveRequestDto dto) {
        Date now = DateUtils.getNowDate();

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(dto.getConversationId());
        userMessage.setRole("user");
        userMessage.setContent(dto.getUserMessage());
        userMessage.setSourceType("llm");
        userMessage.setCreateTime(now);
        messageMapper.insertMessage(userMessage);

        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(dto.getConversationId());
        aiMessage.setRole("assistant");
        aiMessage.setContent(dto.getAiMessage());
        aiMessage.setSourceType("llm");
        if (dto.getTokenUsage() != null) {
            aiMessage.setTokenCount(dto.getTokenUsage().getTotalTokens());
        }
        aiMessage.setCreateTime(DateUtils.getNowDate());
        messageMapper.insertMessage(aiMessage);

        AiConversation updateConv = new AiConversation();
        updateConv.setConversationId(dto.getConversationId());
        updateConv.setUpdateTime(DateUtils.getNowDate());
        conversationMapper.updateConversation(updateConv);
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