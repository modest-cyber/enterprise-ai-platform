package com.aiplatform.web.controller.ai;

import java.util.List;
import com.aiplatform.ai.domain.AiToolTemplate;
import com.aiplatform.ai.service.IToolTemplateService;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工具模板管理
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/tool/template")
public class ToolTemplateController extends BaseController {

    @Autowired
    private IToolTemplateService templateService;

    /**
     * 获取所有启用的工具模板列表（创建工具时选择）
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @GetMapping("/list")
    public AjaxResult list() {
        List<AiToolTemplate> list = templateService.selectEnabledTemplates();
        return success(list);
    }

    /**
     * 根据模板标识获取模板详情（含完整 JSON Schema）
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @GetMapping("/{templateCode}")
    public AjaxResult getByCode(@PathVariable String templateCode) {
        AiToolTemplate template = templateService.selectTemplateByCode(templateCode);
        return success(template);
    }
}
