# M6 知识库详情页重构 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 连接 Java ↔ Python 文档处理流水线，新增 DOCX 预览，补全数据库 schema，让知识库详情页具备完整的文档查看/处理/状态追踪能力。

**Architecture:** Java 负责文件读取+文本提取+进度管理，通过 FastApiClient 调用 Python 的 `/api/v1/knowledge/index` 完成切块+Embedding+Milvus 写入。前端轮询 Java 状态接口实现实时进度展示。

**Tech Stack:** Java 8+ / Spring Boot / MyBatis / Apache POI / Vue3 + Element Plus / FastAPI / Milvus Lite / pymilvus

## Global Constraints

- 严格遵循 RuoYi 项目分层架构：Controller 在 `ruoyi-admin`，DTO/VO 在对应模块 `domain` 包
- Controller 非空校验用 DTO + @Valid，Service 不做基础入参校验
- 使用现有 AjaxResult / 权限注解 / 字典体系
- 不允许新增与项目重复的基础设施
- 所有代码必须可编译运行，不允许伪代码和 TODO

---

### Task 1: 数据库 schema 补全

**Files:**
- Modify: `backend/sql/ai_init.sql:86-99`（kb_document 建表语句）

**Interfaces:**
- Consumes: nothing
- Produces: `kb_document` 表含 `vector_count`, `process_status`, `process_progress`, `process_message`, `processed_time` 列

- [ ] **Step 1: 更新 ai_init.sql 建表语句**

在 `kb_document` 表的 `chunk_count` 行之后，`vector_ids` 行之前，新增字段：

```sql
  vector_count      int(11)         default 0                  comment '向量数',
```

在 `error_msg` 行之前新增处理状态字段：

```sql
  process_status    varchar(20)     default 'PENDING'          comment '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
  process_progress  int(11)         default 0                  comment '处理进度 0-100',
  process_message   varchar(500)    default ''                 comment '当前处理步骤描述',
  processed_time    datetime        default null               comment '处理完成时间',
```

完整建表语句需包含这些字段（参考 Entity 和 Mapper XML 中已引用的列）。

- [ ] **Step 2: 编写 ALTER TABLE 迁移脚本**

```sql
-- 对已有数据库执行
ALTER TABLE kb_document
ADD COLUMN vector_count INT DEFAULT 0 COMMENT '向量数',
ADD COLUMN process_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态',
ADD COLUMN process_progress INT DEFAULT 0 COMMENT '处理进度 0-100',
ADD COLUMN process_message VARCHAR(500) DEFAULT '' COMMENT '当前处理步骤描述',
ADD COLUMN processed_time DATETIME DEFAULT NULL COMMENT '处理完成时间';
```

写为文件 `backend/sql/migration_add_kb_document_process_fields.sql`。

- [ ] **Step 3: Commit**

```bash
git add backend/sql/ai_init.sql backend/sql/migration_add_kb_document_process_fields.sql
git commit -m "feat: kb_document 表新增 vector_count/process_status/progress 字段"
```

---

### Task 2: Python — 添加 pymilvus 依赖 + IndexRequest 扩展

**Files:**
- Modify: `agent-service/requirements.txt`（新增一行）
- Modify: `agent-service/app/rag/schemas.py:6-9`（IndexRequest 新增可选字段）

**Interfaces:**
- Consumes: nothing
- Produces: `IndexRequest.file_path: str = ""`（可选字段，方便日志排查）

- [ ] **Step 1: requirements.txt 新增 pymilvus**

编辑 `agent-service/requirements.txt`，在 `# RAG` section 下新增：

```
pymilvus>=2.4.0
```

- [ ] **Step 2: schemas.py 扩展 IndexRequest**

```python
class IndexRequest(BaseModel):
    doc_id: int = Field(..., description="文档ID")
    kb_id: int = Field(..., description="知识库ID")
    content: str = Field(..., description="文档全文内容", min_length=1)
    file_path: str = Field(default="", description="文件路径（可选，用于日志）")
```

改动点：`file_path` 行新增。

- [ ] **Step 3: 验证 Python 服务可启动**

```bash
cd agent-service
pip install pymilvus>=2.4.0
python -c "from app.rag.schemas import IndexRequest; print('OK')"
```

- [ ] **Step 4: Commit**

```bash
git add agent-service/requirements.txt agent-service/app/rag/schemas.py
git commit -m "feat: Python 侧添加 pymilvus 依赖 + IndexRequest 扩展 file_path 字段"
```

---

### Task 3: Java — FastApiClient 新增 indexDocument 方法

**Files:**
- Modify: `backend/ruoyi-ai/src/main/java/com/aiplatform/ai/client/FastApiClient.java`（新增方法）

