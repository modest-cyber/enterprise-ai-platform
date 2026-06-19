package com.aiplatform.ai.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.dto.SearchRequestDto;
import com.aiplatform.ai.mapper.KbDocumentMapper;
import com.aiplatform.ai.mapper.KbKnowledgeMapper;
import com.aiplatform.ai.service.IKnowledgeService;
import com.aiplatform.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    @Autowired
    private KbKnowledgeMapper knowledgeMapper;

    @Autowired
    private KbDocumentMapper documentMapper;

    @Override
    public List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge) {
        return knowledgeMapper.selectKnowledgeList(knowledge);
    }

    @Override
    public KbKnowledge selectKnowledgeById(Long kbId) {
        return knowledgeMapper.selectKnowledgeById(kbId);
    }

    @Override
    public int insertKnowledge(KbKnowledge knowledge) {
        Date now = DateUtils.getNowDate();
        knowledge.setCreateTime(now);
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        if (knowledge.getDocCount() == null) {
            knowledge.setDocCount(0);
        }
        if (knowledge.getChunkSize() == null) {
            knowledge.setChunkSize(512);
        }
        if (knowledge.getChunkOverlap() == null) {
            knowledge.setChunkOverlap(50);
        }
        return knowledgeMapper.insertKnowledge(knowledge);
    }

    @Override
    public int updateKnowledge(KbKnowledge knowledge) {
        knowledge.setUpdateTime(DateUtils.getNowDate());
        return knowledgeMapper.updateKnowledge(knowledge);
    }

    @Override
    public int deleteKnowledgeByIds(Long[] kbIds) {
        return knowledgeMapper.deleteKnowledgeByIds(kbIds);
    }

    @Override
    public List<Map<String, Object>> search(SearchRequestDto dto) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (dto.getKbIds() == null || dto.getKbIds().isEmpty()) {
            return results;
        }
        for (Long kbId : dto.getKbIds()) {
            KbDocument query = new KbDocument();
            query.setKbId(kbId);
            List<KbDocument> docs = documentMapper.selectDocumentList(query);
            for (KbDocument doc : docs) {
                if (doc.getContentText() != null
                        && doc.getContentText().toLowerCase().contains(dto.getQuery().toLowerCase())) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("docId", doc.getDocId());
                    item.put("fileName", doc.getFileName());
                    item.put("kbId", doc.getKbId());
                    item.put("content", doc.getContentText());
                    item.put("score", 0.85);
                    item.put("snippet", getSnippet(doc.getContentText(), dto.getQuery()));
                    results.add(item);
                }
            }
        }
        return results;
    }

    private String getSnippet(String content, String query) {
        if (content == null || content.isEmpty()) return "";
        int idx = content.toLowerCase().indexOf(query.toLowerCase());
        if (idx < 0) return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        int start = Math.max(0, idx - 80);
        int end = Math.min(content.length(), idx + query.length() + 80);
        String snippet = content.substring(start, end);
        return (start > 0 ? "..." : "") + snippet + (end < content.length() ? "..." : "");
    }

    @Override
    public List<KbDocument> selectDocList(Long kbId) {
        KbDocument query = new KbDocument();
        query.setKbId(kbId);
        return documentMapper.selectDocumentList(query);
    }

    @Override
    public KbDocument selectDocById(Long docId) {
        return documentMapper.selectDocumentById(docId);
    }
}