package com.aiplatform.ai.service;

import java.util.Map;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.dto.KnowledgeUploadDto;
import com.aiplatform.ai.domain.vo.DocumentProcessVo;

/**
 * 文档服务接口
 *
 * @author aiplatform
 */

public interface IDocumentService {

    KbDocument uploadDocument(KnowledgeUploadDto dto);

    int deleteDocs(Long[] docIds);

    int reindex(Long docId, Integer chunkSize, Integer chunkOverlap);

    void updateDocStatus(Long docId, int status, String errorMsg);

    /** 触发文档向量化处理 */
    void processDocument(Long documentId);

    /** 查询文档处理状态 */
    DocumentProcessVo getProcessStatus(Long documentId);

    /** 获取文档内容（用于预览） */
    String getDocumentContent(Long documentId);

    /** 获取知识库文档统计 */
    Map<String, Object> getDocStats(Long kbId);

    /** 根据ID获取文档 */
    KbDocument getDocument(Long documentId);

    /** 获取文档文件的服务器完整路径 */
    String getDocumentFilePath(Long documentId);
}
