package com.aiplatform.ai.domain;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 工具模板表 ai_tool_template
 * <p>
 * 预置工具模板，包含默认的 inputSchema/outputSchema/authConfig，
 * 用户创建工具时选择模板即可自动填充，无需手动编写 JSON Schema。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class AiToolTemplate {

    private static final long serialVersionUID = 1L;

    /** 模板主键 */
    private Long templateId;

    /** 模板标识（唯一） */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 工具分类：file/network/database/search/code/enterprise */
    private String category;

    /** 模板描述 */
    private String description;

    /** 工具类型 */
    private String toolType;

    /** 建议显示名称 */
    private String displayName;

    /** 默认服务URL */
    private String serverUrl;

    /** 输入参数JSON Schema */
    private String inputSchema;

    /** 输出参数JSON Schema */
    private String outputSchema;

    /** 认证方式 */
    private String authType;

    /** 默认认证配置 */
    private String authConfig;

    /** 状态：1-启用，0-停用 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("templateCode", getTemplateCode())
            .append("templateName", getTemplateName())
            .append("category", getCategory())
            .append("toolType", getToolType())
            .append("status", getStatus())
            .toString();
    }
}
