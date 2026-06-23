package com.aiplatform.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 聊天请求 DTO
 *
 * @author aiplatform
 */
@Data
public class ChatRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID（为空时自动创建新会话） */
    private Long conversationId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息长度不能超过4000字符")
    private String message;

    /** Agent ID */
    private Long agentId;

    /** Agent类型：planner/rag/code/review */
    private String agentType;

    /** 模型ID */
    private Long modelId;

}