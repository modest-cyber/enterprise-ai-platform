package com.aiplatform.ai.service;

import java.util.List;
import com.aiplatform.ai.domain.AiToolTemplate;

/**
 * 工具模板服务接口
 *
 * @author aiplatform
 */
public interface IToolTemplateService {

    /**
     * 根据模板标识查询模板详情（含JSON Schema）
     */
    AiToolTemplate selectTemplateByCode(String templateCode);

    /**
     * 查询所有已启用的工具模板
     */
    List<AiToolTemplate> selectEnabledTemplates();
}
