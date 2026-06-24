package com.aiplatform.ai.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeProcessClient {

    @Resource
    private FastApiClient fastApiClient;
    @Resource
    private ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeProcessClient(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    public static class ProcessResult {
        public final boolean success;
        public final int chunkCount;
        public final int vectorCount;
        public final String message;

        ProcessResult(boolean success, int chunkCount, int vectorCount, String message) {
            this.success = success;
            this.chunkCount = chunkCount;
            this.vectorCount = vectorCount;
            this.message = message;
        }
    }

    @lombok.Data
    public static class PreviewResult {
        private final boolean success;
        private final String fileType;
        private final String fileName;
        private final String content;
        private final Map<String, Object> metadata;

        PreviewResult(boolean success, String fileType, String fileName, String content, Map<String, Object> metadata) {
            this.success = success;
            this.fileType = fileType;
            this.fileName = fileName;
            this.content = content;
            this.metadata = metadata;
        }
    }

    public PreviewResult preview(long documentId, String filePath) {
        log.info("[Preview] 调用 Python 预览: docId={}, path={}", documentId, filePath);

        Map<String, Object> body = new HashMap<>();
        body.put("documentId", documentId);
        body.put("filePath", filePath);

        try {
            String rawJson = fastApiClient.post("/preview", body);
            log.info("[Preview] Python 返回: {}", rawJson);

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            boolean success = Boolean.TRUE.equals(map.get("success"));
            String fileType = map.get("fileType") != null ? map.get("fileType").toString() : "";
            String fileName = map.get("fileName") != null ? map.get("fileName").toString() : "";
            String content = map.get("content") != null ? map.get("content").toString() : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = map.get("metadata") != null ? (Map<String, Object>) map.get("metadata") : Map.of();

            return new PreviewResult(success, fileType, fileName, content, metadata);

        } catch (Exception e) {
            log.error("[Preview] Python 调用失败", e);
            return new PreviewResult(false, "", "", "Python 服务不可用: " + e.getMessage(), Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    public ProcessResult process(long documentId, String filePath, long knowledgeBaseId, int chunkSize, int chunkOverlap) {
        log.info("[RAG] 调用 Python 处理: docId={}, kbId={}, path={}, chunkSize={}, chunkOverlap={}",
                documentId, knowledgeBaseId, filePath, chunkSize, chunkOverlap);

        Map<String, Object> body = new HashMap<>();
        body.put("documentId", documentId);
        body.put("filePath", filePath);
        body.put("knowledgeBaseId", knowledgeBaseId);
        body.put("chunkSize", chunkSize);
        body.put("chunkOverlap", chunkOverlap);

        try {
            String rawJson = fastApiClient.post("/knowledge/process", body);
            log.info("[RAG] Python 返回: {}", rawJson);

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(rawJson, Map.class);
            boolean success = Boolean.TRUE.equals(map.get("success"));
            int chunkCount = map.get("chunkCount") != null ? ((Number) map.get("chunkCount")).intValue() : 0;
            int vectorCount = map.get("vectorCount") != null ? ((Number) map.get("vectorCount")).intValue() : 0;
            String message = map.get("message") != null ? map.get("message").toString() : "";

            return new ProcessResult(success, chunkCount, vectorCount, message);

        } catch (Exception e) {
            log.error("[RAG] Python 调用失败", e);
            return new ProcessResult(false, 0, 0,
                    e.getMessage() != null ? e.getMessage() : "Python 服务不可用");
        }
    }
}