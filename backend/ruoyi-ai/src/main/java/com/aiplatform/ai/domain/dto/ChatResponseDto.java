package com.aiplatform.ai.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天响应 DTO
 *
 * @author aiplatform
 */
@Data
public class ChatResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long conversationId;

    /** 会话标题 */
    private String title;

    /** 消息ID */
    private Long messageId;

    /** 回复内容 */
    private String content;

    /** 角色：user/assistant */
    private String role;

    /** Agent类型 */
    private String agentType;

    /** 引用的知识来源 */
    private Object sources;

    /** Token用量信息 */
    private Object tokenUsage;

}