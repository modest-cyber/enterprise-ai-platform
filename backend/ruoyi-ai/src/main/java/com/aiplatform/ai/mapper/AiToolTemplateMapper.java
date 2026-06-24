package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiToolTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工具模板 数据层
 *
 * @author aiplatform
 */
@Mapper
public interface AiToolTemplateMapper {

    /**
     * 根据模板标识查询
     */
    AiToolTemplate selectTemplateByCode(String templateCode);

    /**
     * 查询所有启用的模板
     */
    List<AiToolTemplate> selectEnabledTemplates();

    /**
     * 按分类查询模板
     */
    List<AiToolTemplate> selectTemplatesByCategory(String category);
}
