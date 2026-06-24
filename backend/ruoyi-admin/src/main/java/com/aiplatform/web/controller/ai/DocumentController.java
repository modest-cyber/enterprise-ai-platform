package com.aiplatform.web.controller.ai;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.aiplatform.ai.client.KnowledgeProcessClient;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.vo.DocumentProcessVo;
import com.aiplatform.ai.service.IDocumentService;
import com.aiplatform.common.annotation.Log;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.enums.BusinessType;
import com.aiplatform.common.exception.ServiceException;


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

    @Autowired
    private KnowledgeProcessClient knowledgeProcessClient;

    /** 统一文档预览 — 调用 Python 服务获取解析后的内容和元数据 */
    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @PostMapping("/preview/{documentId}")
    public AjaxResult preview(@PathVariable Long documentId) {
        KbDocument doc = documentService.getDocument(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);

        String filePath = documentService.getDocumentFilePath(documentId);
        KnowledgeProcessClient.PreviewResult result = knowledgeProcessClient.preview(documentId, filePath);

        return success(result);
    }

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

    /** 下载/预览文档原始文件 */
    @PreAuthorize("@ss.hasPermi('ai:kb:query')")
    @GetMapping("/{documentId}/file")
    public void downloadFile(@PathVariable Long documentId, HttpServletResponse response) throws IOException {
        KbDocument doc = documentService.getDocument(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);

        String filePath = documentService.getDocumentFilePath(documentId);
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) throw new ServiceException("文件不存在: " + doc.getFileName());

        // 设置响应头
        String encodedFileName = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
