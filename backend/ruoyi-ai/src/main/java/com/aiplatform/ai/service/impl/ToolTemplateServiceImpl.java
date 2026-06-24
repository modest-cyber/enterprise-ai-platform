package com.aiplatform.ai.service.impl;

import java.util.List;
import com.aiplatform.ai.domain.AiToolTemplate;
import com.aiplatform.ai.mapper.AiToolTemplateMapper;
import com.aiplatform.ai.service.IToolTemplateService;
import com.aiplatform.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工具模板服务实现
 *
 * @author aiplatform
 */
@Service
public class ToolTemplateServiceImpl implements IToolTemplateService {

    @Autowired
    private AiToolTemplateMapper templateMapper;

    @Override
    public AiToolTemplate selectTemplateByCode(String templateCode) {
        AiToolTemplate template = templateMapper.selectTemplateByCode(templateCode);
        if (template == null) {
            throw new ServiceException("工具模板不存在: " + templateCode);
        }
        return template;
    }

    @Override
    public List<AiToolTemplate> selectEnabledTemplates() {
        return templateMapper.selectEnabledTemplates();
    }
}
