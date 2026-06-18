package com.aiplatform.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * Agent配置表 agent_config
 *
 * @author aiplatform
 */
public class AgentConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long agentId;

    private String agentName;

    private String agentType;

    private String description;

    private Long modelId;

    private String systemPrompt;

    private String toolsJson;

    private String workflowJson;

    private Integer maxIterations;

    private Double temperature;

    private Integer timeoutSeconds;

    private Integer status;

    public Long getAgentId()
    {
        return agentId;
    }

    public void setAgentId(Long agentId)
    {
        this.agentId = agentId;
    }

    public String getAgentName()
    {
        return agentName;
    }

    public void setAgentName(String agentName)
    {
        this.agentName = agentName;
    }

    public String getAgentType()
    {
        return agentType;
    }

    public void setAgentType(String agentType)
    {
        this.agentType = agentType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt)
    {
        this.systemPrompt = systemPrompt;
    }

    public String getToolsJson()
    {
        return toolsJson;
    }

    public void setToolsJson(String toolsJson)
    {
        this.toolsJson = toolsJson;
    }

    public String getWorkflowJson()
    {
        return workflowJson;
    }

    public void setWorkflowJson(String workflowJson)
    {
        this.workflowJson = workflowJson;
    }

    public Integer getMaxIterations()
    {
        return maxIterations;
    }

    public void setMaxIterations(Integer maxIterations)
    {
        this.maxIterations = maxIterations;
    }

    public Double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(Double temperature)
    {
        this.temperature = temperature;
    }

    public Integer getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds)
    {
        this.timeoutSeconds = timeoutSeconds;
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
            .append("agentId", getAgentId())
            .append("agentName", getAgentName())
            .append("agentType", getAgentType())
            .append("description", getDescription())
            .append("modelId", getModelId())
            .append("maxIterations", getMaxIterations())
            .append("temperature", getTemperature())
            .append("timeoutSeconds", getTimeoutSeconds())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}