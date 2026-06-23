package com.aiplatform.ai.client;

import java.util.HashMap;
import java.util.Map;

import com.aiplatform.common.core.domain.AjaxResult;
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

    public ProcessResult process(long documentId, String filePath, long knowledgeBaseId) {
        log.info("[RAG] 调用 Python 处理: docId={}, kbId={}, path={}", documentId, knowledgeBaseId, filePath);

        Map<String, Object> body = new HashMap<>();
        body.put("documentId", documentId);
        body.put("filePath", filePath);
        body.put("knowledgeBaseId", knowledgeBaseId);

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