package com.aiplatform.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息保存请求 DTO — FastAPI 流式完成后回调保存消息
 *
 * @author aiplatform
 */
@Getter
@Setter
public class MessageSaveRequestDto {

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /** 用户消息内容 */
    @NotBlank(message = "用户消息不能为空")
    private String userMessage;

    /** AI 回复内容 */
    @NotBlank(message = "AI回复不能为空")
    private String aiMessage;

    /** Token 用量信息 */
    private TokenUsage tokenUsage;

    @Getter
    @Setter
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
