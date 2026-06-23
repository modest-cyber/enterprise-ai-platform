package com.aiplatform.web.controller.ai;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aiplatform.ai.domain.AiConversation;
import com.aiplatform.ai.domain.AiMessage;
import com.aiplatform.ai.domain.dto.ChatRequestDto;
import com.aiplatform.ai.domain.dto.ChatResponseDto;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.page.TableDataInfo;
import com.aiplatform.common.enums.BusinessType;
import com.aiplatform.common.utils.SecurityUtils;
import jakarta.validation.Valid;

/**
 * AI 聊天 — 统一管理会话 CRUD 与消息发送
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController {

    @Autowired
    private IAiChatService chatService;

    // ==================== 会话管理 ====================

    @PreAuthorize("@ss.hasPermi('ai:chat:list')")
    @GetMapping("/conversations")
    public TableDataInfo listConversations(AiConversation conversation) {
        startPage();
        List<AiConversation> list = chatService.selectConversationList(conversation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:query')")
    @GetMapping("/conversations/{id}")
    public AjaxResult getConversation(@PathVariable Long id) {
        return success(chatService.selectConversationById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:add')")
    @Log(title = "AI会话", businessType = BusinessType.INSERT)
    @PostMapping("/conversations")
    public AjaxResult addConversation(@RequestBody AiConversation conversation) {
        conversation.setUserId(SecurityUtils.getUserId());
        conversation.setCreateBy(SecurityUtils.getUsername());
        Long id = chatService.createConversation(conversation);
        return success(id);
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:edit')")
    @Log(title = "AI会话", businessType = BusinessType.UPDATE)
    @PutMapping("/conversations")
    public AjaxResult editConversation(@Validated @RequestBody AiConversation conversation) {
        conversation.setUpdateBy(getUsername());
        return toAjax(chatService.updateConversation(conversation));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:remove')")
    @Log(title = "AI会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/conversations/{id}")
    public AjaxResult removeConversation(@PathVariable Long[] id) {
        chatService.deleteConversation(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:query')")
    @GetMapping("/conversations/{id}/messages")
    public AjaxResult getMessages(@PathVariable Long id) {
        List<AiMessage> messages = chatService.listMessages(id);
        return AjaxResult.success(messages);
    }

    /**
     * 重命名会话
     */
    @PreAuthorize("@ss.hasPermi('ai:chat:edit')")
    @Log(title = "AI会话重命名", businessType = BusinessType.UPDATE)
    @PutMapping("/conversations/{id}/rename")
    public AjaxResult renameConversation(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        chatService.renameConversation(id, title);
        return success();
    }

    /**
     * 重新生成会话标题
     */
    @PreAuthorize("@ss.hasPermi('ai:chat:edit')")
    @Log(title = "AI会话标题生成", businessType = BusinessType.UPDATE)
    @PostMapping("/conversations/{id}/generate-title")
    public AjaxResult generateTitle(@PathVariable Long id) {
        String title = chatService.generateTitle(id);
        return success(title);
    }

    // ==================== 消息发送（已废弃，流式聊天已迁移至 FastAPI /api/v1/chat/stream） ====================

    /**
     * @deprecated 流式聊天已迁移至 FastAPI {@code POST /api/v1/chat/stream}，
     *             前端请直接连接 FastAPI SSE 端点。
     */
    @Deprecated
    @PreAuthorize("@ss.hasPermi('ai:chat:send')")
    @Log(title = "AI聊天（废弃）", businessType = BusinessType.OTHER)
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody ChatRequestDto dto) {
        ChatResponseDto response = chatService.chat(dto);
        return AjaxResult.success(response);
    }
}