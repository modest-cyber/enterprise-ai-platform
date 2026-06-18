package com.aiplatform.ai.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 模型配置 DTO
 * <p>
 * 用于新增/修改模型配置的入参封装，覆盖 AiModel 的核心字段并携带校验注解。
 * Controller 通过 @Valid @RequestBody 触发校验。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class ModelConfigDto {

    /** 模型ID（修改时必填，新增时可为空） */
    private Long modelId;

    /** 模型名称（API调用标识，如 deepseek-chat） */
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    /** 展示名称（前端显示，如 DeepSeek-V3） */
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

    /** 模型类型：chat/embedding/rerank */
    @NotBlank(message = "模型类型不能为空")
    @Size(max = 30, message = "模型类型长度不能超过30个字符")
    private String modelType;

    /** 最大输出Token数 */
    @Positive(message = "最大Token数必须大于0")
    private Integer maxTokens;

    /** 默认温度参数（0-2） */
    @DecimalMin(value = "0.0", message = "温度不能小于0")
    @DecimalMax(value = "2.0", message = "温度不能大于2")
    private Double temperature;

    /** 是否默认模型：0-否，1-是 */
    private Integer isDefault;

    /** 是否启用：1-是，0-否 */
    private Integer isEnabled;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
