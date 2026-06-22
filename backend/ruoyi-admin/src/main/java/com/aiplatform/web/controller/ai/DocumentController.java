package com.aiplatform.web.controller.ai;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.aiplatform.ai.domain.vo.DocumentProcessVo;
import com.aiplatform.ai.service.IDocumentService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.enums.BusinessType;

/**
 * 文档处理控制器
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/ai/document")
public class DocumentController extends BaseController {

    @Autowired
    private IDocumentService documentService;

    /** 触发文档向量化处理 */
    @PreAuthorize("@ss.hasPermi('ai:kb:upload')")
    @Log(title = "文档处理", businessType = BusinessType.UPDATE)
    @PostMapping("/process/{documentId}")
    public AjaxResult process(@PathVariable Long documentId) {
        documentService.processDocument(documentId);
        return success("处理任务已提交");
    }

    /** 查询文档处理状态 */
    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/process/{documentId}")
    public AjaxResult getProcessStatus(@PathVariable Long documentId) {
        DocumentProcessVo vo = documentService.getProcessStatus(documentId);
        return success(vo);
    }

    /** 获取文档内容（预览） */
    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/{documentId}/content")
    public AjaxResult getContent(@PathVariable Long documentId) {
        String content = documentService.getDocumentContent(documentId);
        return success(content);
    }

    /** 知识库文档统计 */
    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/stats/{kbId}")
    public AjaxResult getDocStats(@PathVariable Long kbId) {
        Map<String, Object> stats = documentService.getDocStats(kbId);
        return success(stats);
    }
}
