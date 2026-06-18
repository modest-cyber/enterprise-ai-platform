package com.aiplatform.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.aiplatform.common.core.domain.BaseEntity;

/**
 * 知识库条目表 kb_knowledge
 *
 * @author aiplatform
 */
public class KbKnowledge extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long kbId;

    private String name;

    private String description;

    private String kbType;

    private String embeddingModel;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private Integer status;

    private Integer docCount;

    public Long getKbId()
    {
        return kbId;
    }

    public void setKbId(Long kbId)
    {
        this.kbId = kbId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getKbType()
    {
        return kbType;
    }

    public void setKbType(String kbType)
    {
        this.kbType = kbType;
    }

    public String getEmbeddingModel()
    {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel)
    {
        this.embeddingModel = embeddingModel;
    }

    public Integer getChunkSize()
    {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize)
    {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap()
    {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap)
    {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getDocCount()
    {
        return docCount;
    }

    public void setDocCount(Integer docCount)
    {
        this.docCount = docCount;
    }

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