package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.KbDocument;

/**
 * 知识库服务 —— 负责知识库和文档的CRUD管理
 * <p>
 * 知识库（KbKnowledge）是文档的逻辑分组容器，文档（KbDocument）是实际的知识载体。
 * 文档上传后异步进行文本提取→分段→Embedding→Milvus索引，RAG检索由Python Agent完成。
 * 采用逻辑删除（is_delete字段），支持文档重新索引（reindex）。
 *
 * @author aiplatform
 */
public interface IKnowledgeService {

    /**
     * 根据ID查询知识库
     */
    KbKnowledge selectKnowledgeById(Long kbId);

    /**
     * 分页查询知识库列表（配合 PageHelper.startPage()）
     */
    List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge);

    /**
     * 新增知识库
     */
    int insertKnowledge(KbKnowledge knowledge);

    /**
     * 修改知识库
     */
    int updateKnowledge(KbKnowledge knowledge);

    /**
     * 批量删除知识库（物理删除）
     */
    int deleteKnowledgeByIds(Long[] kbIds);

    /**
     * 查询知识库下的文档列表
     */
    List<KbDocument> selectDocumentsByKbId(Long kbId);

    /**
     * 根据ID查询文档
     */
    KbDocument selectDocumentById(Long docId);

    /**
     * 新增文档记录
     */
    int insertDocument(KbDocument document);

    /**
     * 更新文档信息
     */
    int updateDocument(KbDocument document);

    /**
     * 批量删除文档（逻辑删除，设置 is_delete=1）
     */
    int deleteDocumentByIds(Long[] docIds);
}