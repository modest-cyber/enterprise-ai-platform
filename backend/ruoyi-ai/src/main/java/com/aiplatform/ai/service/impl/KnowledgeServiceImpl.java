package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.service.IKnowledgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    @Override
    public KbKnowledge selectKnowledgeById(Long kbId) {
        return null;
    }

    @Override
    public List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge) {
        return new ArrayList<>();
    }

    @Override
    public int insertKnowledge(KbKnowledge knowledge) {
        return 0;
    }

    @Override
    public int updateKnowledge(KbKnowledge knowledge) {
        return 0;
    }

    @Override
    public int deleteKnowledgeByIds(Long[] kbIds) {
        return 0;
    }

    @Override
    public List<KbDocument> selectDocumentsByKbId(Long kbId) {
        return new ArrayList<>();
    }

    @Override
    public KbDocument selectDocumentById(Long docId) {
        return null;
    }

    @Override
    public int insertDocument(KbDocument document) {
        return 0;
    }

    @Override
    public int updateDocument(KbDocument document) {
        return 0;
    }

    @Override
    public int deleteDocumentByIds(Long[] docIds) {
        return 0;
    }
}