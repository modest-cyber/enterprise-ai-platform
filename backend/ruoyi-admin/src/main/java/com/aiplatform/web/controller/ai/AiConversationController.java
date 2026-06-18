package com.aiplatform.web.controller.ai;

import java.util.List;
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
import com.aiplatform.ai.service.IAiConversationService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.page.TableDataInfo;
import com.aiplatform.common.enums.BusinessType;

/**
 * AI 会话管理
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/conversation")
public class AiConversationController extends BaseController {

    @Autowired
    private IAiConversationService conversationService;

    /**
     * 获取会话列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiConversation conversation) {
        startPage();
        List<AiConversation> list = conversationService.selectConversationList(conversation);
        return getDataTable(list);
    }

    /**
     * 根据会话ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(conversationService.selectById(id));
    }

    /**
     * 新增会话
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:add')")
    @Log(title = "AI会话", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiConversation conversation) {
        conversation.setCreateBy(getUsername());
        return toAjax(conversationService.insert(conversation));
    }

    /**
     * 修改会话
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:edit')")
    @Log(title = "AI会话", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiConversation conversation) {
        conversation.setUpdateBy(getUsername());
        return toAjax(conversationService.update(conversation));
    }

    /**
     * 删除会话
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:remove')")
    @Log(title = "AI会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        conversationService.deleteByIds(ids);
        return success();
    }

    /**
     * 获取会话消息列表
     */
    @PreAuthorize("@ss.hasPermi('ai:conversation:query')")
    @GetMapping("/{id}/messages")
    public AjaxResult getMessages(@PathVariable Long id) {
        List<AiMessage> messages = conversationService.selectMessagesByConversationId(id);
        return AjaxResult.success(messages);
    }
}