# M6 知识库详情页重构 — 设计文档

日期: 2026-06-22

## 概述

将知识库详情页从简单上传/删除改造为完整知识库管理能力：文档处理、状态追踪、内容预览。

## 当前状态

大部分基础设施已部分实现——实体、服务、控制器、Vue 组件都存在，但关键连接缺失：

- Java `processDocument()` 是空壳，不调用 Python
- Python `requirements.txt` 缺少 `pymilvus`
- SQL schema 落后于 Java Entity
- 前端缺少 DOCX 预览

## 方案决策

- **文件上传：** 已存服务端（`user.dir/upload/ai/kb/...`），无需改动
- **处理流程：** Java 提取文本 → 发送 content 给 Python → Python 切块+嵌入+写 Milvus
- **进度报告：** 两阶段推进 — Java 同步执行步骤 1-2，异步调用 Python 执行步骤 3-5
- **DOCX 预览：** 后端 POI 转换 HTML → 前端 v-html 渲染
- **历史数据：** 无需迁移

---

## 改动清单

### 1. 数据库

```sql
ALTER TABLE kb_document
ADD COLUMN vector_count INT DEFAULT 0 COMMENT '向量数',
ADD COLUMN process_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态',
ADD COLUMN process_progress INT DEFAULT 0 COMMENT '处理进度 0-100',
ADD COLUMN process_message VARCHAR(500) COMMENT '当前处理步骤描述',
ADD COLUMN processed_time DATETIME COMMENT '处理完成时间';
```

同步更新 `backend/sql/ai_init.sql` 建表语句。

### 2. 后端 Java

| 文件 | 改动 |
|------|------|
| `DocumentController.java` | 新增 `GET /{id}/preview` → DOCX→HTML |
| `IDocumentService.java` | 新增 `getDocumentPreview(Long docId)` |
| `DocumentServiceImpl.java` | `processDocument()` 改为真正调用 FastApiClient；实现 DOCX→HTML 转换 |
| `FastApiClient.java` | 新增 `indexDocument(Map)` → `POST /api/v1/knowledge/index` |
| `ai_init.sql` | 建表语句同步新增字段 |

**处理流程（processDocument 重写）：**

```
1. 查文档 + 校验文件存在
2. 状态 → PROCESSING, progress=10, message="文件读取中"
3. 读取文件提取文本 (PDFBox/POI/Tika)
4. progress=30, message="文本解析完成"
5. FastApiClient.indexDocument({docId, kbId, content, filePath})
6. progress=60, message="向量化处理中"
7. Python 返回 → 更新 chunkCount/vectorCount/processedTime
   成功 → SUCCESS, progress=100
   失败 → FAILED, processMessage=错误信息
```

**DOCX 预览：**

- `getDocumentPreview(docId)` — 对 DOCX 类型用 POI `XWPFDocument` 转 HTML
- 其他类型直接返回文本或下载 URL
- Controller 返回 `Content-Type: text/html`

### 3. Python Agent 服务

| 文件 | 改动 |
|------|------|
| `requirements.txt` | 新增 `pymilvus>=2.4.0` |
| `app/rag/schemas.py` | `IndexRequest` 新增可选字段 `file_path: str = ""` |

**无其他改动** — `api/knowledge.py`、`milvus_service.py`、`embedding_service.py`、`chunk_service.py` 已实现完毕。

### 4. 前端 Vue

| 文件 | 改动 |
|------|------|
| `views/ai/knowledge/kbDetail.vue` | DOCX 预览调用 `/preview` 接口，`v-html` 渲染；完善处理弹窗结果展示（耗时、错误信息） |
| `api/ai/knowledge.ts` | 新增 `getDocumentPreview(docId)` |

**DOCX 预览逻辑（kbDetail.vue 内）：**

```
fileType === 'docx' → GET /ai/document/{id}/preview → v-html 渲染 HTML
```

**处理完成展示（已部分实现，需补全）：**

```
成功 → "✓ 已完成 | 切块数: N | 向量数: N"
失败 → "✗ 处理失败 | 错误: {processMessage}"
```

### 5. DocumentProcessStatus 枚举

已有 `com.aiplatform.ai.domain.enums.DocumentProcessStatus`（或内联在 Entity 中）：
- `PENDING` — 待处理（灰色 Tag）
- `PROCESSING` — 处理中（橙色 Tag）
- `SUCCESS` — 已完成（绿色 Tag）
- `FAILED` — 失败（红色 Tag）

---

## 数据流

```
用户上传文档
  ↓
Java: 存文件到服务端 → 写 DB (PENDING)
  ↓
用户点击"处理"
  ↓
Java: 更新 PROCESSING → 读文件提取文本
  ↓
Java → FastApiClient → POST /api/v1/knowledge/index
  ↓
Python: split_text → embed_batch → milvus.insert
  ↓
Java ← {success: true, chunk_count: N}
  ↓
Java: 更新 SUCCESS + chunkCount + vectorCount + processedTime
  ↓
前端轮询 GET /process/{id} 获取最新状态
```

## 验收标准

1. 数据库 `kb_document` 包含所有新字段
2. `POST /ai/document/process/{id}` 触发完整处理流程
3. `GET /ai/document/process/{id}` 返回实时状态
4. `GET /ai/document/{id}/preview` DOCX→HTML 可用
5. `GET /ai/document/stats/{kbId}` 统计正确
6. 前端统计卡片数据正确
7. 前端处理弹窗展示步骤条 + 进度 + 结果
8. 前端文档预览支持 PDF/Markdown/TXT/DOCX
9. Python `requirements.txt` 含 pymilvus
