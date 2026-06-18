package com.aiplatform.ai.domain;

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
 * 知识文档表 kb_document
 * <p>
 * 记录上传到知识库的文档，包含文件元信息、提取的文本内容和向量化状态。
 * 文档上传后异步进行文本提取→分段→Embedding→Milvus索引的四步处理链。
 * 采用逻辑删除（is_delete字段），支持文档重新索引。
 *
 * @author aiplatform
 */
@Getter
@Setter
public class KbDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 文档主键 */
    private Long docId;

    /** 知识库ID（关联 kb_knowledge.kb_id） */
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    /** 文件名 */
    @NotBlank(message = "文件名不能为空")
    @Size(max = 300, message = "文件名长度不能超过300个字符")
    private String fileName;

    /** 文件类型：txt/pdf/md/docx/xlsx/html */
    @Size(max = 20, message = "文件类型长度不能超过20个字符")
    private String fileType;

    /** 文件大小（字节） */
    @Min(value = 0, message = "文件大小不能为负数")
    private Long fileSize;

    /** 文件存储路径（磁盘或OSS完整路径） */
    @Size(max = 500, message = "文件路径长度不能超过500个字符")
    private String filePath;

    /** 提取后的原始文本内容 */
    private String contentText;

    /** 文本切分块数 */
    private Integer chunkCount;

    /** Milvus向量ID列表（JSON数组，用于追踪和批量删除） */
    private String vectorIds;

    /**
     * 文档处理状态：
     * 0-待处理（已上传，等待异步处理）
     * 1-处理中（正在提取文本/向量化）
     * 2-已完成（已索引，可检索）
     * 3-失败（处理异常，见error_msg）
     */
    private Integer status;

    /** 处理失败原因 */
    @Size(max = 500, message = "错误信息长度不能超过500个字符")
    private String errorMsg;

    /** 逻辑删除：0-正常，1-已删除 */
    private Integer isDelete;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("kbId", getKbId())
            .append("fileName", getFileName())
            .append("fileType", getFileType())
            .append("fileSize", getFileSize())
            .append("filePath", getFilePath())
            .append("chunkCount", getChunkCount())
            .append("status", getStatus())
            .append("isDelete", getIsDelete())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}