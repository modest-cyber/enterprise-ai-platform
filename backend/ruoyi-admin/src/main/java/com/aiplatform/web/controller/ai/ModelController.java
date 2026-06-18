package com.aiplatform.web.controller.ai;

import java.util.List;

import com.aiplatform.ai.domain.AiModel;
import com.aiplatform.ai.domain.dto.ModelConfigDto;
import com.aiplatform.ai.service.IModelConfigService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.page.TableDataInfo;
import com.aiplatform.common.enums.BusinessType;
import org.springframework.beans.BeanUtils;
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
 * 模型配置管理
 * <p>
 * 管理所有 LLM 模型配置，支持多 Provider（OpenAI/DeepSeek/Qwen/Ollama）和
 * 多模型类型（chat/embedding/rerank）。通过 is_default 字段标识默认模型。
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/model")
public class ModelController extends BaseController {

    @Autowired
    private IModelConfigService modelConfigService;

    /**
     * 获取模型列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping("/list")
    public TableDataInfo list(AiModel model) {
        startPage();
        List<AiModel> list = modelConfigService.selectModelList(model);
        return getDataTable(list);
    }

    /**
     * 根据模型 ID 获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping("/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId) {
        return success(modelConfigService.selectModelById(modelId));
    }

    /**
     * 新增模型配置
     */
    @PreAuthorize("@ss.hasPermi('ai:model:add')")
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ModelConfigDto dto) {
        AiModel model = new AiModel();
        BeanUtils.copyProperties(dto, model);
        model.setCreateBy(getUsername());
        return toAjax(modelConfigService.insertModel(model));
    }

    /**
     * 修改模型配置
     */
    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ModelConfigDto dto) {
        AiModel model = new AiModel();
        BeanUtils.copyProperties(dto, model);
        model.setUpdateBy(getUsername());
        return toAjax(modelConfigService.updateModel(model));
    }

    /**
     * 删除模型配置
     */
    @PreAuthorize("@ss.hasPermi('ai:model:remove')")
    @Log(title = "模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds) {
        modelConfigService.deleteModelByIds(modelIds);
        return success();
    }

    /**
     * 测试模型连通性
     */
    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @Log(title = "模型连通性测试", businessType = BusinessType.OTHER)
    @PostMapping("/test/{modelId}")
    public AjaxResult testConnection(@PathVariable Long modelId) {
        boolean connected = modelConfigService.testConnection(modelId);
        return connected ? success("模型连通性测试通过") : error("模型连通性测试失败");
    }

    /**
     * 设置默认模型
     */
    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PutMapping("/set-default/{modelId}")
    public AjaxResult setDefault(@PathVariable Long modelId) {
        modelConfigService.setDefaultModel(modelId);
        return success("默认模型设置成功");
    }
}
