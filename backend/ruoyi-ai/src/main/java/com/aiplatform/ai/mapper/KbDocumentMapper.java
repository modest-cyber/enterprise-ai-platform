package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.KbDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 数据层
 *
 * @author aiplatform
 */
@Mapper
public interface KbDocumentMapper
{
    KbDocument selectDocument(KbDocument document);

    KbDocument selectDocumentById(Long docId);

    List<KbDocument> selectDocumentList(KbDocument document);

    List<KbDocument> selectDocumentsByKbId(Long kbId);

    int insertDocument(KbDocument document);

    int updateDocument(KbDocument document);

    int deleteDocumentById(Long docId);

    int deleteDocumentByIds(Long[] docIds);

    List<java.util.Map<String, Object>> countDocsByStatus(Long kbId);
}