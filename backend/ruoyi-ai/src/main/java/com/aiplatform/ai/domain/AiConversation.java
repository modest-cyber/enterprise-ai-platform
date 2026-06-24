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
 * AI会话表 ai_conversation
 * <p>
 * 记录每次AI对话的会话信息，包含会话标题、Agent类型等元数据。
 * 一个会话包含多条消息（ai_message），会话标题由首条消息自动截取。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AiConversation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 会话主键 */
    private Long conversationId;

    /** 用户ID（关联 sys_user.user_id） */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 会话标题（自动截取首条消息前30字） */
    @Size(max = 200, message = "会话标题长度不能超过200个字符")
    private String title;

    /** Agent类型：planner-规划/rag-知识检索/code-代码生成/review-代码审查 */
    @Size(max = 50, message = "Agent类型长度不能超过50个字符")
    private String agentType;

    /** 绑定的Agent ID */
    private Long agentId;

    /** 绑定的模型 ID */
    private Long modelId;

    /** 绑定的知识库 ID */
    private Long knowledgeId;

    /** 状态：1-进行中，0-已归档 */
    private Integer status;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("conversationId", getConversationId())
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("agentType", getAgentType())
            .append("agentId", getAgentId())
            .append("modelId", getModelId())
            .append("knowledgeId", getKnowledgeId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}