**Interfaces:**
- Produces: `String indexDocument(Map<String, Object> request)` → 返回 Python JSON 响应字符串

- [ ] **Step 1: 在 FastApiClient 中新增 indexDocument 方法**

在 `streamPost` 方法之后新增：

```java
/**
 * 调用知识库索引接口
 * @param request 包含 docId, kbId, content, filePath 的 Map
 * @return Python 返回的 JSON 字符串
 */
public String indexDocument(Map<String, Object> request) {
    return post("/knowledge/index", request);
}
```

注意：此方法复用已有的 `post(String path, Map<String, Object> body)` 方法。`path` 为 `/knowledge/index`，base URL 已拼好 `/api/v1` 前缀，完整路径为 `/api/v1/knowledge/index`。

- [ ] **Step 2: Commit**

```bash
git add backend/ruoyi-ai/src/main/java/com/aiplatform/ai/client/FastApiClient.java
git commit -m "feat: FastApiClient 新增 indexDocument 方法"
```

---

### Task 4: Java — IDocumentService 新增 getDocumentPreview 签名

**Files:**
- Modify: `backend/ruoyi-ai/src/main/java/com/aiplatform/ai/service/IDocumentService.java`（新增方法签名）

**Interfaces:**
- Produces: `String getDocumentPreview(Long documentId)` — 返回预览 HTML 字符串（DOCX 转 HTML）或文本内容

- [ ] **Step 1: 在 IDocumentService 接口新增方法**

在 `getDocumentFilePath` 方法签名之后新增：

```java
/** 获取文档预览内容（DOCX 转 HTML，其他类型返回文本） */
String getDocumentPreview(Long documentId);
```

- [ ] **Step 2: Commit**

```bash
git add backend/ruoyi-ai/src/main/java/com/aiplatform/ai/service/IDocumentService.java
git commit -m "feat: IDocumentService 新增 getDocumentPreview 方法签名"
```

---

### Task 5: Java — DocumentServiceImpl 重写 processDocument + 实现 getDocumentPreview

**Files:**
- Modify: `backend/ruoyi-ai/src/main/java/com/aiplatform/ai/service/impl/DocumentServiceImpl.java`（修改 2 个方法）

**Interfaces:**
- Consumes: `FastApiClient.indexDocument(Map)` from Task 3
- Produces: `processDocument(Long)` 完整处理流水线；`getDocumentPreview(Long)` DOCX→HTML

- [ ] **Step 1: 注入 FastApiClient**

在 `DocumentServiceImpl` 类的字段区新增：

```java
@Autowired
private FastApiClient fastApiClient;
```

同时在文件顶部新增 import：

```java
import com.aiplatform.ai.client.FastApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
```

- [ ] **Step 2: 重写 processDocument 方法**

将现有的 `processDocument` 方法（整个方法体，约第 235-282 行）替换为：

```java
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
        // Step 1: 文件读取 (10%)
        updateProgress(documentId, 10, "文件读取完成");
        String filePath = resolveFullPath(doc.getFilePath());
        if (!new File(filePath).exists()) {
            throw new ServiceException("文件不存在: " + filePath);
        }

        // Step 2: 文本解析 (30%)
        String contentText = doc.getContentText();
        if (contentText == null || contentText.isEmpty()) {
            contentText = extractText(doc.getFileType(), filePath);
            doc.setContentText(contentText);
        }
        updateProgress(documentId, 30, "文本解析完成");

        // Step 3-5: 调用Python进行切块+Embedding+Milvus写入 (50%)
        updateProgress(documentId, 50, "向量化处理中...");

        Map<String, Object> indexReq = new LinkedHashMap<>();
        indexReq.put("doc_id", doc.getDocId());
        indexReq.put("kb_id", doc.getKbId());
        indexReq.put("content", contentText);
        indexReq.put("file_path", filePath);

        String responseJson = fastApiClient.indexDocument(indexReq);
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = mapper.readValue(responseJson, Map.class);

        Object successObj = response.get("success");
        boolean success = successObj instanceof Boolean ? (Boolean) successObj
                : ((Number) successObj).intValue() != 0;
        if (!success) {
            throw new ServiceException("Python 索引失败: " + responseJson);
        }

        int chunkCount = response.get("chunk_count") != null
                ? ((Number) response.get("chunk_count")).intValue() : 0;

        // 处理成功
        doc.setChunkCount(chunkCount);
        doc.setVectorCount(chunkCount);
        doc.setProcessStatus("SUCCESS");
        doc.setProcessProgress(100);
        doc.setProcessMessage("处理完成");
        doc.setProcessedTime(DateUtils.getNowDate());
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);
        log.info("文档处理完成: docId={}, chunkCount={}, vectorCount={}",
                documentId, chunkCount, chunkCount);

    } catch (ServiceException e) {
        throw e;
    } catch (Exception e) {
        log.error("文档处理失败: docId={}", documentId, e);
        doc.setProcessStatus("FAILED");
        doc.setProcessProgress(0);
        doc.setProcessMessage("处理失败: " + e.getMessage());
        doc.setUpdateTime(DateUtils.getNowDate());
        documentMapper.updateDocument(doc);
    }
}
```

