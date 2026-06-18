package com.aiplatform.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * MCP工具注册表 ai_tool
 *
 * @author aiplatform
 */
public class AiTool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long toolId;

    private String toolName;

    private String displayName;

    private String toolType;

    private String description;

    private String serverUrl;

    private String inputSchema;

    private String outputSchema;

    private String authType;

    private String authConfig;

    private Integer timeoutMs;

    private Integer retryCount;

    private Integer isEnabled;

    public Long getToolId()
    {
        return toolId;
    }

    public void setToolId(Long toolId)
    {
        this.toolId = toolId;
    }

    public String getToolName()
    {
        return toolName;
    }

    public void setToolName(String toolName)
    {
        this.toolName = toolName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getToolType()
    {
        return toolType;
    }

    public void setToolType(String toolType)
    {
        this.toolType = toolType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    public String getInputSchema()
    {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema)
    {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema()
    {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema)
    {
        this.outputSchema = outputSchema;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    public String getAuthConfig()
    {
        return authConfig;
    }

    public void setAuthConfig(String authConfig)
    {
        this.authConfig = authConfig;
    }

    public Integer getTimeoutMs()
    {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public Integer getIsEnabled()
    {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("toolId", getToolId())
            .append("toolName", getToolName())
            .append("displayName", getDisplayName())
            .append("toolType", getToolType())
            .append("description", getDescription())
            .append("serverUrl", getServerUrl())
            .append("authType", getAuthType())
            .append("timeoutMs", getTimeoutMs())
            .append("retryCount", getRetryCount())
            .append("isEnabled", getIsEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}