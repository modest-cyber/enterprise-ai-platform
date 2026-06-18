package com.aiplatform.ai.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * 知识库条目表 kb_knowledge
 * <p>
 * 知识库是文档的逻辑分组，包含 Embedding 模型配置、文档切分参数等。
 * 一个知识库包含多份文档（kb_document），文档向量化后存入 Milvus。
 * RAG检索时以知识库为单位进行向量搜索。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class KbKnowledge extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 知识库主键 */
    private Long kbId;

    /** 知识库名称 */
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 200, message = "知识库名称长度不能超过200个字符")
    private String name;

    /** 知识库描述 */
    @Size(max = 500, message = "知识库描述长度不能超过500个字符")
    private String description;

    /** 知识库类型：general-通用/code-代码/doc-文档/faq-问答 */
    @Size(max = 50, message = "知识库类型长度不能超过50个字符")
    private String kbType;

    /** Embedding模型（如text-embedding-3-small） */
    @Size(max = 100, message = "Embedding模型名称长度不能超过100个字符")
    private String embeddingModel;

    /** 文档切分块大小（字符数） */
    @Min(value = 100, message = "块大小不能小于100")
    @Max(value = 8192, message = "块大小不能大于8192")
    private Integer chunkSize;

    /** 文档切分重叠大小（滑动窗口） */
    @Min(value = 0, message = "重叠大小不能小于0")
    private Integer chunkOverlap;

    /** 状态：1-启用，0-停用 */
    private Integer status;

    /** 文档数量（冗余字段，用于列表展示） */
    private Integer docCount;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("kbId", getKbId())
            .append("name", getName())
            .append("description", getDescription())
            .append("kbType", getKbType())
            .append("embeddingModel", getEmbeddingModel())
            .append("chunkSize", getChunkSize())
            .append("chunkOverlap", getChunkOverlap())
            .append("status", getStatus())
            .append("docCount", getDocCount())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}