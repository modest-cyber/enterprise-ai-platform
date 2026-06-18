package com.aiplatform.ai.service.impl;

import com.aiplatform.ai.domain.KnowledgeDoc;
import com.aiplatform.ai.service.IKnowledgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    @Override
    public List<KnowledgeDoc> listDocs() {
        return new ArrayList<>();
    }

    @Override
    public KnowledgeDoc getDocById(Long id) {
        return null;
    }

    @Override
    public int addDoc(KnowledgeDoc doc) {
        return 0;
    }

    @Override
    public int updateDoc(KnowledgeDoc doc) {
        return 0;
    }

    @Override
    public int deleteDocByIds(Long[] ids) {
        return 0;
    }

    @Override
    public List<String> splitDoc(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(content.split("\\n{2,}"));
    }
}