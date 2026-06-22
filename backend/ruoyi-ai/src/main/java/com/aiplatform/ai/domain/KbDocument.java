package com.aiplatform.ai.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 *
 * @author aiplatform
 */
@Getter
@Setter
public class KbDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 文档主键 */
    private Long docId;

    /** 知识库ID */
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

    /** 文件存储路径 */
    @Size(max = 500, message = "文件路径长度不能超过500个字符")
    private String filePath;

    /** 提取后的原始文本内容 */
    private String contentText;

    /** 文本切分块数 */
    private Integer chunkCount;

    /** 向量化后的向量数量 */
    private Integer vectorCount;

    /** Milvus向量ID列表 */
    private String vectorIds;

    /** 旧状态字段：0-待处理，1-处理中，2-已完成，3-失败 */
    private Integer status;

    /** 处理状态：PENDING/PROCESSING/SUCCESS/FAILED */
    private String processStatus;

    /** 处理进度 0-100 */
    private Integer processProgress;

    /** 处理信息 */
    @Size(max = 500, message = "处理信息长度不能超过500个字符")
    private String processMessage;

    /** 处理完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedTime;

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
            .append("vectorCount", getVectorCount())
            .append("processStatus", getProcessStatus())
            .append("processProgress", getProcessProgress())
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
