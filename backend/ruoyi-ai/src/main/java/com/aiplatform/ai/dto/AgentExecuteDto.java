package com.aiplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Agent 执行请求 DTO
 * <p>
 * 用于同步/异步执行 Agent 任务的入参封装。
 * task 为自然语言任务描述，input 为 JSON 格式的输入参数。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AgentExecuteDto {

    /** Agent ID */
    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    /** 任务描述（自然语言） */
    @NotBlank(message = "任务描述不能为空")
    private String task;

    /** 输入参数（JSON 字符串，可选） */
    private String input;

    /** 是否异步执行：false-同步（默认），true-异步 */
    private Boolean async = false;
}