- [ ] **Step 3: 实现 getDocumentPreview 方法**

在 `getDocumentFilePath` 方法之后新增：

```java
@Override
public String getDocumentPreview(Long documentId) {
    KbDocument doc = documentMapper.selectDocumentById(documentId);
    if (doc == null) throw new ServiceException("文档不存在: " + documentId);

    String fileType = doc.getFileType();
    if (fileType == null) fileType = "txt";

    // 非 DOCX 类型直接返回已提取的文本
    if (!"docx".equalsIgnoreCase(fileType)) {
        if (doc.getContentText() != null && !doc.getContentText().isEmpty()) {
            return doc.getContentText();
        }
        try {
            return extractText(fileType, resolveFullPath(doc.getFilePath()));
        } catch (Exception e) {
            log.error("读取文本失败: docId={}", documentId, e);
            throw new ServiceException("读取文本失败: " + e.getMessage());
        }
    }

    // DOCX 转 HTML
    try {
        String filePath = resolveFullPath(doc.getFilePath());
        return convertDocxToHtml(filePath);
    } catch (Exception e) {
        log.error("DOCX 转 HTML 失败: docId={}", documentId, e);
        throw new ServiceException("DOCX 转换失败: " + e.getMessage());
    }
}

/**
 * 使用 Apache POI 将 DOCX 转换为 HTML
 */
private String convertDocxToHtml(String filePath) throws Exception {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
        .append("<style>")
        .append("body{font-family:'Microsoft YaHei',sans-serif;font-size:14px;line-height:1.8;padding:16px;color:#333;max-width:900px;margin:0 auto;}")
        .append("h1,h2,h3,h4,h5,h6{margin:16px 0 8px;font-weight:bold;color:#1a1a1a;}")
        .append("p{margin:4px 0 12px;}")
        .append("table{border-collapse:collapse;width:100%;margin:12px 0;}")
        .append("td,th{border:1px solid #ddd;padding:8px;}")
        .append("img{max-width:100%;height:auto;}")
        .append("ul,ol{padding-left:24px;margin:8px 0;}")
        .append("li{margin:4px 0;}")
        .append("blockquote{border-left:3px solid #ddd;padding-left:16px;color:#666;margin:12px 0;}")
        .append("pre{background:#f5f5f5;padding:12px;border-radius:4px;overflow-x:auto;}")
        .append("code{background:#f0f0f0;padding:2px 4px;border-radius:2px;font-size:13px;}")
        .append("</style></head><body>");

    try (FileInputStream fis = new FileInputStream(filePath);
         XWPFDocument doc = new XWPFDocument(fis)) {

        for (var element : doc.getBodyElements()) {
            switch (element.getElementType()) {
                case PARAGRAPH -> {
                    var para = (org.apache.poi.xwpf.usermodel.XWPFParagraph) element;
                    String style = para.getStyle();
                    String text = para.getText();

                    if (text == null || text.isBlank()) {
                        html.append("<p>&nbsp;</p>");
                        continue;
                    }

                    String escaped = escapeHtml(text);

                    // 处理段落内的 run 样式
                    StringBuilder paraHtml = new StringBuilder();
                    for (var run : para.getRuns()) {
                        String runText = escapeHtml(run.text());
                        if (runText.isEmpty()) continue;
                        if (run.isBold()) runText = "<strong>" + runText + "</strong>";
                        if (run.isItalic()) runText = "<em>" + runText + "</em>";
                        paraHtml.append(runText);
                    }
                    String inner = paraHtml.length() > 0 ? paraHtml.toString() : escaped;

                    if (style != null) {
                        String lower = style.toLowerCase();
                        if (lower.startsWith("heading")) {
                            int level = 1;
                            try { level = Integer.parseInt(lower.replace("heading", "").trim()); } catch (NumberFormatException ignored) {}
                            if (level < 1) level = 1; if (level > 6) level = 6;
                            html.append("<h").append(level).append(">").append(inner).append("</h").append(level).append(">");
                            continue;
                        }
                    }
                    html.append("<p>").append(inner).append("</p>");
                }
                case TABLE -> {
                    var table = (org.apache.poi.xwpf.usermodel.XWPFTable) element;
                    html.append("<table>");
                    for (var row : table.getRows()) {
                        html.append("<tr>");
                        for (var cell : row.getTableCells()) {
                            String cellText = escapeHtml(cell.getText());
                            html.append("<td>").append(cellText).append("</td>");
                        }
                        html.append("</tr>");
                    }
                    html.append("</table>");
                }
                default -> {}
            }
        }
    }

    html.append("</body></html>");
    return html.toString();
}

private String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
```

