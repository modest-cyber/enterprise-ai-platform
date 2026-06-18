package com.aiplatform.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * AI会话表 ai_conversation
 *
 * @author aiplatform
 */
public class AiConversation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long conversationId;

    private Long userId;

    private String title;

    private String agentType;

    private Integer status;

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAgentType()
    {
        return agentType;
    }

    public void setAgentType(String agentType)
    {
        this.agentType = agentType;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("conversationId", getConversationId())
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("agentType", getAgentType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}