package com.aiplatform.ai.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * Agent配置表 agent_config
 * <p>
 * Agent是AI任务的执行单元，定义了System Prompt模板、绑定的模型、工具集和工作流配置。
 * 每种Agent类型（planner/rag/code/review/tool/custom）对应Python Agent端的一个LangGraph工作流。
 * Agent配置可绑定MCP工具列表，在执行时由PlannerAgent根据任务类型分发到对应Agent。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AgentConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Agent主键 */
    private Long agentId;

    /** Agent名称（唯一标识） */
    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称长度不能超过100个字符")
    private String agentName;

    /** Agent类型：planner/rag/code/review/tool/custom */
    @NotBlank(message = "Agent类型不能为空")
    @Size(max = 50, message = "Agent类型长度不能超过50个字符")
    private String agentType;

    /** Agent描述（用于前端展示和Agent选择） */
    @Size(max = 500, message = "Agent描述长度不能超过500个字符")
    private String description;

    /** 绑定的模型ID（关联 ai_model.model_id） */
    private Long modelId;

    /** System Prompt模板（支持变量占位符，执行时动态渲染） */
    private String systemPrompt;

    /** 绑定的MCP工具ID列表（JSON数组） */
    private String toolsJson;

    /** LangGraph工作流配置JSON（定义Agent的执行流程图） */
    private String workflowJson;

    /** 最大迭代次数（防止Agent无限循环） */
    @Min(value = 1, message = "最大迭代次数不能小于1")
    private Integer maxIterations;

    /** 温度参数（0.0-2.0，控制输出随机性） */
    private Double temperature;

    /** 超时时间（秒），单次Agent执行的最大时长 */
    @PositiveOrZero(message = "超时时间不能为负数")
    private Integer timeoutSeconds;

    /** 状态：1-启用，0-停用 */
    private Integer status;

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