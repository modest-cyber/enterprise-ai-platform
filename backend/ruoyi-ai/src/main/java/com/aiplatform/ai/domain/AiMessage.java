package com.aiplatform.ai.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * AI消息表 ai_message
 * <p>
 * 存储AI对话的每条消息，包含用户输入和AI回复。
 * 支持 Markdown 和代码块，支持记录 Token 消耗和消息来源类型。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AiMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 消息主键 */
    private Long messageId;

    /** 会话ID（关联 ai_conversation.conversation_id） */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /** 角色：user-用户/assistant-AI回复/system-系统提示/tool-工具调用 */
    @NotBlank(message = "消息角色不能为空")
    @Size(max = 20, message = "角色长度不能超过20个字符")
    private String role;

    /** 消息内容（支持Markdown和代码块） */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** Token消耗数量 */
    private Integer tokenCount;

    /** 使用的模型名称（如deepseek-chat） */
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    /** 消息来源：llm-大模型/rag-知识检索/tool-工具调用/agent-Agent */
    @Size(max = 30, message = "消息来源长度不能超过30个字符")
    private String sourceType;

    /** 元数据JSON（引用的文档ID、工具调用结果等扩展信息） */
    private String metadataJson;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("messageId", getMessageId())
            .append("conversationId", getConversationId())
            .append("role", getRole())
            .append("content", getContent())
            .append("tokenCount", getTokenCount())
            .append("modelName", getModelName())
            .append("sourceType", getSourceType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}