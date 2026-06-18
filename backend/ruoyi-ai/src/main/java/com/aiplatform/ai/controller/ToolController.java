package com.aiplatform.ai.controller;

import java.util.List;

import com.aiplatform.ai.domain.AiTool;
import com.aiplatform.ai.service.IToolService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.page.TableDataInfo;
import com.aiplatform.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 工具管理
 * <p>
 * 管理通过 MCP（Model Context Protocol）协议注册的外部工具。
 * 工具注册后可由 Agent 在执行任务时自动发现并调用。
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/tool")
public class ToolController extends BaseController {

    @Autowired
    private IToolService toolService;

    /**
     * 获取工具列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @GetMapping("/list")
    public TableDataInfo list(AiTool tool) {
        startPage();
        List<AiTool> list = toolService.selectToolList(tool);
        return getDataTable(list);
    }

    /**
     * 根据工具 ID 获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @GetMapping("/{toolId}")
    public AjaxResult getInfo(@PathVariable Long toolId) {
        return success(toolService.selectToolById(toolId));
    }

    /**
     * 新增工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:add')")
    @Log(title = "工具管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiTool tool) {
        tool.setCreateBy(getUsername());
        return toAjax(toolService.insertTool(tool));
    }

    /**
     * 修改工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:edit')")
    @Log(title = "工具管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiTool tool) {
        tool.setUpdateBy(getUsername());
        return toAjax(toolService.updateTool(tool));
    }

    /**
     * 删除工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:remove')")
    @Log(title = "工具管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{toolIds}")
    public AjaxResult remove(@PathVariable Long[] toolIds) {
        toolService.deleteToolByIds(toolIds);
        return success();
    }

    /**
     * 调用工具执行
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:execute')")
    @Log(title = "工具调用", businessType = BusinessType.OTHER)
    @PostMapping("/invoke/{toolId}")
    public AjaxResult invoke(@PathVariable Long toolId, @RequestBody(required = false) String params) {
        String result = toolService.invokeTool(toolId, params);
        return success(result);
    }

    /**
     * 测试工具连通性
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @Log(title = "工具连通性测试", businessType = BusinessType.OTHER)
    @PostMapping("/test/{toolId}")
    public AjaxResult testConnection(@PathVariable Long toolId) {
        boolean connected = toolService.testConnection(toolId);
        return connected ? success("工具连通性测试通过") : error("工具连通性测试失败");
    }
}
