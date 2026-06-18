package com.aiplatform.ai.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * MCP工具注册表 ai_tool
 * <p>
 * 管理通过MCP（Model Context Protocol）协议注册的外部工具。
 * 工具注册后可由Agent在执行任务时自动发现并调用，包括搜索引擎、数据库查询、文件操作等。
 * 每个工具包含输入/输出JSON Schema、认证配置、超时和重试策略。
 * McpToolExecutor负责工具的实际调用执行，Spring AI的Tool封装使其可被LLM直接调用。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AiTool extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工具主键 */
    private Long toolId;

    /** 工具名称（唯一标识，用于Agent推理选择工具） */
    @NotBlank(message = "工具名称不能为空")
    @Size(max = 100, message = "工具名称长度不能超过100个字符")
    private String toolName;

    /** 展示名称（前端显示） */
    @NotBlank(message = "展示名称不能为空")
    @Size(max = 200, message = "展示名称长度不能超过200个字符")
    private String displayName;

    /** 工具类型：mcp_server-MCP协议/http_api-HTTP接口/function-本地函数 */
    @NotBlank(message = "工具类型不能为空")
    @Size(max = 30, message = "工具类型长度不能超过30个字符")
    private String toolType;

    /** 工具描述（影响Agent选择准确率，应详细说明工具功能和适用场景） */
    @Size(max = 500, message = "工具描述长度不能超过500个字符")
    private String description;

    /** MCP Server URL或HTTP API URL */
    @Size(max = 500, message = "Server URL长度不能超过500个字符")
    private String serverUrl;

    /** 输入参数JSON Schema（定义工具接受的参数类型、必填项等） */
    @NotBlank(message = "输入参数Schema不能为空")
    private String inputSchema;

    /** 输出参数JSON Schema（定义工具返回结果的数据结构） */
    private String outputSchema;

    /** 认证方式：none-无/api_key-API Key/oauth2-OAuth2 */
    @Size(max = 20, message = "认证方式长度不能超过20个字符")
    private String authType;

    /** 认证配置（JSON格式，如{"api_key":"xxx"}，加密存储） */
    private String authConfig;

    /** 超时时间（毫秒），单次工具调用的最大等待时间 */
    @Min(value = 100, message = "超时时间不能小于100毫秒")
    private Integer timeoutMs;

    /** 重试次数（调用失败后的自动重试次数） */
    @Min(value = 0, message = "重试次数不能小于0")
    private Integer retryCount;

    /** 是否启用：1-是，0-否 */
    private Integer isEnabled;

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