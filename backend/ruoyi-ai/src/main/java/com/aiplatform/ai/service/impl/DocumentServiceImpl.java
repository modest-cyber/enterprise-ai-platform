package com.aiplatform.ai.service.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.aiplatform.ai.client.KnowledgeProcessClient;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.dto.KnowledgeUploadDto;
import com.aiplatform.ai.domain.vo.DocumentProcessVo;
import com.aiplatform.ai.mapper.KbDocumentMapper;
import com.aiplatform.ai.mapper.KbKnowledgeMapper;
import com.aiplatform.ai.service.IDocumentService;
import com.aiplatform.common.config.RuoYiConfig;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.Tika;

@Slf4j
@Service
public class DocumentServiceImpl implements IDocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of("txt", "pdf", "md", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "html", "csv");

    /** 服务器本地存储根目录 */
    private static final String KB_UPLOAD_ROOT = "upload/ai/kb/";

    private String getUploadBaseDir() {
        // 使用应用工作目录 + upload/ai/kb/，文件存储在服务器本地
        return System.getProperty("user.dir") + "/" + KB_UPLOAD_ROOT;
    }

    /** 解析文件完整路径（兼容旧绝对路径和新相对路径） */
    private String resolveFullPath(String filePath) {
        if (filePath == null) return null;
        // 已是绝对路径（如 F:/... 或 /home/...），直接返回
        if (Paths.get(filePath).isAbsolute() || filePath.matches("^[A-Za-z]:[/\\\\].*")) {
            return filePath;
        }
        // 相对路径，拼接工作目录前缀
        return System.getProperty("user.dir") + "/" + filePath;
    }

    @Autowired
    private KbDocumentMapper documentMapper;

    @Autowired
    private KnowledgeProcessClient knowledgeProcessClient;

    @Autowired
    private KbKnowledgeMapper knowledgeMapper;

    @Override
    public KbDocument uploadDocument(KnowledgeUploadDto dto) {
        MultipartFile file = dto.getFile();
        Long kbId = dto.getKbId();

        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileType(originalFilename);
        if (!ALLOWED_TYPES.contains(fileType)) {
            throw new ServiceException("不支持的文件类型: " + fileType + "，支持: txt, pdf, md, docx, xlsx, html");
        }

        try {
            // 生成日期子目录和UUID文件名
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
            String newFileName = uuid + ext;
            String uploadDir = getUploadBaseDir() + kbId + "/" + dateDir + "/";
            Files.createDirectories(Paths.get(uploadDir));
            String savedPath = uploadDir + newFileName;
            file.transferTo(new File(savedPath));

            // 存储相对路径（相对于user.dir）
            String relativePath = KB_UPLOAD_ROOT + kbId + "/" + dateDir + "/" + newFileName;

            String contentText = extractText(fileType, savedPath);

            KbKnowledge knowledge = knowledgeMapper.selectKnowledgeById(kbId);

            KbDocument doc = new KbDocument();
            doc.setKbId(kbId);
            doc.setFileName(originalFilename);
            doc.setFileType(fileType);
            doc.setFileSize(file.getSize());
            doc.setFilePath(relativePath);
            doc.setContentText(contentText);
            doc.setStatus(0);
            doc.setProcessStatus("PENDING");
            doc.setProcessProgress(0);
            doc.setIsDelete(0);
            doc.setCreateTime(DateUtils.getNowDate());
            documentMapper.insertDocument(doc);

            // 更新知识库文档计数
            if (knowledge != null) {
                knowledge.setDocCount((knowledge.getDocCount() == null ? 0 : knowledge.getDocCount()) + 1);
                knowledge.setUpdateTime(DateUtils.getNowDate());
                knowledgeMapper.updateKnowledge(knowledge);
            }

            log.info("文档上传完成: docId={}, path={}, processStatus=PENDING", doc.getDocId(), relativePath);
            return doc;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档上传处理失败", e);
            throw new ServiceException("文档处理失败: " + e.getMessage());
        }
    }

    private String getFileType(String filename) {
        if (filename == null) return "txt";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "txt" : filename.substring(dot + 1).toLowerCase();
    }

    private String extractText(String fileType, String filePath) throws IOException, TikaException {
        return switch (fileType) {
            case "txt", "md", "csv" -> {
                try {
                    yield Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
                } catch (java.nio.charset.MalformedInputException e) {
                    yield new String(Files.readAllBytes(Paths.get(filePath)), "GBK");
                }
            }
            case "pdf" -> extractPdf(filePath);
            case "docx" -> extractDocx(filePath);
            default -> new Tika().parseToString(new File(filePath));
        };
    }

    private String extractPdf(String filePath) throws IOException {
        try (var doc = Loader.loadPDF(new File(filePath))) {
            var stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractDocx(String filePath) throws IOException {
        try (var fis = new FileInputStream(filePath);
             var doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    public List<String> chunkText(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk.setLength(0);
            }
            if (currentChunk.length() > 0) currentChunk.append("\n\n");
            currentChunk.append(trimmed);

            while (currentChunk.length() > chunkSize) {
                int splitAt = chunkSize;
                for (int i = chunkSize - chunkOverlap; i < currentChunk.length(); i++) {
                    char c = currentChunk.charAt(i);
                    if (c == '.' || c == '。' || c == '！' || c == '？' || c == '\n') {
                        splitAt = i + 1;
                        break;
                    }
                }
                if (splitAt >= currentChunk.length()) break;
                chunks.add(currentChunk.substring(0, splitAt).trim());
                int newStart = Math.max(0, splitAt - chunkOverlap);
                String remain = currentChunk.substring(newStart);
                currentChunk.setLength(0);
                currentChunk.append(remain);
            }
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    @Override
    public int deleteDocs(Long[] docIds) {
        return documentMapper.deleteDocumentByIds(docIds);
    }

    @Override
    public int reindex(Long docId, Integer chunkSize, Integer chunkOverlap) {
        KbDocument doc = documentMapper.selectDocumentById(docId);
        if (doc == null) throw new ServiceException("文档不存在");

        List<String> chunks = chunkText(doc.getContentText(),
                chunkSize != null ? chunkSize : 512,
                chunkOverlap != null ? chunkOverlap : 50);

        doc.setChunkCount(chunks.size());
        doc.setStatus(1);
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);

        log.info("待向量化(重索引): docId={}, chunkCount={}", docId, chunks.size());

        doc.setStatus(2);
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);

        return chunks.size();
    }

    @Override
    public void updateDocStatus(Long docId, int status, String errorMsg) {
        KbDocument doc = new KbDocument();
        doc.setDocId(docId);
        doc.setStatus(status);
        doc.setErrorMsg(errorMsg);
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);
    }

    @Override
    public void processDocument(Long documentId) {
        KbDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);

        // 更新状态为处理中
        doc.setProcessStatus("PROCESSING");
        doc.setProcessProgress(0);
        doc.setProcessMessage("开始处理...");
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);

        try {
            String filePath = resolveFullPath(doc.getFilePath());
            log.info("[RAG] 调用 Python 处理: docId={}, kbId={}, path={}", documentId, doc.getKbId(), filePath);

            if (doc.getContentText() == null || doc.getContentText().isEmpty()) {
                doc.setContentText(extractText(doc.getFileType(), filePath));
            }

            KnowledgeProcessClient.ProcessResult result = knowledgeProcessClient.process(
                    documentId, filePath, doc.getKbId());

            if (result.success) {
                doc.setChunkCount(result.chunkCount);
                doc.setVectorCount(result.vectorCount);
                doc.setProcessStatus("SUCCESS");
                doc.setProcessProgress(100);
                doc.setProcessMessage("处理完成");
                doc.setProcessedTime(DateUtils.getNowDate());
                doc.setUpdateTime(DateUtils.getNowDate());
                documentMapper.updateDocument(doc);
                log.info("[RAG] 文档处理完成: docId={}, chunkCount={}, vectorCount={}",
                        documentId, result.chunkCount, result.vectorCount);
            } else {
                doc.setProcessStatus("FAILED");
                doc.setProcessProgress(0);
                doc.setProcessMessage(result.message);
                doc.setUpdateTime(DateUtils.getNowDate());
                documentMapper.updateDocument(doc);
                log.error("[RAG] 文档处理失败: docId={}, message={}", documentId, result.message);
            }

        } catch (Exception e) {
            log.error("[RAG] 文档处理异常: docId={}", documentId, e);
            doc.setProcessStatus("FAILED");
            doc.setProcessProgress(0);
            doc.setProcessMessage("Python 服务不可用: " + e.getMessage());
            doc.setUpdateTime(DateUtils.getNowDate());
            documentMapper.updateDocument(doc);
        }
    }

    private void updateProgress(Long docId, int progress, String message) {
        KbDocument doc = new KbDocument();
        doc.setDocId(docId);
        doc.setProcessProgress(progress);
        doc.setProcessMessage(message);
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);
    }

    @Override
    public DocumentProcessVo getProcessStatus(Long documentId) {
        KbDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);

        DocumentProcessVo vo = new DocumentProcessVo();
        vo.setDocId(doc.getDocId());
        vo.setStatus(doc.getProcessStatus() != null ? doc.getProcessStatus() : "PENDING");
        vo.setProgress(doc.getProcessProgress() != null ? doc.getProcessProgress() : 0);
        vo.setMessage(doc.getProcessMessage());
        vo.setChunkCount(doc.getChunkCount());
        vo.setVectorCount(doc.getVectorCount());
        return vo;
    }

    @Override
    public String getDocumentContent(Long documentId) {
        KbDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);
        if (doc.getContentText() != null && !doc.getContentText().isEmpty()) {
            return doc.getContentText();
        }
        // 未提取内容时重新读取
        try {
            return extractText(doc.getFileType(), resolveFullPath(doc.getFilePath()));
        } catch (Exception e) {
            log.error("读取文档内容失败: docId={}", documentId, e);
            throw new ServiceException("读取文档内容失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDocStats(Long kbId) {
        List<Map<String, Object>> rows = documentMapper.countDocsByStatus(kbId);
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = 0, pending = 0, processing = 0, success = 0, failed = 0;
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            long count = ((Number) row.get("count")).longValue();
            total += count;
            switch (status != null ? status : "PENDING") {
                case "PENDING" -> pending = count;
                case "PROCESSING" -> processing = count;
                case "SUCCESS" -> success = count;
                case "FAILED" -> failed = count;
            }
        }
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("processing", processing);
        stats.put("success", success);
        stats.put("failed", failed);
        return stats;
    }

    @Override
    public KbDocument getDocument(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public String getDocumentFilePath(Long documentId) {
        KbDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) throw new ServiceException("文档不存在: " + documentId);
        return resolveFullPath(doc.getFilePath());
    }
}