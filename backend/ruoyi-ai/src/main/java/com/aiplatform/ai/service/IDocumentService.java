package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.dto.KnowledgeUploadDto;

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
}