需要在文件顶部新增 import：

```java
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
```

（`LinkedHashMap` 已在 `java.util.*` 中，`ObjectMapper` 需显式 import。）

- [ ] **Step 4: 验证编译**

```bash
cd backend
mvn compile -pl ruoyi-ai -am -q
```

预期：BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-ai/src/main/java/com/aiplatform/ai/service/impl/DocumentServiceImpl.java
git commit -m "feat: processDocument 接入 FastAPI + 新增 getDocumentPreview DOCX转HTML"
```

---

### Task 6: Java — DocumentController 新增 preview 端点

**Files:**
- Modify: `backend/ruoyi-admin/src/main/java/com/aiplatform/web/controller/ai/DocumentController.java`（新增 1 个方法）

**Interfaces:**
- Consumes: `IDocumentService.getDocumentPreview(Long)` from Task 4
- Produces: `GET /ai/document/{documentId}/preview` → HTML 内容

- [ ] **Step 1: 在 DocumentController 新增 preview 端点**

在 `downloadFile` 方法之前新增：

```java
/** 获取文档预览内容（DOCX→HTML，其他类型返回文本） */
@PreAuthorize("@ss.hasPermi('ai:kb:query')")
@GetMapping("/{documentId}/preview")
public AjaxResult getPreview(@PathVariable Long documentId) {
    String html = documentService.getDocumentPreview(documentId);
    return success(html);
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend
mvn compile -pl ruoyi-admin -am -q
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/ruoyi-admin/src/main/java/com/aiplatform/web/controller/ai/DocumentController.java
git commit -m "feat: DocumentController 新增 GET /{id}/preview 端点"
```

---

### Task 7: 前端 — knowledge.ts 新增 getDocumentPreview

**Files:**
- Modify: `frontend/src/api/ai/knowledge.ts`（新增 1 个函数）

**Interfaces:**
- Produces: `getDocumentPreview(docId: number)` → `GET /ai/document/{docId}/preview`

- [ ] **Step 1: 在知识库 API 文件中新增函数**

在 `getDocStats` 函数之后、`searchKnowledge` 之前新增：

```typescript
export function getDocumentPreview(docId: number) {
  return request({ url: '/ai/document/' + docId + '/preview', method: 'get' })
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/ai/knowledge.ts
git commit -m "feat: 前端 API 层新增 getDocumentPreview 方法"
```

---

### Task 8: 前端 — kbDetail.vue DOCX 预览 + 处理弹窗优化

**Files:**
- Modify: `frontend/src/views/ai/knowledge/kbDetail.vue`（改动 2 处）

**Interfaces:**
- Consumes: `getDocumentPreview(docId)` from Task 7
- Produces: DOCX 预览通过 `v-html` 渲染；处理弹窗展示 processedTime

- [ ] **Step 1: Import getDocumentPreview**

在 `<script setup>` 的 import 行追加 `getDocumentPreview`：

```typescript
import { getKnowledge, listDocuments, deleteDocument, processDocument, getProcessStatus, getDocumentContent, getDocStats, getDocumentPreview } from '@/api/ai/knowledge'
```

即将 `getDocumentPreview` 追加到已有 import 语句的解构中。

- [ ] **Step 2: 修改 handleView 支持 DOCX 预览**

将 `handleView` 函数（约第 257-279 行）替换为：

```typescript
function handleView(row: any) {
  previewDocId.value = row.docId
  previewFileName.value = row.fileName
  previewLoading.value = true
  previewOpen.value = true

  const ext = (row.fileType || '').toLowerCase()
  if (ext === 'pdf') {
    previewType.value = 'pdf'
    previewLoading.value = false
  } else if (ext === 'docx') {
    // DOCX: 后端返回 HTML
    getDocumentPreview(row.docId).then((res: any) => {
      previewHtml.value = res.data || ''
      previewType.value = 'markdown'
      previewLoading.value = false
    }).catch(() => {
      previewContent.value = 'DOCX 预览加载失败'
      previewType.value = 'text'
      previewLoading.value = false
    })
  } else if (ext === 'md') {
    getDocumentContent(row.docId).then((res: any) => {
      previewHtml.value = (res.data || '').replace(/\n/g, '<br/>')
      previewType.value = 'markdown'
      previewLoading.value = false
    })
  } else {
    getDocumentContent(row.docId).then((res: any) => {
      previewContent.value = res.data || ''
      previewType.value = 'text'
      previewLoading.value = false
    })
  }
}
```

关键改动：新增 `else if (ext === 'docx')` 分支，调用 `getDocumentPreview` 获取 HTML 并通过 `v-html` 渲染（复用已有的 `previewType === 'markdown'` 渲染路径）。

- [ ] **Step 3: 处理弹窗展示 processedTime**

在处理弹窗的模板中，找到 `el-alert` 错误展示区域（约第 121 行），在其下方新增成功时展示耗时：

修改处理弹窗模板中 `el-alert` 前面的结果展示区域（约第 116-121 行），替换为：

```html
<p v-if="processData.chunkCount" style="margin-top: 8px;">
  切块数：{{ processData.chunkCount }} | 向量数：{{ processData.vectorCount }}
</p>
<p v-if="processSuccess && processElapsed" style="color: #67C23A; margin-top: 4px;">
  耗时：{{ processElapsed }}
</p>
<el-alert v-if="processFailed" title="处理失败" type="error" :description="processMessage" show-icon style="margin-top: 12px;" />
<el-alert v-if="processSuccess" title="处理完成" type="success" show-icon style="margin-top: 12px;" />
```

在 script 中新增状态变量（约第 186 行附近）：

```typescript
const processSuccess = ref(false)
const processElapsed = ref('')
const processStartTime = ref<number>(0)
```

修改 `startProcess` 函数（约第 293 行），记录开始时间：

```typescript
function startProcess() {
  if (!processDocId.value) return
  processRunning.value = true
  processFailed.value = false
  processSuccess.value = false
  processElapsed.value = ''
  processStartTime.value = Date.now()
  processDocument(processDocId.value).then(() => {
    startPolling()
  }).catch(() => {
    processFailed.value = true
    processMessage.value = '提交处理失败'
    processRunning.value = false
  })
}
```

修改 `startPolling` 中的 SUCCESS 分支（约第 315 行），计算耗时：

```typescript
if (res.data.status === 'SUCCESS') {
  processProgress.value = 100
  processRunning.value = false
  processSuccess.value = true
  const elapsed = Math.round((Date.now() - processStartTime.value) / 1000)
  processElapsed.value = elapsed >= 60 ? Math.floor(elapsed / 60) + '分' + (elapsed % 60) + '秒' : elapsed + '秒'
  stopPolling()
}
```

- [ ] **Step 4: 验证前端编译**

```bash
cd frontend
pnpm run build --mode production 2>&1 | tail -20
```

或者用 dev server 验证无语法错误：

```bash
cd frontend
npx vue-tsc --noEmit 2>&1 | head -30
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ai/knowledge/kbDetail.vue
git commit -m "feat: kbDetail 新增 DOCX 预览 + 处理耗时展示"
```

---

### Task 9: 集成验证

- [ ] **Step 1: 编译后端全部模块**

```bash
cd backend
mvn compile -q
```

预期：BUILD SUCCESS

- [ ] **Step 2: 确认 Python 依赖安装**

```bash
cd agent-service
pip install -r requirements.txt
python -c "from app.rag.milvus_service import MilvusService; from app.rag.schemas import IndexRequest; print('All imports OK')"
```

- [ ] **Step 3: 确认前端编译**

```bash
cd frontend
pnpm run build --mode production 2>&1 | tail -5
```

预期：无 error

- [ ] **Step 4: 启动服务验证处理流程**

```bash
# 终端 1: 启动 Python 服务
cd agent-service && python main.py

# 终端 2: 启动 Java 服务
cd backend && mvn spring-boot:run -pl ruoyi-admin

# 终端 3: 启动前端
cd frontend && pnpm dev
```

验证步骤：
1. 打开知识库详情页 → 看到统计卡片有数据
2. 上传一个 txt/pdf 文档 → 文档出现在列表中，状态为"待处理"
3. 点击"处理" → 弹窗显示 5 步骤条，进度推进
4. 等待完成 → 显示"已完成"，切块数/向量数有值，耗时正确
5. 点击"查看" → PDF 用 iframe 预览，TXT/MD 正常显示
6. 上传 DOCX → 点击"查看" → HTML 正确渲染

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "feat: M6 知识库详情页重构完成 — 处理流程 + DOCX预览 + 统计卡片"
```
