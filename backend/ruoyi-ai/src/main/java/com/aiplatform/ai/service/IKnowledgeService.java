package com.aiplatform.ai.service;

import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.KbDocument;

import java.util.List;

/**
 * 知识库服务 —— 负责知识库和文档 CRUD 管理，RAG 检索由 Python Agent 完成
 *
 * @author aiplatform
 */
public interface IKnowledgeService {

    KbKnowledge selectKnowledgeById(Long kbId);

    List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge);

    int insertKnowledge(KbKnowledge knowledge);

    int updateKnowledge(KbKnowledge knowledge);

    int deleteKnowledgeByIds(Long[] kbIds);

    List<KbDocument> selectDocumentsByKbId(Long kbId);

    KbDocument selectDocumentById(Long docId);

    int insertDocument(KbDocument document);

    int updateDocument(KbDocument document);

    int deleteDocumentByIds(Long[] docIds);
}