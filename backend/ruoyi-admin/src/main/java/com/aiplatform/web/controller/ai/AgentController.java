package com.aiplatform.web.controller.ai;

import java.util.List;

import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.domain.dto.AgentExecuteDto;
import com.aiplatform.ai.service.IAgentService;
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
 * Agent 配置管理
 * <p>
 * Agent 是 AI 任务的执行单元，定义了 System Prompt 模板、绑定的模型和工具集。
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/agent")
public class AgentController extends BaseController {

    @Autowired
    private IAgentService agentService;

    /**
     * 获取 Agent 列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:query')")
    @GetMapping("/list")
    public TableDataInfo list(AgentConfig agent) {
        startPage();
        List<AgentConfig> list = agentService.selectAgentList(agent);
        return getDataTable(list);
    }

    /**
     * 根据 Agent ID 获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:query')")
    @GetMapping("/{agentId}")
    public AjaxResult getInfo(@PathVariable Long agentId) {
        return success(agentService.selectAgentById(agentId));
    }

    /**
     * 新增 Agent
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:add')")
    @Log(title = "Agent管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AgentConfig agent) {
        agent.setCreateBy(getUsername());
        return toAjax(agentService.insertAgent(agent));
    }

    /**
     * 修改 Agent
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:edit')")
    @Log(title = "Agent管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AgentConfig agent) {
        agent.setUpdateBy(getUsername());
        return toAjax(agentService.updateAgent(agent));
    }

    /**
     * 删除 Agent
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:remove')")
    @Log(title = "Agent管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{agentIds}")
    public AjaxResult remove(@PathVariable Long[] agentIds) {
        agentService.deleteAgentByIds(agentIds);
        return success();
    }

    /**
     * 同步执行 Agent 任务
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:execute')")
    @Log(title = "Agent执行", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public AjaxResult execute(@Validated @RequestBody AgentExecuteDto dto) {
        String result = agentService.executeSync(dto.getAgentId(), dto.getTask(), dto.getInput());
        return success(result);
    }

    /**
     * 异步提交 Agent 任务
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:execute')")
    @Log(title = "Agent任务提交", businessType = BusinessType.OTHER)
    @PostMapping("/submit")
    public AjaxResult submit(@Validated @RequestBody AgentExecuteDto dto) {
        String taskId = agentService.submitTask(dto.getAgentId(), dto.getTask(), dto.getInput());
        return success(taskId);
    }

    /**
     * 查询异步任务状态
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:query')")
    @GetMapping("/status/{taskId}")
    public AjaxResult status(@PathVariable String taskId) {
        String status = agentService.getTaskStatus(taskId);
        return success(status);
    }

    /**
     * 取消异步任务
     */
    @PreAuthorize("@ss.hasPermi('ai:agent:execute')")
    @Log(title = "Agent任务取消", businessType = BusinessType.OTHER)
    @PostMapping("/cancel/{taskId}")
    public AjaxResult cancel(@PathVariable String taskId) {
        boolean cancelled = agentService.cancelTask(taskId);
        return cancelled ? success("任务已取消") : error("取消任务失败");
    }
}
