package com.aiplatform.web.controller.ai;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.dto.KnowledgeUploadDto;
import com.aiplatform.ai.domain.dto.SearchRequestDto;
import com.aiplatform.ai.service.IDocumentService;
import com.aiplatform.ai.service.IKnowledgeService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.page.TableDataInfo;
import com.aiplatform.common.enums.BusinessType;
import com.aiplatform.common.utils.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai/kb")
public class KnowledgeController extends BaseController {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private IDocumentService documentService;

    @PreAuthorize("@ss.hasPermi('ai:kb:list')")
    @GetMapping("/list")
    public TableDataInfo list(KbKnowledge knowledge) {
        startPage();
        List<KbKnowledge> list = knowledgeService.selectKnowledgeList(knowledge);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(knowledgeService.selectKnowledgeById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:add')")
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody KbKnowledge knowledge) {
        knowledge.setCreateBy(SecurityUtils.getUsername());
        return toAjax(knowledgeService.insertKnowledge(knowledge));
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:edit')")
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody KbKnowledge knowledge) {
        knowledge.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(knowledgeService.updateKnowledge(knowledge));
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:remove')")
    @Log(title = "知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(knowledgeService.deleteKnowledgeByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:upload')")
    @Log(title = "文档上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("kbId") Long kbId,
                             @RequestParam(required = false) Integer chunkSize,
                             @RequestParam(required = false) Integer chunkOverlap) {
        KnowledgeUploadDto dto = new KnowledgeUploadDto();
        dto.setKbId(kbId);
        dto.setFile(file);
        dto.setChunkSize(chunkSize);
        dto.setChunkOverlap(chunkOverlap);
        KbDocument doc = documentService.uploadDocument(dto);
        return success(doc);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:search')")
    @PostMapping("/search")
    public AjaxResult search(@Valid @RequestBody SearchRequestDto dto) {
        List<Map<String, Object>> results = knowledgeService.search(dto);
        return AjaxResult.success(results);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/{id}/doc/list")
    public AjaxResult docList(@PathVariable Long id) {
        List<KbDocument> docs = knowledgeService.selectDocList(id);
        return AjaxResult.success(docs);
    }

    @PreAuthorize("@ss.hasPermi('ai:kb:remove')")
    @Log(title = "文档删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/doc/{docId}")
    public AjaxResult deleteDoc(@PathVariable Long docId) {
        return toAjax(documentService.deleteDocs(new Long[]{docId}));
    }
}