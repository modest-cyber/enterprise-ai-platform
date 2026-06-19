package com.aiplatform.ai.service.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.aiplatform.ai.domain.KbDocument;
import com.aiplatform.ai.domain.KbKnowledge;
import com.aiplatform.ai.domain.dto.KnowledgeUploadDto;
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

    private static final Set<String> ALLOWED_TYPES = Set.of("txt", "pdf", "md", "docx", "xlsx", "html");

    private String getUploadBaseDir() {
        return RuoYiConfig.getProfile() + "/ai/kb/";
    }

    @Autowired
    private KbDocumentMapper documentMapper;

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
            String uploadDir = getUploadBaseDir() + kbId + "/";
            Files.createDirectories(Paths.get(uploadDir));
            String savedPath = uploadDir + System.currentTimeMillis() + "_" + originalFilename;
            file.transferTo(new File(savedPath));

            String contentText = extractText(fileType, savedPath);

            KbKnowledge knowledge = knowledgeMapper.selectKnowledgeById(kbId);
            int chunkSize = dto.getChunkSize() != null ? dto.getChunkSize()
                    : (knowledge != null && knowledge.getChunkSize() != null ? knowledge.getChunkSize() : 512);
            int chunkOverlap = dto.getChunkOverlap() != null ? dto.getChunkOverlap()
                    : (knowledge != null && knowledge.getChunkOverlap() != null ? knowledge.getChunkOverlap() : 50);

            KbDocument doc = new KbDocument();
            doc.setKbId(kbId);
            doc.setFileName(originalFilename);
            doc.setFileType(fileType);
            doc.setFileSize(file.getSize());
            doc.setFilePath(savedPath);
            doc.setContentText(contentText);
            doc.setStatus(0);
            doc.setIsDelete(0);
            doc.setCreateTime(DateUtils.getNowDate());
            documentMapper.insertDocument(doc);

            List<String> chunks = chunkText(contentText, chunkSize, chunkOverlap);
            doc.setChunkCount(chunks.size());
            doc.setStatus(1);
            doc.setUpdateTime(DateUtils.getNowDate());
            documentMapper.updateDocument(doc);

            log.info("待向量化: docId={}, chunkCount={}", doc.getDocId(), chunks.size());

            doc.setStatus(2);
            doc.setUpdateTime(DateUtils.getNowDate());
            documentMapper.updateDocument(doc);

            if (knowledge != null) {
                knowledge.setDocCount((knowledge.getDocCount() == null ? 0 : knowledge.getDocCount()) + 1);
                knowledge.setUpdateTime(DateUtils.getNowDate());
                knowledgeMapper.updateKnowledge(knowledge);
            }

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
            case "txt", "md" -> Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
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
}