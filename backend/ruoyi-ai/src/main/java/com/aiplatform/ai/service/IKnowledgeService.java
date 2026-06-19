package com.aiplatform.ai.service;

import java.util.List;
import java.util.Map;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.dto.SearchRequestDto;

public interface IKnowledgeService {

    List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge);

    KbKnowledge selectKnowledgeById(Long kbId);

    int insertKnowledge(KbKnowledge knowledge);

    int updateKnowledge(KbKnowledge knowledge);

    int deleteKnowledgeByIds(Long[] kbIds);

    List<Map<String, Object>> search(SearchRequestDto dto);

    List<KbDocument> selectDocList(Long kbId);

    KbDocument selectDocById(Long docId);
}