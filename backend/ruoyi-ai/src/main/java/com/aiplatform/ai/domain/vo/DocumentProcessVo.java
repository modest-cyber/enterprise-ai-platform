package com.aiplatform.ai.domain.vo;

import lombok.Data;

/**
 * 文档处理状态 VO
 *
 * @author aiplatform
 */
@Data
public class DocumentProcessVo {

    /** 文档ID */
    private Long docId;

    /** 处理状态：PENDING/PROCESSING/SUCCESS/FAILED */
    private String status;

    /** 处理进度 0-100 */
    private Integer progress;

    /** 处理信息 */
    private String message;

    /** 切块数 */
    private Integer chunkCount;

    /** 向量数 */
    private Integer vectorCount;
}
