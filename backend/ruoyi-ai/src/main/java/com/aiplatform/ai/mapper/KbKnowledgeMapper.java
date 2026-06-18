package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.KbKnowledge;

/**
 * 知识库 数据层
 *
 * @author aiplatform
 */
public interface KbKnowledgeMapper
{
    KbKnowledge selectKnowledge(KbKnowledge knowledge);

    KbKnowledge selectKnowledgeById(Long kbId);

    List<KbKnowledge> selectKnowledgeList(KbKnowledge knowledge);

    int insertKnowledge(KbKnowledge knowledge);

    int updateKnowledge(KbKnowledge knowledge);

    int deleteKnowledgeById(Long kbId);

    int deleteKnowledgeByIds(Long[] kbIds);
}