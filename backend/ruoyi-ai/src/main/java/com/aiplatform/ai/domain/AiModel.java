package com.aiplatform.ai.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * AI模型配置表 ai_model
 * <p>
 * 管理所有LLM模型配置，支持多Provider（OpenAI/DeepSeek/Qwen/Ollama）和多模型类型（chat/embedding/rerank）。
 * 每个模型包含API Key、Base URL、温度等参数。
 * 模型配置由ModelConfigService统一管理，ChatService和AgentService通过选择的模型调用LLM。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AiModel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 模型主键 */
    private Long modelId;

    /** 模型名称（API调用标识，如deepseek-chat） */
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    /** 展示名称（前端显示，如DeepSeek-V3） */
    @NotBlank(message = "展示名称不能为空")
    @Size(max = 200, message = "展示名称长度不能超过200个字符")
    private String displayName;

    /** Provider：openai/deepseek/qwen/ollama */
    @NotBlank(message = "Provider不能为空")
    @Size(max = 50, message = "Provider长度不能超过50个字符")
    private String provider;

    /** API Key（加密存储，用于调用LLM API鉴权） */
    @NotBlank(message = "API Key不能为空")
    @Size(max = 500, message = "API Key长度不能超过500个字符")
    private String apiKey;

    /** API Base URL（LLM服务端点完整地址） */
    @NotBlank(message = "Base URL不能为空")
    @Size(max = 300, message = "Base URL长度不能超过300个字符")
    private String baseUrl;

    /** 模型类型：chat-对话/embedding-向量化/rerank-重排序 */
    @Size(max = 30, message = "模型类型长度不能超过30个字符")
    private String modelType;

    /** 最大输出Token数 */
    @Positive(message = "最大Token数必须大于0")
    private Integer maxTokens;

    /** 默认温度参数（0-2） */
    private Double temperature;

    /** 是否启用：1-是，0-否 */
    private Integer isEnabled;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("modelId", getModelId())
            .append("modelName", getModelName())
            .append("displayName", getDisplayName())
            .append("provider", getProvider())
            .append("modelType", getModelType())
            .append("maxTokens", getMaxTokens())
            .append("temperature", getTemperature())
            .append("isEnabled", getIsEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}