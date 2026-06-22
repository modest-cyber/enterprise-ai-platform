package com.aiplatform.ai.domain.enums;

/**
 * 文档处理状态枚举
 *
 * @author aiplatform
 */
public enum DocumentProcessStatus {

    /** 待处理 */
    PENDING("PENDING", "待处理"),

    /** 处理中 */
    PROCESSING("PROCESSING", "处理中"),

    /** 已完成 */
    SUCCESS("SUCCESS", "已完成"),

    /** 失败 */
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    DocumentProcessStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
