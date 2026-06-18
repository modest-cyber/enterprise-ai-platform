package com.aiplatform.ai.service;

import com.aiplatform.ai.domain.KnowledgeDoc;

import java.util.List;

/**
 * 知识库服务 —— 负责文档 CRUD 管理，RAG 检索由 Python Agent 完成
 */
public interface IKnowledgeService {

    List<KnowledgeDoc> listDocs();

    KnowledgeDoc getDocById(Long id);

    int addDoc(KnowledgeDoc doc);

    int updateDoc(KnowledgeDoc doc);

    int deleteDocByIds(Long[] ids);

    /**
     * 文档切分（文本预处理后交给 Python 做向量化和索引）
     */
    List<String> splitDoc(String content);
}