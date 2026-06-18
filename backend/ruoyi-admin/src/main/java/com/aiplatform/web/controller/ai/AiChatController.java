package com.aiplatform.web.controller.ai;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aiplatform.ai.dto.ChatRequestDto;
import com.aiplatform.ai.dto.ChatResponseDto;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.enums.BusinessType;

/**
 * AI 聊天服务
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController {

    @Autowired
    private IAiChatService chatService;

    /**
     * 发送消息
     */
    @PreAuthorize("@ss.hasPermi('ai:chat:send')")
    @Log(title = "AI聊天", businessType = BusinessType.OTHER)
    @PostMapping
    public AjaxResult send(@Valid @RequestBody ChatRequestDto dto) {
        ChatResponseDto response = chatService.chat(dto);
        return AjaxResult.success(response);
    }
}