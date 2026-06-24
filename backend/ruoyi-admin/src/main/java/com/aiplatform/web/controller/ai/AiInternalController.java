package com.aiplatform.web.controller.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.dto.ConversationConfigDto;
import com.aiplatform.ai.domain.dto.MessageSaveRequestDto;
import com.aiplatform.ai.service.IAiChatService;
import com.aiplatform.ai.service.IDocumentService;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.framework.web.service.InternalTokenService;

import jakarta.validation.Valid;

/**
 * AI 内部接口 — 供 FastAPI 调用，使用 Internal Token 认证
 * <p>
 * 所有接口由 InternalTokenFilter 进行认证，不经过用户 JWT。
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/internal")
public class AiInternalController extends BaseController {

    @Autowired
    private InternalTokenService internalTokenService;

    @Autowired
    private IAiChatService chatService;

    @Autowired
    private IDocumentService documentService;

    /**
     * 获取内部 JWT
     * 认证方式：X-Internal-Secret 预共享密钥（由 InternalTokenFilter 验证）
     */
    @PostMapping("/auth/token")
    public AjaxResult generateToken() {
        String token = internalTokenService.generateToken();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", 1800); // 30 分钟 = 1800 秒
        return AjaxResult.success(data);
    }

    /**
     * 获取会话配置（Agent + Model + 历史消息）
     * 认证方式：X-Internal-Token（由 InternalTokenFilter 验证）
     */
    @GetMapping("/conversation/{id}")
    public AjaxResult getConversationConfig(@PathVariable Long id) {
        ConversationConfigDto config = chatService.getConversationConfig(id);
        return AjaxResult.success(config);
    }

    /**
     * 创建新会话（供 FastAPI 在 conversationId=null 时调用）
     * 认证方式：X-Internal-Token（由 InternalTokenFilter 验证）
     */
    @PostMapping("/conversation")
    public AjaxResult createConversation(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String title = body.get("title") != null ? body.get("title").toString() : "新会话";
        Long agentId = body.get("agentId") != null ? Long.valueOf(body.get("agentId").toString()) : null;
        Long modelId = body.get("modelId") != null ? Long.valueOf(body.get("modelId").toString()) : null;
        Long knowledgeId = body.get("knowledgeId") != null ? Long.valueOf(body.get("knowledgeId").toString()) : null;
        ConversationConfigDto config = chatService.createConversationFromInternal(userId, title, agentId, modelId, knowledgeId);
        return AjaxResult.success(config);
    }

    /**
     * 批量获取文档信息（供 FastAPI RAG 来源展示调用）
     * 认证方式：X-Internal-Token（由 InternalTokenFilter 验证）
     */
    @GetMapping("/documents")
    public AjaxResult getDocumentsBatch(@RequestParam String ids) {
        String[] idArr = ids.split(",");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String idStr : idArr) {
            Long docId = Long.valueOf(idStr.trim());
            KbDocument doc = documentService.getDocument(docId);
            if (doc != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("docId", doc.getDocId());
                item.put("fileName", doc.getFileName());
                result.add(item);
            }
        }
        return AjaxResult.success(result);
    }

    /**
     * 保存消息（用户消息 + AI 回复）
     * 认证方式：X-Internal-Token（由 InternalTokenFilter 验证）
     */
    @PostMapping("/message/save")
    public AjaxResult saveMessage(@Valid @RequestBody MessageSaveRequestDto dto) {
        chatService.saveMessage(dto);
        return AjaxResult.success();
    }
}
