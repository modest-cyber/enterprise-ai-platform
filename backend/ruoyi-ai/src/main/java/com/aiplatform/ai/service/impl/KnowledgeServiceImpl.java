package com.aiplatform.ai.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.service.IKnowledgeService;
import com.aiplatform.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识库服务实现 —— 负责知识库和文档的CRUD操作
 * <p>
 * 当前为骨架实现（Mapper 未注入），P2 阶段完善后对接 KbKnowledgeMapper 和 KbDocumentMapper。
 * 文档上传、文本提取、Embedding、Milvus索引等异步操作在 P5 阶段完善。
 *
 * @author aiplatform
 */
@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    /**
     * 根据ID查询知识库
     */
    @Override
    public KbKnowledge selectKnowledgeById(Long kbId) {
        if (kbId == null) {
            throw new ServiceException("知识库ID不能为空");
        }
        return null;
    }

    /**
     * 分页查询知识库列表
     */
    @Override
    public List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge) {
        return new ArrayList<>();
    }

    /**
     * 新增知识库
     */
    @Override
    public int insertKnowledge(KbKnowledge knowledge) {
        if (knowledge == null || !StringUtils.hasText(knowledge.getName())) {
            throw new ServiceException("知识库名称不能为空");
        }
        return 0;
    }

    /**
     * 修改知识库
     */
    @Override
    public int updateKnowledge(KbKnowledge knowledge) {
        if (knowledge == null || knowledge.getKbId() == null) {
            throw new ServiceException("知识库ID不能为空");
        }
        return 0;
    }

    /**
     * 批量删除知识库（物理删除）
     */
    @Override
    public int deleteKnowledgeByIds(Long[] kbIds) {
        if (kbIds == null || kbIds.length == 0) {
            throw new ServiceException("待删除的知识库ID列表不能为空");
        }
        return 0;
    }

    /**
     * 查询知识库下的文档列表
     */
    @Override
    public List<KbDocument> selectDocumentsByKbId(Long kbId) {
        if (kbId == null) {
            throw new ServiceException("知识库ID不能为空");
        }
        return new ArrayList<>();
    }

    /**
     * 根据ID查询文档
     */
    @Override
    public KbDocument selectDocumentById(Long docId) {
        if (docId == null) {
            throw new ServiceException("文档ID不能为空");
        }
        return null;
    }

    /**
     * 新增文档记录
     */
    @Override
    public int insertDocument(KbDocument document) {
        if (document == null || document.getKbId() == null) {
            throw new ServiceException("文档所属知识库ID不能为空");
        }
        if (!StringUtils.hasText(document.getFileName())) {
            throw new ServiceException("文件名不能为空");
        }
        return 0;
    }

    /**
     * 更新文档信息
     */
    @Override
    public int updateDocument(KbDocument document) {
        if (document == null || document.getDocId() == null) {
            throw new ServiceException("文档ID不能为空");
        }
        return 0;
    }

    /**
     * 批量删除文档（逻辑删除，设置 is_delete=1）
     */
    @Override
    public int deleteDocumentByIds(Long[] docIds) {
        if (docIds == null || docIds.length == 0) {
            throw new ServiceException("待删除的文档ID列表不能为空");
        }
        return 0;
    }
}